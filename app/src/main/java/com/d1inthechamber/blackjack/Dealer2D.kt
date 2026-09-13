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
internal fun dealingPose(p:Float,right:Boolean)=when {p<0f->0;p<.18f->1;p<.38f->2;p<.55f->3;p<.73f->if(right)5 else 4;p<.84f->3;else->0}
internal fun dealerSheet(room:RoomStyle)=when(room) {
    RoomStyle.VEGAS->R.drawable.poses_vegas;RoomStyle.CARNIVAL->R.drawable.poses_carnival
    RoomStyle.EGYPT->R.drawable.poses_egypt;RoomStyle.IRON->R.drawable.poses_iron
    RoomStyle.WEST->R.drawable.poses_west;RoomStyle.PUNK->R.drawable.poses_punk
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
            DealerMood.IDLE->while(true){delay(6000);reactionFrame=9;delay(180);reactionFrame=0}
            DealerMood.ANGRY->{reactionFrame=6;delay(260);reactionFrame=7;delay(300);reactionFrame=6}
            DealerMood.SMUG->{reactionFrame=8;delay(900);reactionFrame=9;delay(180);reactionFrame=8}
            DealerMood.LAUGH->{repeat(12){reactionFrame=if(it%2==0)10 else 11;delay(150)};reactionFrame=8}
        }
    }
    val phase=flights.gestureProgress
    val frame=if(phase>=0f)dealingPose(phase,flights.pushRight) else if(game.shuffling&&animated)2 else reactionFrame
    val state=if(phase>=0f)"Dealing" else mood.name.lowercase().replaceFirstChar { it.uppercase() }
    Box(modifier.testTag("cartoon-dealer").semantics { stateDescription=state;contentDescription="${room.host}, animated cartoon dealer" }
        .onGloballyPositioned { flights.dealerBounds=it.boundsInRoot() }) {
        Canvas(Modifier.fillMaxSize().testTag("visible-shoe").semantics { contentDescription="Visible six-deck shoe, ${game.deckRemaining} cards" }) {
            val side=min(size.height-24.dp.toPx(),size.width*.72f).coerceAtLeast(1f)
            val left=(size.width-side)/2;val top=0f
            flights.spriteLocal=Rect(left,top,left+side,top+side)
            val shoe=Offset(left+side*.24f,top+side*.94f);val cw=side*.19f;val ch=cw*1.35f
            drawRoundRect(Color(0xFF241911),shoe-Offset(cw*.72f,ch*.36f),Size(cw*1.44f,ch*.96f),androidx.compose.ui.geometry.CornerRadius(5.dp.toPx()),style=androidx.compose.ui.graphics.drawscope.Fill)
            repeat(6){i->drawRoundRect(Color(0xFFE1D9C6),shoe-Offset(cw/2,ch*.25f)+Offset(i*.6f,i*2f),Size(cw,ch*.5f),androidx.compose.ui.geometry.CornerRadius(2f),style=Stroke(1.3f))}
            if(back!=null)drawImage(back,srcSize=IntSize(back.width,back.height),dstOffset=IntOffset((shoe.x-cw/2).roundToInt(),(shoe.y-ch*.25f).roundToInt()),dstSize=IntSize(cw.roundToInt(),(ch*.5f).roundToInt()))
            else drawRect(Color(0xFF183563),shoe-Offset(cw/2,ch*.25f),Size(cw,ch*.5f))
            if(phase in .30f.. .68f) {
                val hand=flights.localHand(phase);val cardSize=Size(cw,ch*.52f)
                if(back!=null)drawImage(back,srcSize=IntSize(back.width,back.height),dstOffset=IntOffset((hand.x-cw/2).roundToInt(),(hand.y-cardSize.height/2).roundToInt()),dstSize=IntSize(cw.roundToInt(),cardSize.height.roundToInt()))
                else drawRect(Color(0xFF183563),hand-Offset(cw/2,cardSize.height/2),cardSize)
            }
            sheet?.let { atlas ->
                val rows=when(room){RoomStyle.CARNIVAL->intArrayOf(0,356,702,1086);RoomStyle.PUNK->intArrayOf(0,362,716,1086);RoomStyle.IRON->intArrayOf(0,358,712,1086);RoomStyle.EGYPT->intArrayOf(0,366,728,1086);else->intArrayOf(0,364,723,1086)}
                val row=frame/4;val sy=(rows[row]*atlas.height/1086f).roundToInt();val ey=(rows[row+1]*atlas.height/1086f).roundToInt();val w=atlas.width/4
                drawImage(atlas,srcOffset=IntOffset((frame%4)*w,sy),srcSize=IntSize(w,ey-sy),dstOffset=IntOffset(left.roundToInt(),top.roundToInt()),dstSize=IntSize(side.roundToInt(),side.roundToInt()),filterQuality=FilterQuality.Medium)
            }
            // Front edge remains readable even while the hand covers the top card.
            drawLine(room.accent,Offset(shoe.x-cw*.7f,side+5.dp.toPx()),Offset(shoe.x+cw*.7f,side+5.dp.toPx()),3.dp.toPx())
            repeat(3){i->drawLine(Color(0xFFE6DDC8),Offset(shoe.x-cw*.48f,side+8.dp.toPx()+i*2),Offset(shoe.x+cw*.48f,side+8.dp.toPx()+i*2),1f)}
        }
    }
}
