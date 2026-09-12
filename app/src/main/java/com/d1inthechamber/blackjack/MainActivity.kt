package com.d1inthechamber.blackjack

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.ViewModelProvider
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.delay

data class Card(val rank: String, val suit: String) {
    val value: Int get() = when (rank) { "A" -> 11; "K", "Q", "J" -> 10; else -> rank.toInt() }
    override fun toString() = "$rank$suit"
}

interface CardShoe {
    fun draw(): Card
    fun remaining(): Int
}

class Deck : CardShoe {
    private val cards = mutableListOf<Card>()
    init { reset() }
    fun reset() {
        cards.clear()
        repeat(6) {
            listOf("♠", "♥", "♦", "♣").forEach { suit ->
                (2..10).forEach { rank -> cards += Card(rank.toString(), suit) }
                listOf("J", "Q", "K", "A").forEach { rank -> cards += Card(rank, suit) }
            }
        }
        cards.shuffle()
    }
    override fun draw(): Card {
        if (cards.size < 15) reset()
        return cards.removeAt(cards.lastIndex)
    }
    override fun remaining() = cards.size
}

fun score(hand: List<Card>): Int {
    var total = hand.sumOf { it.value }
    var aces = hand.count { it.rank == "A" }
    while (total > 21 && aces-- > 0) total -= 10
    return total
}
fun blackjack(hand: List<Card>) = hand.size == 2 && score(hand) == 21
fun isPair(hand: List<Card>) = hand.size == 2 && hand[0].value == hand[1].value
fun canSplitHand(hand: List<Card>, handCount: Int, availableBankroll: Double, wager: Int) =
    handCount < 3 && isPair(hand) && availableBankroll >= wager
fun isSoft(hand: List<Card>): Boolean {
    var total = hand.sumOf { it.value }
    var highAces = hand.count { it.rank == "A" }
    while (total > 21 && highAces > 0) {
        total -= 10
        highAces--
    }
    return total <= 21 && highAces > 0
}
fun dealerMustHit(hand: List<Card>) = score(hand) < 17

enum class HandResult { BLACKJACK, WIN, PUSH, LOSE }

fun resolveHand(player: List<Card>, dealer: List<Card>, blackjackEligible: Boolean = true): HandResult {
    val playerTotal = score(player)
    val dealerTotal = score(dealer)
    val playerBlackjack = blackjackEligible && blackjack(player)
    val dealerBlackjack = blackjack(dealer)
    return when {
        playerTotal > 21 -> HandResult.LOSE
        dealerTotal > 21 -> if (playerBlackjack) HandResult.BLACKJACK else HandResult.WIN
        playerBlackjack && dealerBlackjack -> HandResult.PUSH
        dealerBlackjack -> HandResult.LOSE
        playerBlackjack -> HandResult.BLACKJACK
        playerTotal > dealerTotal -> HandResult.WIN
        playerTotal == dealerTotal -> HandResult.PUSH
        else -> HandResult.LOSE
    }
}

class PlayerHand(initialCards: List<Card>, wager: Int, finished: Boolean = false, doubled: Boolean = false, val fromSplit: Boolean = false) {
    var wager by mutableIntStateOf(wager)
    var finished by mutableStateOf(finished)
    var doubled by mutableStateOf(doubled)
    val cards = initialCards.toMutableStateList()
    fun total() = score(cards)
}

class BlackjackState(private val deck: CardShoe = Deck()) {
    var bankroll by mutableDoubleStateOf(1000.0)
    var bet by mutableIntStateOf(0)
    var dealer by mutableStateOf(listOf<Card>())
    var hands by mutableStateOf(listOf<PlayerHand>())
    var activeHand by mutableIntStateOf(0)
    var message by mutableStateOf("Place your bet")
    var inRound by mutableStateOf(false)
    var finished by mutableStateOf(false)
    var dealing by mutableStateOf(false)
    var dealerPlaying by mutableStateOf(false)
    var dealStep by mutableIntStateOf(0)
    var drawPulse by mutableIntStateOf(0)
    val deckRemaining: Int get() = deck.remaining()

    private fun drawCard(): Card { val card = deck.draw(); drawPulse++; return card }
    fun addBet(amount: Int) { if (!inRound && !dealing && bet + amount <= bankroll) bet += amount }
    fun clearBet() { if (!inRound && !dealing) bet = 0 }
    fun canDeal() = bet > 0 && bet <= bankroll && !inRound && !dealing

    fun beginDeal() {
        if (!canDeal()) return
        bankroll -= bet
        hands = listOf(PlayerHand(mutableListOf(), bet))
        dealer = emptyList()
        activeHand = 0
        finished = false
        inRound = true
        dealing = true
        dealerPlaying = false
        dealStep = 1
        message = "Dealing..."
    }

    fun dealNextCard() {
        if (!dealing) return
        when (dealStep) {
            1 -> hands[0].cards += drawCard()
            2 -> dealer = dealer + drawCard()
            3 -> hands[0].cards += drawCard()
            4 -> dealer = dealer + drawCard()
        }
        hands = hands.toList()
        if (dealStep >= 4) {
            dealing = false
            message = "Your move"
            if (blackjack(hands[0].cards) || blackjack(dealer)) settleRound()
        } else dealStep++
    }

    fun hit() {
        if (!canAct()) return
        val hand = hands[activeHand]
        hand.cards += drawCard()
        hands = hands.toList()
        if (hand.total() >= 21) finishActiveHand()
    }
    fun stand() {
        if (!canAct()) return
        hands[activeHand].finished = true
        hands = hands.toList()
        advanceOrFinish()
    }
    fun doubleDown() {
        if (!canAct() || hands[activeHand].cards.size != 2 || blackjack(hands[activeHand].cards)) return
        val hand = hands[activeHand]
        if (bankroll < hand.wager) return
        bankroll -= hand.wager
        hand.wager *= 2
        hand.doubled = true
        hand.cards += drawCard()
        hand.finished = true
        hands = hands.toList()
        advanceOrFinish()
    }
    fun split() {
        if (!canAct() || !canSplit()) return
        val original = hands[activeHand]
        bankroll -= original.wager
        val first = PlayerHand(mutableListOf(original.cards[0], drawCard()), original.wager, fromSplit = true)
        val second = PlayerHand(mutableListOf(original.cards[1], drawCard()), original.wager, fromSplit = true)
        val updated = hands.toMutableList()
        updated[activeHand] = first
        updated.add(activeHand + 1, second)
        hands = updated
        message = "Split! Play hand ${activeHand + 1}"
        val splitAces = first.cards[0].rank == "A"
        first.finished = splitAces || first.total() == 21
        second.finished = splitAces || second.total() == 21
        advanceOrFinish()
    }
    fun canSplit(): Boolean {
        if (!canAct() || hands[activeHand].cards.size != 2) return false
        val hand = hands[activeHand]
        return canSplitHand(hand.cards, hands.size, bankroll, hand.wager)
    }
    fun canAct() = inRound && !dealing && !dealerPlaying && !finished && activeHand in hands.indices && !hands[activeHand].finished
    private fun finishActiveHand() { hands[activeHand].finished = true; hands = hands.toList(); advanceOrFinish() }
    private fun advanceOrFinish() {
        val next = hands.indexOfFirst { !it.finished }
        if (next >= 0) { activeHand = next; message = "Your move — hand ${next + 1} of ${hands.size}" } else startDealerTurn()
    }
    private fun startDealerTurn() {
        if (hands.all { it.total() > 21 }) {
            settleRound()
        } else {
            dealerPlaying = true
            message = "Dealer's turn..."
        }
    }
    fun playDealerStep() {
        if (!dealerPlaying) return
        if (dealerMustHit(dealer)) dealer = dealer + drawCard()
        if (!dealerMustHit(dealer)) settleRound()
    }
    private fun settleRound() {
        val results = hands.map { hand ->
            resolveHand(hand.cards, dealer, blackjackEligible = !hand.fromSplit).also { result ->
                when (result) {
                    HandResult.BLACKJACK -> bankroll += hand.wager * 2.5
                    HandResult.WIN -> bankroll += hand.wager * 2
                    HandResult.PUSH -> bankroll += hand.wager
                    HandResult.LOSE -> Unit
                }
            }
        }
        val wins = results.count { it == HandResult.WIN || it == HandResult.BLACKJACK }
        val pushes = results.count { it == HandResult.PUSH }
        message = when {
            results.any { it == HandResult.BLACKJACK } -> "BLACKJACK! 3:2 payout"
            score(dealer) > 21 && wins > 0 -> "DEALER BUSTS — YOU WIN!"
            wins > 0 -> if (hands.size > 1) "$wins hand${if (wins == 1) "" else "s"} won!" else "You win!"
            pushes > 0 -> "Push — bet returned"
            hands.all { it.total() > 21 } -> "Player busts"
            else -> "Dealer wins"
        }
        finished = true
        inRound = false
        dealing = false
        dealerPlaying = false
    }
    fun newRound() { bet = 0; hands = emptyList(); dealer = emptyList(); message = "Place your bet"; finished = false; inRound = false; dealing = false; dealerPlaying = false; dealStep = 0 }
}

class BlackjackViewModel : ViewModel() {
    val game = BlackjackState()
}

class MainActivity : ComponentActivity() {
    private val gameViewModel: BlackjackViewModel by lazy { ViewModelProvider(this)[BlackjackViewModel::class.java] }
    override fun onCreate(savedInstanceState: Bundle?) { super.onCreate(savedInstanceState); setContent { BlackjackApp(gameViewModel.game) } }
}

@Composable
fun BlackjackApp(game: BlackjackState) {
    var soundEnabled by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(true) }
    var ambienceEnabled by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(true) }
    TableSounds(game, soundEnabled)

    LaunchedEffect(game.dealStep, game.dealing) {
        if (game.dealing) { delay(420); game.dealNextCard() }
    }
    LaunchedEffect(game, game.dealerPlaying) {
        while (game.dealerPlaying) {
            delay(520)
            game.playDealerStep()
        }
    }
    MaterialTheme(colorScheme = darkColorScheme(primary = Gold, secondary = GoldDeep)) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val wide = maxWidth >= 600.dp
            val cardWidth = if (wide) 84.dp else 68.dp
            val cardHeight = if (wide) 120.dp else 96.dp
            Box(Modifier.fillMaxSize().background(Color(0xFF030907))) {
                Image(
                    painter = painterResource(R.drawable.casino_dingy_1970s),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                val neonFlicker by rememberInfiniteTransition(label = "brokenNeon").animateFloat(
                    initialValue = .03f,
                    targetValue = .11f,
                    animationSpec = infiniteRepeatable(tween(850), RepeatMode.Reverse),
                    label = "neonGlow"
                )
                Box(Modifier.fillMaxSize().background(Color(0xFF7A1237).copy(alpha = if (ambienceEnabled) neonFlicker else .06f)))
                Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = .28f)))
                if (ambienceEnabled) RoomCreatures()
                Column(
                    modifier = Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)
                        .padding(horizontal = if (wide) 28.dp else 12.dp, vertical = 10.dp)
                        .widthIn(max = 1080.dp).align(Alignment.TopCenter),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                        TextButton(onClick = { soundEnabled = !soundEnabled }) { Text(if (soundEnabled) "SOUND ON" else "SOUND OFF") }
                        TextButton(onClick = { ambienceEnabled = !ambienceEnabled }) { Text(if (ambienceEnabled) "AMBIENCE ON" else "AMBIENCE OFF") }
                    }
                    Header(game.bankroll, wide)
                    Spacer(Modifier.height(if (wide) 12.dp else 6.dp))
                    Column(
                        modifier = Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                    if (wide) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(22.dp)) {
                            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                                TableSectionLabel("DEALER")
                                DealerPortrait(game, true, ambienceEnabled)
                                Spacer(Modifier.height(8.dp))
                                GamePanel("DEALER", game.dealer, game.inRound && !game.dealerPlaying && !game.finished, cardWidth, cardHeight, Modifier.fillMaxWidth())
                            }
                            Column(Modifier.weight(1.35f), horizontalAlignment = Alignment.CenterHorizontally) {
                                CasinoBadge(game.message)
                                Spacer(Modifier.height(10.dp))
                                game.hands.forEachIndexed { index, hand ->
                                    PlayerHandPanel(index, hand, index == game.activeHand && game.canAct(), cardWidth, cardHeight)
                                    if (index < game.hands.lastIndex) Spacer(Modifier.height(7.dp))
                                }
                            }
                            Column(Modifier.weight(.55f), horizontalAlignment = Alignment.CenterHorizontally) {
                                DeckDisplay(game.deckRemaining, game.drawPulse, true)
                            }
                        }
                    } else {
                        CasinoBadge(game.message)
                        Spacer(Modifier.height(7.dp))
                        DealerPortrait(game, false, ambienceEnabled)
                        Spacer(Modifier.height(5.dp))
                        GamePanel("DEALER", game.dealer, game.inRound && !game.dealerPlaying && !game.finished, cardWidth, cardHeight, Modifier.fillMaxWidth())
                        Spacer(Modifier.height(7.dp))
                        game.hands.forEachIndexed { index, hand ->
                            PlayerHandPanel(index, hand, index == game.activeHand && game.canAct(), cardWidth, cardHeight,
                                revealHand = index == game.activeHand && !game.dealing)
                            if (index < game.hands.lastIndex) Spacer(Modifier.height(6.dp))
                        }
                        Spacer(Modifier.height(7.dp))
                        DeckDisplay(game.deckRemaining, game.drawPulse, false)
                    }
                    Spacer(Modifier.height(10.dp))
                    }
                    BettingPanel(game, wide)
                }
            }
        }
    }
}

@Composable
fun DealerPortrait(game: BlackjackState, wide: Boolean, animated: Boolean) {
    val idle = rememberInfiniteTransition(label = "dealerIdle")
    val breath by idle.animateFloat(0f, 1f, infiniteRepeatable(tween(2800), RepeatMode.Reverse), label = "breathing")

    val dealerScale by animateFloatAsState(
        targetValue = when {
            game.dealing || game.dealerPlaying -> 1.06f
            game.finished -> 1.03f
            else -> 1f
        },
        animationSpec = tween(320),
        label = "dealerReaction"
    )
    val line = when {
        game.dealing -> "Coming right up..."
        game.dealerPlaying -> "Let's see what the house has..."
        game.message.contains("BLACKJACK") -> "Now that's a Vegas hand!"
        game.message.contains("DEALER BUSTS") -> "The house takes a tumble!"
        game.message.contains("You win", ignoreCase = true) -> "Well played, high roller."
        game.message.contains("Dealer wins") -> "The house wins this one."
        game.inRound -> "Your move. Hit or stand?"
        else -> "Place your bets!"
    }
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Image(
            painter = painterResource(R.drawable.dealer_old_vegas),
            contentDescription = "Old Vegas blackjack dealer",
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxWidth().height(if (wide) 190.dp else 132.dp).scale(if (animated) dealerScale + breath * .009f else 1f).offset(y = if (animated) (-2f * breath).dp else 0.dp)
        )
        AnimatedContent(targetState = line, label = "dealerLine") { spokenLine ->
            Surface(color = Color(0xFFFAE8B2), shape = RoundedCornerShape(50), shadowElevation = 7.dp) {
                Text(spokenLine, color = Color(0xFF3A1715), fontWeight = FontWeight.Bold, fontSize = if (wide) 11.sp else 10.sp, modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp), textAlign = TextAlign.Center)
            }
        }
    }
}

@Composable
fun CasinoTableBackdrop() {
    Canvas(Modifier.fillMaxSize()) {
        drawRect(Brush.verticalGradient(listOf(Color(0xFF06150E), Color(0xFF020806))))
        val center = Offset(size.width / 2f, size.height * .48f)
        val tableW = size.width * 1.35f
        val tableH = size.height * .86f
        drawOval(Brush.radialGradient(listOf(Color(0xFF1D5A3C), Color(0xFF0A2C1D), Color(0xFF04140D)), center, maxOf(tableW, tableH)),
            topLeft = Offset(center.x - tableW / 2f, center.y - tableH / 2f), size = Size(tableW, tableH))
        drawOval(Color(0xFF7A5A21).copy(alpha = .55f), topLeft = Offset(center.x - tableW / 2f, center.y - tableH / 2f), size = Size(tableW, tableH), style = Stroke(width = 18f))
        drawOval(Color(0xFFD7B64C).copy(alpha = .25f), topLeft = Offset(center.x - tableW / 2f + 15f, center.y - tableH / 2f + 15f), size = Size(tableW - 30f, tableH - 30f), style = Stroke(width = 2f))
        val arcW = size.width * .78f
        val arcH = size.height * .44f
        drawArc(Color.White.copy(alpha = .12f), 205f, 130f, false, Offset(center.x - arcW / 2f, center.y + size.height * .16f), Size(arcW, arcH), style = Stroke(width = 3f, cap = StrokeCap.Round))
        drawCircle(Color(0xFF000000).copy(alpha = .18f), radius = size.minDimension * .12f, center = Offset(center.x, center.y + size.height * .02f))
    }
}

private val Felt = Color(0xFF071A12)
private val Felt2 = Color(0xFF123A27)
private val Gold = Color(0xFFFFD54F)
private val GoldDeep = Color(0xFFC79A20)
private val CardRed = Color(0xFFD32F2F)
private val DeckBlue = Color(0xFF173B70)
private val DeckBlue2 = Color(0xFF245A9B)

fun formatChips(amount: Double): String =
    if (amount % 1.0 == 0.0) amount.toLong().toString() else amount.toString()

@Composable
fun Header(bankroll: Double, wide: Boolean) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("ROYAL FELT", fontSize = if (wide) 14.sp else 11.sp, letterSpacing = 5.sp, color = Gold.copy(alpha = .85f), fontWeight = FontWeight.Bold)
        Text("BLACKJACK", fontSize = if (wide) 40.sp else 30.sp, fontWeight = FontWeight.Black, color = Color.White)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("♠", color = CardRed, fontSize = 13.sp); Text("  VIRTUAL CASINO  ", fontSize = 10.sp, letterSpacing = 2.sp, color = Color.White.copy(alpha = .58f)); Text("♥", color = CardRed, fontSize = 13.sp)
        }
        Spacer(Modifier.height(5.dp))
        Surface(shape = RoundedCornerShape(50), color = Color.Black.copy(alpha = .48f), border = androidx.compose.foundation.BorderStroke(1.dp, Gold.copy(alpha = .55f))) {
            Text("$ ${formatChips(bankroll)}", modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp), color = Gold, fontWeight = FontWeight.Black, fontSize = 18.sp)
        }
    }
}

@Composable
fun TableSectionLabel(text: String) {
    Text(text, color = Gold, fontWeight = FontWeight.Black, letterSpacing = 3.sp, fontSize = 12.sp, modifier = Modifier.padding(bottom = 4.dp))
}

@Composable
fun CasinoBadge(message: String) {
    AnimatedContent(targetState = message, transitionSpec = { fadeIn(tween(180)) togetherWith fadeOut(tween(120)) }, label = "casinoMessage") { text ->
        Surface(shape = RoundedCornerShape(50), color = Color(0xFF050B08).copy(alpha = .62f), border = androidx.compose.foundation.BorderStroke(1.dp, Gold.copy(alpha = .5f))) {
            Row(Modifier.padding(horizontal = 18.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("✦", color = Gold, fontSize = 15.sp)
                Spacer(Modifier.width(9.dp))
                Text(text.uppercase(), color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Black, letterSpacing = .8.sp, textAlign = TextAlign.Center)
                Spacer(Modifier.width(9.dp))
                Text("✦", color = Gold, fontSize = 15.sp)
            }
        }
    }
}

@Composable
fun DeckDisplay(remaining: Int, pulse: Int, wide: Boolean) {
    val deckScale = remember { Animatable(1f) }
    LaunchedEffect(pulse) {
        if (pulse > 0) {
            deckScale.snapTo(1f)
            deckScale.animateTo(1.08f, tween(90))
            deckScale.animateTo(1f, tween(180))
        }
    }
    Surface(modifier = Modifier.fillMaxWidth().scale(deckScale.value), shape = RoundedCornerShape(18.dp), color = Color(0xFF030A07).copy(alpha = .58f), border = androidx.compose.foundation.BorderStroke(1.dp, Gold.copy(alpha = .28f))) {
        Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(modifier = Modifier.width(if (wide) 66.dp else 54.dp).height(if (wide) 86.dp else 68.dp), contentAlignment = Alignment.Center) {
                repeat(4) { i ->
                    Surface(modifier = Modifier.offset(x = (i * 3).dp, y = (i * 2).dp).fillMaxSize(), shape = RoundedCornerShape(8.dp), color = DeckBlue, shadowElevation = 6.dp) {
                        Box(Modifier.border(2.dp, DeckBlue2, RoundedCornerShape(8.dp)), contentAlignment = Alignment.Center) { Text("♠", color = Gold.copy(alpha = .85f), fontSize = if (wide) 25.sp else 20.sp) }
                    }
                }
            }
            Spacer(Modifier.height(7.dp))
            Text("SHOE", color = Gold, fontWeight = FontWeight.Black, letterSpacing = 2.sp, fontSize = 11.sp)
            Text("$remaining", color = Color.White, fontWeight = FontWeight.Black, fontSize = 22.sp)
            Text("CARDS LEFT", color = Color.White.copy(alpha = .5f), fontSize = 9.sp, letterSpacing = 1.5.sp)
        }
    }
}

@Composable
fun GamePanel(title: String, cards: List<Card>, hideSecond: Boolean, cardWidth: androidx.compose.ui.unit.Dp, cardHeight: androidx.compose.ui.unit.Dp, modifier: Modifier) {
    Surface(modifier = modifier.animateContentSize(), shape = RoundedCornerShape(20.dp), color = Color(0xFF030A07).copy(alpha = .48f), border = androidx.compose.foundation.BorderStroke(1.dp, Gold.copy(alpha = .2f))) {
        Column(Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            CardsRow(cards, hideSecond, cardWidth, cardHeight)
            if (cards.isNotEmpty()) Text(if (hideSecond) "HOLE CARD" else "TOTAL ${score(cards)}", color = Gold, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp, modifier = Modifier.padding(top = 5.dp))
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PlayerHandPanel(index: Int, hand: PlayerHand, active: Boolean, cardWidth: androidx.compose.ui.unit.Dp, cardHeight: androidx.compose.ui.unit.Dp, revealHand: Boolean = false) {
    val handRequester = remember { BringIntoViewRequester() }
    LaunchedEffect(revealHand, hand, hand.cards.size) {
        if (revealHand) {
            // Wait for the added card/new split hand to be measured before scrolling.
            withFrameNanos { }
            withFrameNanos { }
            handRequester.bringIntoView()
        }
    }
    val pulse by animateFloatAsState(if (active) 1.018f else 1f, tween(400), label = "handPulse")
    Surface(modifier = Modifier.fillMaxWidth().bringIntoViewRequester(handRequester).scale(pulse), shape = RoundedCornerShape(18.dp), color = if (active) Gold.copy(alpha = .07f) else Color(0xFF030A07).copy(alpha = .4f), border = androidx.compose.foundation.BorderStroke(1.dp, if (active) Gold else Color.White.copy(alpha = .1f))) {
        Column(Modifier.padding(9.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("PLAYER ${index + 1}", color = if (active) Gold else Color.White, fontWeight = FontWeight.Black, letterSpacing = 1.sp, fontSize = 12.sp)
                Spacer(Modifier.width(10.dp)); Text("${score(hand.cards)}  •  ${hand.wager} CHIPS", color = Color.White.copy(alpha = .68f), fontSize = 11.sp)
            }
            Spacer(Modifier.height(5.dp)); CardsRow(hand.cards, false, cardWidth, cardHeight)
        }
    }
}

@Composable
fun CardsRow(cards: List<Card>, hideSecond: Boolean, cardWidth: androidx.compose.ui.unit.Dp, cardHeight: androidx.compose.ui.unit.Dp) {
    val scroll = rememberScrollState()
    LaunchedEffect(cards.size) {
        delay(500)
        scroll.animateScrollTo(scroll.maxValue)
    }
    Row(modifier = Modifier.fillMaxWidth().horizontalScroll(scroll), horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally), verticalAlignment = Alignment.CenterVertically) {
        cards.forEachIndexed { i, card ->
            key("${card}-${i}") {
                val entry = remember { MutableTransitionState(false).apply { targetState = true } }
                AnimatedVisibility(visibleState = entry, enter = slideInHorizontally(initialOffsetX = { it * 2 }, animationSpec = tween(460)) + slideInVertically(initialOffsetY = { -it / 3 }, animationSpec = tween(460)) + fadeIn(tween(240)) + scaleIn(initialScale = .68f, animationSpec = tween(460))) {
                    CardView(if (hideSecond && i == 1) "?" else card.toString(), cardWidth, cardHeight)
                }
            }
        }
    }
}

@Composable
fun CardView(text: String, width: androidx.compose.ui.unit.Dp, height: androidx.compose.ui.unit.Dp) {
    val red = text.contains("♥") || text.contains("♦")
    val back = text == "?"
    Surface(shape = RoundedCornerShape(12.dp), color = if (back) DeckBlue else Color.White, shadowElevation = 14.dp, modifier = Modifier.size(width, height)) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.border(2.dp, if (back) GoldDeep else Color.LightGray, RoundedCornerShape(12.dp))) {
            if (back) {
                Box(Modifier.fillMaxSize().padding(5.dp).border(1.dp, DeckBlue2, RoundedCornerShape(8.dp)), contentAlignment = Alignment.Center) {
                    Text("♠", color = Gold, fontSize = if (width >= 70.dp) 30.sp else 23.sp)
                }
            } else {
                Column(Modifier.fillMaxSize().padding(5.dp), verticalArrangement = Arrangement.SpaceBetween) {
                    Text("$text", fontSize = if (width >= 70.dp) 19.sp else 15.sp, fontWeight = FontWeight.Black, color = if (red) CardRed else Color(0xFF171717))
                    Text(text, modifier = Modifier.align(Alignment.CenterHorizontally), fontSize = if (width >= 70.dp) 28.sp else 21.sp, color = if (red) CardRed else Color(0xFF171717))
                }
            }
        }
    }
}

@Composable
fun BettingPanel(game: BlackjackState, wide: Boolean) {
    Surface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp), color = Color(0xFF030A07).copy(alpha = .7f), border = androidx.compose.foundation.BorderStroke(1.dp, Gold.copy(alpha = .3f))) {
        Column(Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("BET", color = Color.White.copy(alpha = .7f), fontWeight = FontWeight.Bold, letterSpacing = 2.sp, fontSize = 11.sp)
                Spacer(Modifier.width(8.dp)); Text("${game.bet} CHIPS", color = Gold, fontWeight = FontWeight.Black, fontSize = 16.sp)
            }
            Spacer(Modifier.height(7.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                listOf(10, 25, 50, 100).forEach { n -> ChipButton(n, enabled = !game.inRound && !game.dealing && game.bet + n <= game.bankroll) { game.addBet(n) } }
                OutlinedButton(onClick = { game.clearBet() }, enabled = !game.inRound && !game.dealing, modifier = Modifier.height(42.dp), shape = RoundedCornerShape(12.dp)) { Text("CLEAR", fontSize = 10.sp, fontWeight = FontWeight.Black) }
            }
            Spacer(Modifier.height(7.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                GameButton("DEAL", game.canDeal(), Modifier.weight(1f)) { game.beginDeal() }
                GameButton("HIT", game.canAct(), Modifier.weight(1f)) { game.hit() }
                GameButton("STAND", game.canAct(), Modifier.weight(1f)) { game.stand() }
                GameButton("DOUBLE", game.canAct() && game.hands.getOrNull(game.activeHand)?.cards?.size == 2 && game.bankroll >= (game.hands.getOrNull(game.activeHand)?.wager ?: Int.MAX_VALUE), Modifier.weight(1f)) { game.doubleDown() }
                GameButton("SPLIT", game.canSplit(), Modifier.weight(1f)) { game.split() }
                if (game.finished) GameButton("NEW", true, Modifier.weight(1f)) { game.newRound() }
            }
        }
    }
}

@Composable
fun RowScope.ChipButton(amount: Int, enabled: Boolean, onClick: () -> Unit) {
    Button(onClick = onClick, enabled = enabled, modifier = Modifier.height(42.dp).weight(1f), contentPadding = PaddingValues(0.dp), shape = RoundedCornerShape(50), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF171717), contentColor = Gold)) {
        Text("$amount", fontWeight = FontWeight.Black, fontSize = 12.sp)
    }
}

@Composable
fun GameButton(text: String, enabled: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Button(onClick = onClick, enabled = enabled, modifier = modifier.height(44.dp), shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.buttonColors(containerColor = GoldDeep, contentColor = Color.Black, disabledContainerColor = Color.White.copy(alpha = .08f), disabledContentColor = Color.White.copy(alpha = .28f)), contentPadding = PaddingValues(horizontal = 3.dp)) {
        Text(text, fontWeight = FontWeight.Black, fontSize = 10.sp, maxLines = 1)
    }
}
