package com.d1inthechamber.blackjack

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import kotlinx.coroutines.delay
import java.util.IdentityHashMap
import kotlin.math.*

private class CardIdentity(val card:Card) {
    override fun equals(other:Any?)=other is CardIdentity && card===other.card
    override fun hashCode()=System.identityHashCode(card)
}
internal class CardFlight(val card:Card,val face:()->String,val destination:()->Rect?,val prepare:suspend ()->Unit = {},val arrive:()->Unit)
internal class CardFlights(game:BlackjackState) {
    // Identity distinguishes repeated ranks/suits in a six-deck shoe, and preserves
    // existing cards when a pair is split. Restored cards start seated at the table.
    private val seen=IdentityHashMap<Card,Boolean>().apply {
        game.dealer.forEach { put(it,true) }
        game.hands.forEach { hand -> hand.cards.forEach { put(it,true) } }
    }
    val queue=mutableStateListOf<CardFlight>()
    var motionPulse by mutableIntStateOf(0)
    var dealerBounds:Rect?=null
    var rootOrigin=Offset.Zero
    var handFraction:()->Offset = { Offset(.5f,.7f) }
    val busy get()=queue.isNotEmpty()
    fun hasSeen(card:Card)=seen.containsKey(card)
    fun enqueue(flight:CardFlight) {
        if(seen.put(flight.card,true)==null) queue.add(flight) else flight.arrive()
    }
    fun handOrigin():Offset? = dealerBounds?.let {
        val f=handFraction()
        Offset(it.left+it.width*f.x,it.top+it.height*f.y)-rootOrigin
    }
}
internal val LocalCardFlights=staticCompositionLocalOf<CardFlights?> { null }

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun DealtCard(card:Card,text:String,width:Dp,height:Dp) {
    val flights=LocalCardFlights.current
    val identity = CardIdentity(card)
    val requester = remember { BringIntoViewRequester() }
    var arrived by remember(flights,identity) { mutableStateOf(flights==null || flights.hasSeen(card)) }
    var bounds by remember { mutableStateOf<Rect?>(null) }
    val currentText by rememberUpdatedState(text)
    LaunchedEffect(flights,identity) {
        if(!arrived && flights!=null) flights.enqueue(CardFlight(card,{currentText},{bounds},prepare={ requester.bringIntoView() }) { arrived=true })
    }
    Box(Modifier.bringIntoViewRequester(requester).onGloballyPositioned { bounds=it.boundsInRoot() }.graphicsLayer { alpha=if(arrived) 1f else 0f }) {
        CardView(text,width,height)
    }
}

@Composable
internal fun CardFlightsOverlay(flights:CardFlights) {
    val active=flights.queue.firstOrNull()
    val progress=remember(flights) { Animatable(0f) }
    var origin by remember(flights) { mutableStateOf(Offset.Zero) }
    var moving by remember(flights) { mutableStateOf(false) }
    LaunchedEffect(flights,active) {
        moving=false
        if(active!=null) {
            try {
            // Let the real destination finish measuring and scrolling into view.
            var frames=0
            while((active.destination()==null || flights.handOrigin()==null) && frames++<45) withFrameNanos { }
            if (active.destination()!=null) active.prepare()
            withFrameNanos { };withFrameNanos { }
            flights.motionPulse++
            delay(90)
            val start=flights.handOrigin()
            if(start!=null && active.destination()!=null) {
                origin=start;progress.snapTo(0f);moving=true
                progress.animateTo(1f,tween(370,easing=FastOutSlowInEasing))
            }
            } finally {
                active.arrive();moving=false
                flights.queue.remove(active)
            }
        }
    }
    Box(Modifier.fillMaxSize()) {
        if(moving && active!=null) active.destination()?.let { rect ->
            val density=LocalDensity.current
            val end=rect.center-flights.rootOrigin
            val p=progress.value
            val center=origin+(end-origin)*p-Offset(0f,sin(p*PI.toFloat())*55f*density.density)
            val w=with(density){rect.width.toDp()};val h=with(density){rect.height.toDp()}
            Box(Modifier.testTag("dealing-card").offset { IntOffset((center.x-rect.width/2).roundToInt(),(center.y-rect.height/2).roundToInt()) }
                .graphicsLayer { rotationZ=-14f*(1f-p);scaleX=.72f+.28f*p;scaleY=scaleX }) {
                CardView(active.face(),w,h)
            }
        }
    }
}
