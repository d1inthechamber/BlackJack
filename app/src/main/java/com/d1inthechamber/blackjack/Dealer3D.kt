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
fun Dealer3D(game: BlackjackState, animated: Boolean, modifier: Modifier) {
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
        it.actor.pulse = game.drawPulse
        it.actor.shuffle = game.shuffling
        it.actor.result = if (game.finished) game.message else ""
    })
}
class DealerSurface(context: Context) : GLSurfaceView(context) {
    val actor = DealerRenderer()
    init {
        contentDescription = "Animated 3D casino dealer"
        setEGLContextClientVersion(2)
        setEGLConfigChooser(8,8,8,8,16,0)
        holder.setFormat(PixelFormat.TRANSLUCENT)
        setZOrderOnTop(true)
        preserveEGLContextOnPause = true
        setRenderer(actor)
    }
}

// Original low-poly character, modeled from volumetric meshes with articulated shoulder,
// elbow, wrist and neck transforms. Every part is lit and depth-tested in OpenGL ES.
class DealerRenderer : GLSurfaceView.Renderer {
    @Volatile var framesRendered = 0
    @Volatile var lastGlError = 0
    @Volatile var motionEnabled = true
    @Volatile var pulse = 0
    @Volatile var shuffle = false
    @Volatile var result = ""
    private var seenPulse = 0
    private var gestureStart = -10f
    private val started = System.nanoTime()
    private var program = 0
    private var position = 0; private var normal = 0; private var matrix = 0; private var modelLoc = 0; private var colour = 0
    private val projection = FloatArray(16)
    private val camera = FloatArray(16)
    private val vp = FloatArray(16)
    private val model = FloatArray(16)
    private val mvp = FloatArray(16)
    private val root = FloatArray(16)
    private val skin = floatArrayOf(.65f,.40f,.24f,1f)
    private val cream = floatArrayOf(.62f,.55f,.38f,1f)
    private val shirt = floatArrayOf(.28f,.075f,.05f,1f)
    private val dark = floatArrayOf(.065f,.043f,.029f,1f)
    private val gold = floatArrayOf(.55f,.35f,.10f,1f)
    private val vertices: java.nio.FloatBuffer
    private val normals: java.nio.FloatBuffer
    private val count: Int
    init {
        val v=mutableListOf<Float>(); val n=mutableListOf<Float>()
        fun point(lat:Float,lon:Float)=floatArrayOf(cos(lat)*cos(lon),sin(lat),cos(lat)*sin(lon))
        for (row in 0 until 12) for (col in 0 until 18) {
            val a=-PI.toFloat()/2+row*PI.toFloat()/12; val b=a+PI.toFloat()/12
            val c=col*2*PI.toFloat()/18;val d=c+2*PI.toFloat()/18
            val p=listOf(point(a,c),point(b,c),point(b,d),point(a,c),point(b,d),point(a,d))
            p.chunked(3).forEach { tri ->
                val u=FloatArray(3){tri[1][it]-tri[0][it]}; val w=FloatArray(3){tri[2][it]-tri[0][it]}
                var nn=floatArrayOf(u[1]*w[2]-u[2]*w[1],u[2]*w[0]-u[0]*w[2],u[0]*w[1]-u[1]*w[0])
                val len=sqrt(nn.sumOf{(it*it).toDouble()}).toFloat().coerceAtLeast(.0001f)
                nn=FloatArray(3){nn[it]/len}
                if(nn.indices.sumOf{(nn[it]*tri[0][it]).toDouble()}<0) nn=FloatArray(3){-nn[it]}
                tri.forEach { pnt -> v.addAll(pnt.toList()); n.addAll(nn.toList()) }
            }
        }
        count=v.size/3
        fun buffer(data:List<Float>)=ByteBuffer.allocateDirect(data.size*4).order(ByteOrder.nativeOrder()).asFloatBuffer().apply{put(data.toFloatArray());position(0)}
        vertices=buffer(v);normals=buffer(n)
    }
    override fun onSurfaceCreated(gl:GL10?,config:EGLConfig?) {
        fun shader(type:Int,source:String):Int { val id=glCreateShader(type);glShaderSource(id,source);glCompileShader(id);return id }
        val vs=shader(GL_VERTEX_SHADER,"attribute vec3 p; attribute vec3 n; uniform mat4 mvp; uniform mat4 model; varying float light; void main(){gl_Position=mvp*vec4(p,1.0); vec3 normal=normalize(mat3(model)*n); light=0.30+0.70*max(dot(normal,normalize(vec3(-0.5,0.8,1.4))),0.0);}")
        val fs=shader(GL_FRAGMENT_SHADER,"precision mediump float; uniform vec4 color; varying float light; void main(){gl_FragColor=vec4(color.rgb*light,color.a);}")
        program=glCreateProgram();glAttachShader(program,vs);glAttachShader(program,fs);glLinkProgram(program)
        glDeleteShader(vs);glDeleteShader(fs)
        position=glGetAttribLocation(program,"p");normal=glGetAttribLocation(program,"n")
        matrix=glGetUniformLocation(program,"mvp");modelLoc=glGetUniformLocation(program,"model");colour=glGetUniformLocation(program,"color")
        glEnable(GL_DEPTH_TEST);glClearColor(0f,0f,0f,0f)
    }
    override fun onSurfaceChanged(gl:GL10?,width:Int,height:Int) {
        glViewport(0,0,width,height)
        val aspect=width.toFloat()/height.coerceAtLeast(1)
        Matrix.orthoM(projection,0,-1.7f*aspect,1.7f*aspect,-1.25f,2.15f,1f,20f)
        Matrix.setLookAtM(camera,0,0f,0f,7f,0f,0f,0f,0f,1f,0f)
        Matrix.multiplyMM(vp,0,projection,0,camera,0)
    }
    private fun part(parent:FloatArray,x:Float,y:Float,z:Float,sx:Float,sy:Float,sz:Float,c:FloatArray,tilt:Float=0f) {
        System.arraycopy(parent,0,model,0,16);Matrix.translateM(model,0,x,y,z)
        Matrix.rotateM(model,0,tilt,0f,0f,1f);Matrix.scaleM(model,0,sx,sy,sz)
        Matrix.multiplyMM(mvp,0,vp,0,model,0)
        glUniformMatrix4fv(matrix,1,false,mvp,0);glUniformMatrix4fv(modelLoc,1,false,model,0)
        glUniform4fv(colour,1,c,0);glDrawArrays(GL_TRIANGLES,0,count)
    }
    override fun onDrawFrame(gl:GL10?) {
        val t=(System.nanoTime()-started)/1_000_000_000f
        if(pulse!=seenPulse){seenPulse=pulse;gestureStart=t}
        val g=if(motionEnabled) sin(((t-gestureStart)/.8f).coerceIn(0f,1f)*PI.toFloat()) else 0f
        val idle=if(motionEnabled) sin(t*.65f) else 0f
        glClear(GL_COLOR_BUFFER_BIT or GL_DEPTH_BUFFER_BIT);glUseProgram(program)
        glEnableVertexAttribArray(position);glEnableVertexAttribArray(normal)
        glVertexAttribPointer(position,3,GL_FLOAT,false,0,vertices);glVertexAttribPointer(normal,3,GL_FLOAT,false,0,normals)
        Matrix.setIdentityM(root,0)
        // Creased leisure jacket, oxblood shirt, narrow tie and lapels.
        part(root,0f,-.35f,0f,.68f,.83f,.30f,cream)
        part(root,0f,-.15f,.27f,.31f,.64f,.06f,shirt)
        part(root,-.26f,.0f,.29f,.13f,.56f,.06f,cream,22f)
        part(root,.26f,.0f,.29f,.13f,.56f,.06f,cream,-22f)
        part(root,.03f,-.27f,.36f,.07f,.47f,.03f,dark,5f)
        part(root,.0f,.18f,.38f,.08f,.08f,.04f,dark)
        part(root,.43f,-.1f,.29f,.12f,.055f,.045f,shirt)
        part(root,0f,.53f,0f,.16f,.24f,.15f,skin)
        val head=root.copyOf();Matrix.translateM(head,0,0f,.98f,.02f)
        Matrix.rotateM(head,0,idle*7f,0f,1f,0f)
        Matrix.rotateM(head,0,if(motionEnabled && result.isNotEmpty()) sin(t*2.2f)*3 else -3f,0f,0f,1f)
        part(head,0f,.12f,0f,.32f,.45f,.28f,skin)
        part(head,0f,-.14f,.07f,.25f,.23f,.23f,skin)
        part(head,-.31f,.08f,0f,.07f,.12f,.075f,skin);part(head,.31f,.08f,0f,.07f,.12f,.075f,skin)
        // Messy receding hair, heavy eyebrows, moustache, nose and amber aviators.
        part(head,0f,.48f,-.035f,.31f,.13f,.24f,dark)
        part(head,-.27f,.25f,-.09f,.075f,.27f,.18f,dark)
        part(head,.28f,.23f,-.09f,.07f,.27f,.18f,dark)
        part(head,-.12f,.55f,.06f,.17f,.09f,.17f,dark,-15f)
        part(head,.04f,.045f,.29f,.065f,.14f,.10f,skin,-7f)
        for(side in listOf(-1f,1f)) {
            part(head,side*.15f,.18f,.267f,.15f,.112f,.045f,gold,side*-8)
            part(head,side*.15f,.18f,.302f,.132f,.093f,.02f,floatArrayOf(.24f,.115f,.035f,1f),side*-8)
            part(head,side*.145f,.315f,.225f,.14f,.032f,.025f,dark,side*10)
        }
        part(head,0f,.19f,.31f,.065f,.015f,.02f,gold)
        part(head,.025f,-.11f,.279f,.15f,.04f,.028f,dark,-7f)
        part(head,.03f,-.19f,.262f,.12f,.015f,.02f,shirt,-7f)
        // Independent shoulder/elbow joints: a card-delivery reach and shuffle motion.
        for(side in listOf(-1f,1f)) {
            val arm=root.copyOf();Matrix.translateM(arm,0,side*.58f,.27f,0f)
            val shuffleMove=if(shuffle && motionEnabled) sin(t*9f+side)*16 else 0f
            Matrix.rotateM(arm,0,side*(14f+g*26f)+shuffleMove,0f,0f,1f)
            Matrix.rotateM(arm,0,if(side<0) -g*40 else idle*3,1f,0f,0f)
            part(arm,0f,-.34f,0f,.19f,.42f,.2f,cream)
            Matrix.translateM(arm,0,0f,-.68f,0f);Matrix.rotateM(arm,0,-35f-g*35,1f,0f,0f)
            part(arm,0f,-.25f,0f,.15f,.32f,.17f,cream)
            part(arm,0f,-.51f,.0f,.14f,.055f,.15f,shirt)
            part(arm,0f,-.65f,.0f,.13f,.17f,.08f,skin)
            repeat(4){finger ->part(arm,-.085f+finger*.052f,-.81f,.0f,.022f,.12f,.035f,skin)}
            part(arm,side*.13f,-.65f,.02f,.035f,.10f,.04f,skin,side*-30)
            if(side<0 && g>.08f) part(arm,0f,-.93f,.07f,.16f,.23f,.015f,floatArrayOf(.86f,.8f,.63f,1f),10f)
        }
        glDisableVertexAttribArray(position);glDisableVertexAttribArray(normal)
        lastGlError = glGetError()
        framesRendered++
        Thread.sleep(25)
    }
}
