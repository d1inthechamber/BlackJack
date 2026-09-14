package com.d1inthechamber.blackjack

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
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
internal class CardFlights(private val game:BlackjackState) {
    // Identity distinguishes repeated ranks/suits in a six-deck shoe, and preserves
    // existing cards when a pair is split. Restored cards start seated at the table.
    private val seen=IdentityHashMap<Card,Boolean>().apply {
        game.dealer.forEach { put(it,true) }
        game.hands.forEach { hand -> hand.cards.forEach { put(it,true) } }
    }
    private val landed=IdentityHashMap<Card,Boolean>().apply { putAll(seen) }
    var arrivalRevision by mutableIntStateOf(0)
    fun hasArrived(card:Card):Boolean { arrivalRevision;return landed.containsKey(card) }
    fun visible(cards:List<Card>)=cards.filter { hasArrived(it) }
    fun landed(card:Card){landed[card]=true;arrivalRevision++}
    val pending get()=busy || game.dealer.any{!hasArrived(it)} || game.hands.any{h->h.cards.any{!hasArrived(it)}}
    val queue=mutableStateListOf<CardFlight>()
    var motionPulse by mutableIntStateOf(0)
    var releasePulse by mutableIntStateOf(0)
    var gestureProgress by mutableFloatStateOf(-1f)
    var pushRight by mutableStateOf(false)
    var spriteLocal=Rect.Zero
    fun localHand(p:Float):Offset {
        val r=spriteLocal
        val shoe=Offset(r.left+r.width*.28f,r.top+r.height*.94f)
        val middle=Offset(r.left+r.width*.33f,r.top+r.height*.94f)
        val release=Offset(r.left+r.width*(if(pushRight).84f else .16f),r.top+r.height*.98f)
        return when {p<.38f->shoe;p<.55f->shoe+(middle-shoe)*((p-.38f)/.17f);else->middle+(release-middle)*((p-.55f)/.13f).coerceIn(0f,1f)}
    }
    fun releaseOrigin():Offset?=dealerBounds?.let { it.topLeft+localHand(.68f)-rootOrigin }
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
    var arrived by remember(flights,identity) { mutableStateOf(flights==null || flights.hasArrived(card)) }
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
            flights.pushRight=(active.destination()?.center?.x ?: 0f) >= (flights.dealerBounds?.center?.x ?: 0f)
            val start=flights.releaseOrigin()
            if(start!=null && active.destination()!=null) {
                origin=start;progress.snapTo(0f);moving=true
                var released=false
                progress.animateTo(1f,tween(1080,easing=LinearEasing)) { flights.gestureProgress=value; if(value>=.68f&&!released){flights.releasePulse++;released=true} }
            }
            } finally {
                flights.landed(active.card);active.arrive();moving=false;flights.gestureProgress=-1f
                flights.queue.remove(active)
            }
        }
    }
    Box(Modifier.fillMaxSize()) {
        if(moving && active!=null && progress.value>=.68f) active.destination()?.let { rect ->
            val density=LocalDensity.current
            val end=rect.center-flights.rootOrigin
            val p=((progress.value-.68f)/.32f).coerceIn(0f,1f)
            val center=origin+(end-origin)*p
            val w=with(density){rect.width.toDp()};val h=with(density){rect.height.toDp()}
            Box(Modifier.offset { IntOffset((center.x-rect.width/2).roundToInt(),(center.y-rect.height/2).roundToInt()) }
                .testTag("dealing-card").graphicsLayer { rotationZ=-6f*(1f-p);scaleX=.55f+.45f*p;scaleY=.35f+.65f*p }) {
                CardView(active.face(),w,h)
            }
        }
    }
}
