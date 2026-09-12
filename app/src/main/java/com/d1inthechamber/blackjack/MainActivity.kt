package com.d1inthechamber.blackjack

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

data class Card(val rank: String, val suit: String) {
    val value: Int get() = when (rank) { "A" -> 11; "K", "Q", "J" -> 10; else -> rank.toInt() }
    override fun toString() = "$rank$suit"
}

class Deck {
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
    fun draw(): Card {
        if (cards.size < 15) reset()
        return cards.removeAt(cards.lastIndex)
    }
    fun remaining() = cards.size
}

fun score(hand: List<Card>): Int {
    var total = hand.sumOf { it.value }
    var aces = hand.count { it.rank == "A" }
    while (total > 21 && aces-- > 0) total -= 10
    return total
}
fun blackjack(hand: List<Card>) = hand.size == 2 && score(hand) == 21
fun isPair(hand: List<Card>) = hand.size == 2 && hand[0].value == hand[1].value
fun canSplitHand(hand: List<Card>, handCount: Int, availableBankroll: Int, wager: Int) =
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

class PlayerHand(initialCards: List<Card>, var wager: Int, var finished: Boolean = false, var doubled: Boolean = false, val fromSplit: Boolean = false) {
    val cards = initialCards.toMutableStateList()
    fun total() = score(cards)
}

class BlackjackState {
    var bankroll by mutableIntStateOf(1000)
    var bet by mutableIntStateOf(0)
    var dealer by mutableStateOf(listOf<Card>())
    var hands by mutableStateOf(listOf<PlayerHand>())
    var activeHand by mutableIntStateOf(0)
    var message by mutableStateOf("Place your bet")
    var inRound by mutableStateOf(false)
    var finished by mutableStateOf(false)
    var dealing by mutableStateOf(false)
    var dealStep by mutableIntStateOf(0)
    var drawPulse by mutableIntStateOf(0)
    private var deck = Deck()
    val deckRemaining: Int get() = deck.remaining()

    private fun drawCard(): Card { val card = deck.draw(); drawPulse++; return card }
    fun addBet(amount: Int) { if (!inRound && !dealing && bet + amount <= bankroll) bet += amount }
    fun clearBet() { if (!inRound && !dealing) bet = 0 }

    fun beginDeal() {
        if (bet <= 0 || inRound || dealing) return
        bankroll -= bet
        hands = listOf(PlayerHand(mutableListOf(), bet))
        dealer = emptyList()
        activeHand = 0
        finished = false
        inRound = true
        dealing = true
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
            if (blackjack(hands[0].cards) || blackjack(dealer)) finishRound()
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
        if (first.cards[0].rank == "A") {
            first.finished = true
            second.finished = true
            advanceOrFinish()
        }
    }
    fun canSplit(): Boolean {
        if (!canAct() || hands[activeHand].cards.size != 2) return false
        val hand = hands[activeHand]
        return canSplitHand(hand.cards, hands.size, bankroll, hand.wager)
    }
    private fun canAct() = inRound && !dealing && !finished && activeHand in hands.indices && !hands[activeHand].finished
    private fun finishActiveHand() { hands[activeHand].finished = true; hands = hands.toList(); advanceOrFinish() }
    private fun advanceOrFinish() {
        val next = hands.indexOfFirst { !it.finished }
        if (next >= 0) { activeHand = next; message = "Your move — hand ${next + 1} of ${hands.size}" } else finishRound()
    }
    private fun finishRound() {
        if (hands.any { it.total() <= 21 }) {
            while (dealerMustHit(dealer)) dealer = dealer + drawCard()
        }
        val results = hands.map { hand ->
            resolveHand(hand.cards, dealer, blackjackEligible = !hand.fromSplit).also { result ->
                when (result) {
                    HandResult.BLACKJACK -> bankroll += (hand.wager * 2.5).toInt()
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
    }
    fun newRound() { bet = 0; hands = emptyList(); dealer = emptyList(); message = "Place your bet"; finished = false; inRound = false; dealing = false; dealStep = 0 }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) { super.onCreate(savedInstanceState); setContent { BlackjackApp() } }
}

@Composable
fun BlackjackApp() {
    val game = remember { BlackjackState() }
    LaunchedEffect(game.dealStep, game.dealing) {
        if (game.dealing) { delay(420); game.dealNextCard() }
    }
    MaterialTheme(colorScheme = darkColorScheme(primary = Gold, secondary = GoldDeep)) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val wide = maxWidth >= 600.dp
            val cardWidth = if (wide) 84.dp else 68.dp
            val cardHeight = if (wide) 120.dp else 96.dp
            Box(Modifier.fillMaxSize().background(Color(0xFF030907))) {
                CasinoTableBackdrop()
                Column(
                    modifier = Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)
                        .padding(horizontal = if (wide) 28.dp else 12.dp, vertical = 10.dp)
                        .widthIn(max = 1080.dp).align(Alignment.TopCenter),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
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
                                DealerAvatar(82.dp)
                                Spacer(Modifier.height(8.dp))
                                GamePanel("DEALER", game.dealer, game.inRound && !game.finished, cardWidth, cardHeight, Modifier.fillMaxWidth())
                            }
                            Column(Modifier.weight(1.35f), horizontalAlignment = Alignment.CenterHorizontally) {
                                CasinoBadge(game.message)
                                Spacer(Modifier.height(10.dp))
                                game.hands.forEachIndexed { index, hand ->
                                    PlayerHandPanel(index, hand, index == game.activeHand && game.inRound && !game.dealing, cardWidth, cardHeight)
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
                        DealerAvatar(58.dp)
                        Spacer(Modifier.height(5.dp))
                        GamePanel("DEALER", game.dealer, game.inRound && !game.finished, cardWidth, cardHeight, Modifier.fillMaxWidth())
                        Spacer(Modifier.height(7.dp))
                        game.hands.forEachIndexed { index, hand ->
                            PlayerHandPanel(index, hand, index == game.activeHand && game.inRound && !game.dealing, cardWidth, cardHeight)
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
fun DealerAvatar(avatarSize: androidx.compose.ui.unit.Dp) {
    Canvas(Modifier.size(avatarSize)) {
        val c = Offset(size.width / 2f, size.height / 2f)
        drawCircle(Brush.radialGradient(listOf(Color(0xFFFFB74D), Color(0xFF7B1F36), Color(0xFF071A2D)), c, size.minDimension / 2f))
        drawCircle(Color(0xFFE0A06D), radius = size.minDimension * .18f, center = Offset(c.x, c.y * .72f))
        drawArc(Color(0xFF2B1B18), 190f, 160f, false, Offset(c.x - size.width * .19f, c.y * .35f), Size(size.width * .38f, size.height * .25f), style = Stroke(size.width * .08f))
        drawLine(Color(0xFF101820), Offset(c.x - size.width * .17f, c.y * .68f), Offset(c.x + size.width * .17f, c.y * .68f), strokeWidth = size.width * .09f, cap = StrokeCap.Round)
        drawLine(Gold, Offset(c.x - size.width * .34f, size.height * .86f), Offset(c.x, size.height * .62f), strokeWidth = size.width * .22f, cap = StrokeCap.Round)
        drawLine(GoldDeep, Offset(c.x + size.width * .34f, size.height * .86f), Offset(c.x, size.height * .62f), strokeWidth = size.width * .22f, cap = StrokeCap.Round)
        drawLine(Color.White.copy(alpha = .75f), Offset(size.width * .17f, size.height * .83f), Offset(size.width * .83f, size.height * .83f), strokeWidth = size.width * .035f)
        drawCircle(Gold, radius = size.width * .045f, center = Offset(c.x, size.height * .76f))
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

@Composable
fun Header(bankroll: Int, wide: Boolean) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("ROYAL FELT", fontSize = if (wide) 14.sp else 11.sp, letterSpacing = 5.sp, color = Gold.copy(alpha = .85f), fontWeight = FontWeight.Bold)
        Text("BLACKJACK", fontSize = if (wide) 40.sp else 30.sp, fontWeight = FontWeight.Black, color = Color.White)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("♠", color = CardRed, fontSize = 13.sp); Text("  VIRTUAL CASINO  ", fontSize = 10.sp, letterSpacing = 2.sp, color = Color.White.copy(alpha = .58f)); Text("♥", color = CardRed, fontSize = 13.sp)
        }
        Spacer(Modifier.height(5.dp))
        Surface(shape = RoundedCornerShape(50), color = Color.Black.copy(alpha = .48f), border = androidx.compose.foundation.BorderStroke(1.dp, Gold.copy(alpha = .55f))) {
            Text("$ $bankroll", modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp), color = Gold, fontWeight = FontWeight.Black, fontSize = 18.sp)
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
    val deckScale by animateFloatAsState(if (pulse > 0) 1.04f else 1f, tween(180), label = "deckPulse")
    Surface(modifier = Modifier.fillMaxWidth().scale(deckScale), shape = RoundedCornerShape(18.dp), color = Color(0xFF030A07).copy(alpha = .58f), border = androidx.compose.foundation.BorderStroke(1.dp, Gold.copy(alpha = .28f))) {
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

@Composable
fun PlayerHandPanel(index: Int, hand: PlayerHand, active: Boolean, cardWidth: androidx.compose.ui.unit.Dp, cardHeight: androidx.compose.ui.unit.Dp) {
    val pulse by animateFloatAsState(if (active) 1.018f else 1f, tween(400), label = "handPulse")
    Surface(modifier = Modifier.fillMaxWidth().scale(pulse), shape = RoundedCornerShape(18.dp), color = if (active) Gold.copy(alpha = .07f) else Color(0xFF030A07).copy(alpha = .4f), border = androidx.compose.foundation.BorderStroke(1.dp, if (active) Gold else Color.White.copy(alpha = .1f))) {
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
        delay(80)
        scroll.animateScrollTo(scroll.maxValue)
    }
    Row(modifier = Modifier.fillMaxWidth().horizontalScroll(scroll), horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally), verticalAlignment = Alignment.CenterVertically) {
        cards.forEachIndexed { i, card ->
            key("${card}-${i}") {
                AnimatedVisibility(visible = true, enter = slideInHorizontally(initialOffsetX = { it * 2 }, animationSpec = tween(460)) + slideInVertically(initialOffsetY = { -it / 3 }, animationSpec = tween(460)) + fadeIn(tween(240)) + scaleIn(initialScale = .68f, animationSpec = tween(460))) {
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
                GameButton("DEAL", !game.inRound && !game.dealing && game.bet > 0, Modifier.weight(1f)) { game.beginDeal() }
                GameButton("HIT", game.inRound && !game.dealing && !game.finished, Modifier.weight(1f)) { game.hit() }
                GameButton("STAND", game.inRound && !game.dealing && !game.finished, Modifier.weight(1f)) { game.stand() }
                GameButton("DOUBLE", game.inRound && !game.dealing && !game.finished && game.hands.getOrNull(game.activeHand)?.cards?.size == 2 && game.bankroll >= (game.hands.getOrNull(game.activeHand)?.wager ?: Int.MAX_VALUE), Modifier.weight(1f)) { game.doubleDown() }
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
