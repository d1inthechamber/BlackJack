package com.d1inthechamber.blackjack

import android.content.Context
import android.graphics.PixelFormat
import android.opengl.GLES20.*
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import java.nio.ByteBuffer
import java.nio.ByteOrder
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import kotlin.math.*

@Composable
fun Dealer3D(game: BlackjackState, animated: Boolean, modifier: Modifier, motionPulse: Int = game.drawPulse, onSurfaceReady: (DealerSurface) -> Unit = {}) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val view = remember { DealerSurface(context) }
    val owner = LocalLifecycleOwner.current
    DisposableEffect(owner, view) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) view.onResume()
            if (event == Lifecycle.Event.ON_PAUSE) view.onPause()
        }
        owner.lifecycle.addObserver(observer)
        onDispose { owner.lifecycle.removeObserver(observer); view.onPause() }
    }
    AndroidView(factory = { view }, modifier = modifier, update = {
        it.actor.motionEnabled = animated
        it.actor.pulse = motionPulse
        onSurfaceReady(it)
        it.actor.shuffle = game.shuffling
        it.actor.result = if (game.finished) game.message else ""
    })
}
class DealerSurface(context: Context) : GLSurfaceView(context) {
    val actor = DealerRenderer(context.applicationContext)
    init {
        contentDescription = "Animated original casino dealer"
        setEGLContextClientVersion(2)
        setEGLConfigChooser(8,8,8,8,16,0)
        holder.setFormat(PixelFormat.TRANSLUCENT)
        setZOrderOnTop(true)
        preserveEGLContextOnPause = true
        setRenderer(actor)
    }
}

// The original artwork is the texture of an articulated relief. Gentle camera-facing
// rotations reveal its sculpted depth without pretending to provide unseen side/back art.
class DealerRenderer(private val context: Context) : GLSurfaceView.Renderer {
    @Volatile var framesRendered = 0
    @Volatile var lastGlError = 0
    @Volatile var motionEnabled = true
    @Volatile var pulse = 0
    @Volatile var shuffle = false
    @Volatile var result = ""
    @Volatile var textureLoaded = false
    @Volatile var headYaw = 0f
    @Volatile var handFractionX = .5f
    @Volatile var handFractionY = .7f
    private var viewportAspect = 1f
    private val palm = floatArrayOf(DealerMesh.x(.145f),DealerMesh.y(.735f),DealerMesh.depth(.145f,.735f),1f)
    private val movedPalm = FloatArray(4)
    private var seenPulse = 0
    private var gestureStart = -10f
    private val started = System.nanoTime()
    private var program = 0
    private var texture = 0
    private var vertexBuffer = 0
    private val attributes = IntArray(4)
    private var mvpLocation = 0
    private var neckLocation = 0
    private var leftLocation = 0
    private var rightLocation = 0
    private var textureLocation = 0
    private val projection=FloatArray(16)
    private val camera=FloatArray(16)
    private val mvp=FloatArray(16)
    private val neck=FloatArray(16)
    private val left=FloatArray(16)
    private val right=FloatArray(16)
    private var count=0

    override fun onSurfaceCreated(gl:GL10?,config:EGLConfig?) {
        textureLoaded=false
        fun shader(type:Int,source:String):Int {
            val id=glCreateShader(type)
            glShaderSource(id,source);glCompileShader(id)
            val status=IntArray(1);glGetShaderiv(id,GL_COMPILE_STATUS,status,0)
            check(status[0]!=0) { "Dealer shader: " + glGetShaderInfoLog(id) }
            return id
        }
        val vs=shader(GL_VERTEX_SHADER,"""
            attribute vec3 p;
            attribute vec2 uv;
            attribute vec3 n;
            attribute vec3 weights;
            uniform mat4 mvp;
            uniform mat4 neck;
            uniform mat4 leftArm;
            uniform mat4 rightArm;
            varying vec2 tex;
            varying float light;
            void main() {
                vec4 base=vec4(p,1.0);
                float rest=1.0-weights.x-weights.y-weights.z;
                vec4 skinned=base*rest + (neck*base)*weights.x
                    + (leftArm*base)*weights.y + (rightArm*base)*weights.z;
                vec3 normal=normalize(n*rest + (mat3(neck)*n)*weights.x
                    + (mat3(leftArm)*n)*weights.y + (mat3(rightArm)*n)*weights.z);
                gl_Position=mvp*skinned;
                tex=uv;
                light=0.92+0.08*max(dot(normal,normalize(vec3(-0.4,0.6,1.0))),0.0);
            }
        """.trimIndent())
        val fs=shader(GL_FRAGMENT_SHADER,"""
            precision mediump float;
            uniform sampler2D artwork;
            varying vec2 tex;
            varying float light;
            void main() {
                vec4 c=texture2D(artwork,tex);
                if(c.a<0.05) discard;
                gl_FragColor=vec4(c.rgb*light,c.a);
            }
        """.trimIndent())
        program=glCreateProgram();glAttachShader(program,vs);glAttachShader(program,fs);glLinkProgram(program)
        val linked=IntArray(1);glGetProgramiv(program,GL_LINK_STATUS,linked,0)
        check(linked[0]!=0) { "Dealer program: " + glGetProgramInfoLog(program) }
        glDeleteShader(vs);glDeleteShader(fs)
        listOf("p","uv","n","weights").forEachIndexed { i,name -> attributes[i]=glGetAttribLocation(program,name) }
        mvpLocation=glGetUniformLocation(program,"mvp")
        neckLocation=glGetUniformLocation(program,"neck")
        leftLocation=glGetUniformLocation(program,"leftArm")
        rightLocation=glGetUniformLocation(program,"rightArm")
        textureLocation=glGetUniformLocation(program,"artwork")
        val data=DealerMesh.build();count=data.size/DealerMesh.STRIDE
        val buffer=ByteBuffer.allocateDirect(data.size*4).order(ByteOrder.nativeOrder()).asFloatBuffer().apply { put(data);position(0) }
        val ids=IntArray(1)
        glGenBuffers(1,ids,0);vertexBuffer=ids[0]
        glBindBuffer(GL_ARRAY_BUFFER,vertexBuffer);glBufferData(GL_ARRAY_BUFFER,data.size*4,buffer,GL_STATIC_DRAW)
        glGenTextures(1,ids,0);texture=ids[0];glBindTexture(GL_TEXTURE_2D,texture)
        glTexParameteri(GL_TEXTURE_2D,GL_TEXTURE_MIN_FILTER,GL_LINEAR)
        glTexParameteri(GL_TEXTURE_2D,GL_TEXTURE_MAG_FILTER,GL_LINEAR)
        glTexParameteri(GL_TEXTURE_2D,GL_TEXTURE_WRAP_S,GL_CLAMP_TO_EDGE)
        glTexParameteri(GL_TEXTURE_2D,GL_TEXTURE_WRAP_T,GL_CLAMP_TO_EDGE)
        val options=android.graphics.BitmapFactory.Options().apply { inScaled=false }
        val bitmap=checkNotNull(android.graphics.BitmapFactory.decodeResource(context.resources,R.drawable.dealer_old_vegas,options))
        android.opengl.GLUtils.texImage2D(GL_TEXTURE_2D,0,bitmap,0)
        bitmap.recycle()
        textureLoaded=glGetError()==GL_NO_ERROR
        check(textureLoaded) { "Dealer artwork texture failed to upload" }
        glEnable(GL_DEPTH_TEST);glEnable(GL_BLEND)
        // Android's decoded artwork is premultiplied alpha. Preserve soft hair edges.
        glBlendFunc(GL_ONE,GL_ONE_MINUS_SRC_ALPHA)
        glClearColor(0f,0f,0f,0f)
    }
    override fun onSurfaceChanged(gl:GL10?,width:Int,height:Int) {
        glViewport(0,0,width,height)
        val aspect=width.toFloat()/height.coerceAtLeast(1)
        viewportAspect=aspect
        Matrix.orthoM(projection,0,-1.08f*aspect,1.08f*aspect,-1.08f,1.08f,1f,20f)
        Matrix.setLookAtM(camera,0,0f,0f,5f,0f,0f,0f,0f,1f,0f)
        Matrix.multiplyMM(mvp,0,projection,0,camera,0)
    }
    private fun pose(matrix:FloatArray,u:Float,v:Float,yaw:Float,tilt:Float,pitch:Float,reach:Float=0f) {
        val x=DealerMesh.x(u);val y=DealerMesh.y(v);val z=DealerMesh.depth(u,v)
        Matrix.setIdentityM(matrix,0)
        Matrix.translateM(matrix,0,x,y,z+reach)
        Matrix.rotateM(matrix,0,yaw,0f,1f,0f)
        Matrix.rotateM(matrix,0,tilt,0f,0f,1f)
        Matrix.rotateM(matrix,0,pitch,1f,0f,0f)
        Matrix.translateM(matrix,0,-x,-y,-z)
    }
    override fun onDrawFrame(gl:GL10?) {
        val t=(System.nanoTime()-started)/1_000_000_000f
        if(pulse!=seenPulse) {seenPulse=pulse;gestureStart=t}
        val g=if(motionEnabled) sin(((t-gestureStart)/.48f).coerceIn(0f,1f)*PI.toFloat()) else 0f
        val idle=if(motionEnabled) sin(t*.78f) else 0f
        val riffle=if(motionEnabled && shuffle) sin(t*8f) else 0f
        headYaw=idle*4f
        pose(neck,.51f,.36f,headYaw,idle*1.6f,if(motionEnabled && result.isNotEmpty()) sin(t*1.5f)*3f else -g*3f)
        pose(left,.245f,.47f,idle*.8f+riffle*2f,g*7f+idle*.7f,-g*12f+riffle*4f,g*.055f)
        pose(right,.81f,.47f,-idle, -g*2f+riffle*3f,idle*1.3f-riffle*3f)
        Matrix.multiplyMV(movedPalm,0,left,0,palm,0)
        handFractionX=.5f+movedPalm[0]/(2.16f*viewportAspect)
        handFractionY=.5f-movedPalm[1]/2.16f
        glClear(GL_COLOR_BUFFER_BIT or GL_DEPTH_BUFFER_BIT);glUseProgram(program)
        glUniformMatrix4fv(mvpLocation,1,false,mvp,0)
        glUniformMatrix4fv(neckLocation,1,false,neck,0)
        glUniformMatrix4fv(leftLocation,1,false,left,0)
        glUniformMatrix4fv(rightLocation,1,false,right,0)
        glActiveTexture(GL_TEXTURE0);glBindTexture(GL_TEXTURE_2D,texture);glUniform1i(textureLocation,0)
        glBindBuffer(GL_ARRAY_BUFFER,vertexBuffer)
        val sizes=intArrayOf(3,2,3,3);val offsets=intArrayOf(0,3,5,8)
        attributes.forEachIndexed { i,location ->
            glEnableVertexAttribArray(location)
            glVertexAttribPointer(location,sizes[i],GL_FLOAT,false,DealerMesh.STRIDE*4,offsets[i]*4)
        }
        glDrawArrays(GL_TRIANGLES,0,count)
        attributes.forEach { glDisableVertexAttribArray(it) }
        lastGlError=glGetError();framesRendered++
        Thread.sleep(25)
    }
}
