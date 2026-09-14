package com.d1inthechamber.blackjack

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.Alignment
import androidx.compose.material3.Text
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlin.math.*

internal enum class DealerMood { IDLE, ANGRY, SMUG, LAUGH }
internal fun dealerMood(net:Double?)=when { net==null->DealerMood.IDLE;net>0->DealerMood.ANGRY;net<0->DealerMood.LAUGH;else->DealerMood.SMUG }
internal fun dealingPose(p:Float,right:Boolean)=when {p<0f->0;p<.18f->1;p<.38f->2;p<.55f->3;p<.73f->if(right)5 else 4;else->0}
internal fun dealerSheet(room:RoomStyle)=when(room) {
    RoomStyle.VEGAS->R.drawable.poses_vegas;RoomStyle.CARNIVAL->R.drawable.poses_carnival
    RoomStyle.EGYPT->R.drawable.poses_egypt;RoomStyle.IRON->R.drawable.poses_iron
    RoomStyle.WEST->R.drawable.poses_west;RoomStyle.PUNK->R.drawable.poses_punk;RoomStyle.GREEN->R.drawable.poses_green
}
// Decode/key once off the UI thread. No SurfaceView, GL context, or permanent frame loop.
internal fun decodeDealer(context:android.content.Context,room:RoomStyle):ImageBitmap {
    val source=checkNotNull(BitmapFactory.decodeResource(context.resources,dealerSheet(room),BitmapFactory.Options().apply{inScaled=false}))
    val pixels=IntArray(source.width*source.height);source.getPixels(pixels,0,source.width,0,0,source.width,source.height)
    for(i in pixels.indices) {
        val c=pixels[i];val r=(c ushr 16)and 255;var g=(c ushr 8)and 255;val b=c and 255
        val green=g-max(r,b)
        val alpha=if(green>35) ((1f-((green-35)/50f).coerceIn(0f,1f))*255).toInt() else 255
        if(green>35)g=min(g,max(r,b)+12)
        pixels[i]=(alpha shl 24) or (r shl 16) or (g shl 8) or b
    }
    val bitmap=Bitmap.createBitmap(pixels,source.width,source.height,Bitmap.Config.ARGB_8888);source.recycle();return bitmap.asImageBitmap()
}

@Composable
internal fun DealerStage(game:BlackjackState,animated:Boolean,flights:CardFlights,modifier:Modifier) {
    val room=LocalRoomStyle.current;val context=LocalContext.current
    val sheet by produceState<ImageBitmap?>(null,room) { value=withContext(Dispatchers.Default){decodeDealer(context,room)} }
    val back=remember(room) { room.cardBack?.let { ImageBitmap.imageResource(context.resources,it) } }
    var reactionFrame by remember(room) { mutableIntStateOf(0) }
    val mood=if(game.finished&&!game.shuffling)dealerMood(game.lastRound?.net) else DealerMood.IDLE
    LaunchedEffect(mood,animated,game.roundNumber,flights.busy) {
        reactionFrame=0
        if(animated&&!flights.busy) when(mood) {
            DealerMood.IDLE->reactionFrame=0
            DealerMood.ANGRY->{reactionFrame=6;delay(260);reactionFrame=7;delay(300);reactionFrame=6}
            DealerMood.SMUG->reactionFrame=8
            DealerMood.LAUGH->{repeat(6){reactionFrame=if(it%2==0)10 else 11;delay(240)};reactionFrame=8}
        }
    }
    var shuffleTime by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(game.shuffling,animated){shuffleTime=0f;if(game.shuffling&&animated)while(true){delay(40);shuffleTime+=.18f}}
    val phase=flights.gestureProgress
    val frame=if(phase>=0f)dealingPose(phase,flights.pushRight) else if(game.shuffling&&animated)2 else reactionFrame
    val state=if(phase>=0f)"Dealing" else mood.name.lowercase().replaceFirstChar { it.uppercase() }
    BoxWithConstraints(modifier.testTag("cartoon-dealer").semantics { stateDescription=state;contentDescription="${room.host}, seated cartoon dealer" }
        .onGloballyPositioned { flights.dealerBounds=it.boundsInRoot() }) {
        val density=LocalDensity.current
        val width=with(density){maxWidth.toPx()};val height=with(density){maxHeight.toPx()}
        val side=min(height-with(density){32.dp.toPx()},width*.76f).coerceAtLeast(1f)
        val left=(width-side)/2f
        val cw=max(side*.27f,with(density){44.dp.toPx()})
        val shoe=Offset(left+side*.28f,side*.94f)
        SideEffect{flights.spriteLocal=Rect(left,0f,left+side,side)}
        Canvas(Modifier.fillMaxSize()) {
            // Chair and torso go behind the table; hands are layered back over the felt.
            drawRoundRect(Color(0xFF21151A),Offset(left+side*.17f,side*.32f),Size(side*.66f,side*.70f),androidx.compose.ui.geometry.CornerRadius(side*.18f),style=androidx.compose.ui.graphics.drawscope.Fill)
            drawRoundRect(room.accent.copy(alpha=.35f),Offset(left+side*.17f,side*.32f),Size(side*.66f,side*.70f),androidx.compose.ui.geometry.CornerRadius(side*.18f),style=Stroke(2.dp.toPx()))
            fun sprite(){sheet?.let{drawDealerPose(it,room,frame,left,side)}}
            sprite()
            val tableY=side*.84f
            val tablePath=Path().apply{moveTo(0f,tableY+16.dp.toPx());quadraticBezierTo(width*.5f,tableY-13.dp.toPx(),width,tableY+16.dp.toPx());lineTo(width,height);lineTo(0f,height);close()}
            drawPath(tablePath,Brush.verticalGradient(listOf(Color(0xFF28402E),Color(0xFF0B211A)),startY=tableY,endY=height))
            val rail=Path().apply{moveTo(0f,tableY+16.dp.toPx());quadraticBezierTo(width*.5f,tableY-13.dp.toPx(),width,tableY+16.dp.toPx())}
            drawPath(rail,Color(0xFF37251D),style=Stroke(9.dp.toPx()))
            drawPath(rail,room.accent.copy(alpha=.65f),style=Stroke(1.2.dp.toPx()))
            // Angled wood-and-brass shoe with a deep card stack, separate from the live card.
            val sx=shoe.x-cw*.55f;val sy=shoe.y-cw*.29f
            val casing=Path().apply{moveTo(sx,sy);lineTo(sx+cw*.92f,sy-cw*.24f);lineTo(sx+cw*1.13f,sy+cw*.50f);lineTo(sx+cw*.10f,sy+cw*.67f);close()}
            drawPath(casing,Color(0xFF4C3425));drawPath(casing,room.accent,style=Stroke(2.dp.toPx()))
            repeat(9){i->val shift=if(game.shuffling&&animated)sin(shuffleTime+i*.7f)*cw*.10f else 0f;val y=sy+cw*.29f+i*cw*.026f;drawLine(Color(0xFFEAE0CB),Offset(sx+cw*.13f+shift,y),Offset(sx+cw*.91f+shift,y-cw*.09f),1.dp.toPx())}
            if(back!=null)drawImage(back,srcSize=IntSize(back.width,back.height),dstOffset=IntOffset((shoe.x-cw*.42f).roundToInt(),(shoe.y-cw*.25f).roundToInt()),dstSize=IntSize((cw*.78f).roundToInt(),(cw*.40f).roundToInt()))
            else drawRoundRect(Color(0xFF163758),Offset(shoe.x-cw*.42f,shoe.y-cw*.25f),Size(cw*.78f,cw*.40f),androidx.compose.ui.geometry.CornerRadius(3f))
            if(phase in .38f.. .68f){
                val hand=flights.localHand(phase)
                if(back!=null)drawImage(back,srcSize=IntSize(back.width,back.height),dstOffset=IntOffset((hand.x-cw*.39f).roundToInt(),(hand.y-cw*.20f).roundToInt()),dstSize=IntSize((cw*.78f).roundToInt(),(cw*.40f).roundToInt()))
                else drawRect(Color(0xFF163758),hand-Offset(cw*.39f,cw*.20f),Size(cw*.78f,cw*.40f))
            }
            // Restore only the forearms over the table, leaving the waist occluded behind it.
            clipRect(left=left,top=tableY,right=left+side*.43f,bottom=height){sprite()}
            clipRect(left=left+side*.61f,top=tableY,right=left+side,bottom=height){sprite()}
            drawLine(room.accent.copy(alpha=.5f),Offset(0f,height-1.dp.toPx()),Offset(width,height-1.dp.toPx()),2.dp.toPx())
        }
        Box(Modifier.offset {IntOffset((shoe.x-cw*.60f).roundToInt(),(shoe.y-cw*.60f).roundToInt())}
            .size(with(density){(cw*1.4f).toDp()},with(density){(cw*1.25f).toDp()})
            .testTag("visible-shoe").semantics{contentDescription="Physical six-deck shoe on table, ${game.deckRemaining} cards"})
        Text(if(game.shuffling)"SHUFFLING…" else "SHOE ${game.deckRemaining}",color=room.accent,fontSize=10.sp,
            modifier=Modifier.align(Alignment.BottomStart).padding(start=10.dp,bottom=3.dp))
    }
}

private fun DrawScope.drawDealerPose(atlas:ImageBitmap,room:RoomStyle,frame:Int,left:Float,side:Float){
    val rows=when(room){RoomStyle.CARNIVAL->intArrayOf(0,356,702,1086);RoomStyle.PUNK->intArrayOf(0,362,716,1086);RoomStyle.IRON->intArrayOf(0,358,712,1086);RoomStyle.EGYPT->intArrayOf(0,366,728,1086);RoomStyle.GREEN->intArrayOf(0,362,724,1086);else->intArrayOf(0,364,723,1086)}
    val row=frame/4;val sy=(rows[row]*atlas.height/1086f).roundToInt();val ey=(rows[row+1]*atlas.height/1086f).roundToInt();val w=atlas.width/4
    drawImage(atlas,srcOffset=IntOffset((frame%4)*w,sy),srcSize=IntSize(w,ey-sy),dstOffset=IntOffset(left.roundToInt(),0),dstSize=IntSize(side.roundToInt(),side.roundToInt()),filterQuality=FilterQuality.Medium)
}
