package com.d1inthechamber.blackjack

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val Felt = Color(0xFF071A12)
private val Felt2 = Color(0xFF123A27)
private val Gold = Color(0xFFFFD54F)
private val GoldDeep = Color(0xFFC79A20)
private val CardRed = Color(0xFFD32F2F)
private val DeckBlue = Color(0xFF173B70)
private val DeckBlue2 = Color(0xFF245A9B)

data class Card(val rank: String, val suit: String) {
    val value: Int get() = when (rank) { "A" -> 11; "K", "Q", "J" -> 10; else -> rank.toInt() }
    override fun toString() = "$rank$suit"
}

class Deck {
    private val cards = mutableListOf<Card>()
    init { reset() }
    fun reset() {
        cards.clear()
        listOf("♠", "♥", "♦", "♣").forEach { suit ->
            (2..10).forEach { rank -> cards += Card(rank.toString(), suit) }
            listOf("J", "Q", "K", "A").forEach { rank -> cards += Card(rank, suit) }
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
fun isPair(hand: List<Card>) = hand.size == 2 && hand[0].rank == hand[1].rank

class PlayerHand(val cards: MutableList<Card>, var wager: Int, var finished: Boolean = false, var doubled: Boolean = false) {
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
    var drawPulse by mutableIntStateOf(0)
    private var deck = Deck()
    val deckRemaining: Int get() = deck.remaining()

    private fun drawCard(): Card {
        val card = deck.draw()
        drawPulse++
        return card
    }

    fun addBet(amount: Int) { if (!inRound && bet + amount <= bankroll) bet += amount }
    fun clearBet() { if (!inRound) bet = 0 }

    fun deal() {
        if (bet <= 0 || inRound) return
        bankroll -= bet
        val hand = PlayerHand(mutableListOf(drawCard(), drawCard()), bet)
        hands = listOf(hand)
        dealer = listOf(drawCard(), drawCard())
        activeHand = 0
        inRound = true
        finished = false
        message = "Your move"
        if (blackjack(hand.cards)) finishRound()
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
        if (!canAct() || hands[activeHand].cards.size != 2) return
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
        if (bankroll < original.wager) return
        bankroll -= original.wager
        val first = PlayerHand(mutableListOf(original.cards[0], drawCard()), original.wager)
        val second = PlayerHand(mutableListOf(original.cards[1], drawCard()), original.wager)
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
        return isPair(hands[activeHand].cards) && bankroll >= hands[activeHand].wager
    }

    private fun canAct() = inRound && !finished && activeHand in hands.indices && !hands[activeHand].finished

    private fun finishActiveHand() {
        hands[activeHand].finished = true
        hands = hands.toList()
        advanceOrFinish()
    }

    private fun advanceOrFinish() {
        val next = hands.indexOfFirst { !it.finished }
        if (next >= 0) {
            activeHand = next
            message = "Your move — hand ${next + 1} of ${hands.size}"
        } else finishRound()
    }

    private fun finishRound() {
        while (score(dealer) < 17) dealer = dealer + drawCard()
        hands.forEach { hand ->
            val p = hand.total()
            val d = score(dealer)
            when {
                blackjack(hand.cards) && !blackjack(dealer) -> bankroll += (hand.wager * 2.5).toInt()
                p > 21 -> Unit
                blackjack(dealer) -> Unit
                d > 21 || p > d -> bankroll += hand.wager * 2
                p == d -> bankroll += hand.wager
            }
        }
        val wins = hands.count { it.total() <= 21 && !blackjack(dealer) && (score(dealer) > 21 || it.total() > score(dealer) || blackjack(it.cards)) }
        val pushes = hands.count { it.total() <= 21 && !blackjack(it.cards) && score(dealer) <= 21 && it.total() == score(dealer) }
        message = when {
            hands.any { blackjack(it.cards) && !blackjack(dealer) } -> "BLACKJACK! 3:2 payout"
            wins > 0 -> if (hands.size > 1) "$wins hand${if (wins == 1) "" else "s"} won!" else "You win!"
            pushes > 0 -> "Push — bet returned"
            hands.all { it.total() > 21 } -> "All hands busted"
            else -> "Dealer wins"
        }
        finished = true
        inRound = false
    }

    fun newRound() {
        bet = 0
        hands = emptyList()
        dealer = emptyList()
        message = "Place your bet"
        finished = false
        inRound = false
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { BlackjackApp() }
    }
}

@Composable
fun BlackjackApp() {
    val game = remember { BlackjackState() }
    MaterialTheme(colorScheme = darkColorScheme(primary = Gold, secondary = GoldDeep)) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(Felt2, Felt, Color(0xFF03100A))))
        ) {
            val wide = maxWidth >= 600.dp
            val cardWidth = if (wide) 76.dp else 58.dp
            val cardHeight = if (wide) 108.dp else 82.dp
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.safeDrawing)
                    .padding(horizontal = if (wide) 32.dp else 16.dp, vertical = 12.dp)
                    .widthIn(max = 1000.dp)
                    .align(Alignment.TopCenter),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Header(game.bankroll, wide)
                Spacer(Modifier.height(if (wide) 14.dp else 8.dp))
                DeckDisplay(game.deckRemaining, game.drawPulse, wide)
                Spacer(Modifier.height(if (wide) 16.dp else 10.dp))

                if (wide) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                        GamePanel("DEALER", game.dealer, game.inRound && !game.finished, cardWidth, cardHeight, Modifier.weight(1f))
                        Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                            MessageBanner(game.message)
                            Spacer(Modifier.height(12.dp))
                            game.hands.forEachIndexed { index, hand ->
                                PlayerHandPanel(index, hand, index == game.activeHand && game.inRound, cardWidth, cardHeight)
                                if (index < game.hands.lastIndex) Spacer(Modifier.height(8.dp))
                            }
                        }
                    }
                } else {
                    GamePanel("DEALER", game.dealer, game.inRound && !game.finished, cardWidth, cardHeight, Modifier.fillMaxWidth())
                    Spacer(Modifier.height(12.dp))
                    MessageBanner(game.message)
                    Spacer(Modifier.height(10.dp))
                    game.hands.forEachIndexed { index, hand ->
                        PlayerHandPanel(index, hand, index == game.activeHand && game.inRound, cardWidth, cardHeight)
                        if (index < game.hands.lastIndex) Spacer(Modifier.height(8.dp))
                    }
                }

                Spacer(Modifier.weight(1f))
                BettingPanel(game, wide)
            }
        }
    }
}

@Composable
fun Header(bankroll: Int, wide: Boolean) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("BLACKJACK", fontSize = if (wide) 38.sp else 30.sp, fontWeight = FontWeight.Black, color = Gold)
        Text("ROYAL FELT • VIRTUAL CHIPS", fontSize = 11.sp, letterSpacing = 2.sp, color = Color.White.copy(alpha = .65f))
        Spacer(Modifier.height(5.dp))
        Surface(shape = RoundedCornerShape(50), color = Color.Black.copy(alpha = .28f), border = androidx.compose.foundation.BorderStroke(1.dp, Gold.copy(alpha = .45f))) {
            Text("$ $bankroll", modifier = Modifier.padding(horizontal = 18.dp, vertical = 7.dp), color = Gold, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        }
    }
}

@Composable
fun DeckDisplay(remaining: Int, pulse: Int, wide: Boolean) {
    val deckScale by animateFloatAsState(if (pulse > 0) 1.02f else 1f, tween(180), label = "deckPulse")
    Surface(
        modifier = Modifier.fillMaxWidth().scale(deckScale),
        shape = RoundedCornerShape(18.dp),
        color = Color.Black.copy(alpha = .22f),
        border = androidx.compose.foundation.BorderStroke(1.dp, Gold.copy(alpha = .28f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = if (wide) 22.dp else 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Box(modifier = Modifier.width(if (wide) 62.dp else 48.dp).height(if (wide) 82.dp else 64.dp), contentAlignment = Alignment.Center) {
                repeat(3) { i ->
                    Surface(
                        modifier = Modifier.offset(x = (i * 3).dp, y = (i * 2).dp).fillMaxSize(),
                        shape = RoundedCornerShape(8.dp),
                        color = DeckBlue,
                        shadowElevation = 5.dp
                    ) {
                        Box(Modifier.border(2.dp, DeckBlue2, RoundedCornerShape(8.dp)), contentAlignment = Alignment.Center) {
                            Text("♠", color = Gold.copy(alpha = .85f), fontSize = if (wide) 25.sp else 20.sp)
                        }
                    }
                }
            }
            Spacer(Modifier.width(14.dp))
            Column {
                Text("SHOE / DECK", color = Gold, fontWeight = FontWeight.Black, letterSpacing = 1.2.sp, fontSize = 13.sp)
                Text("$remaining cards remaining", color = Color.White, fontWeight = FontWeight.Bold, fontSize = if (wide) 20.sp else 17.sp)
                Text("Cards are drawn from the top", color = Color.White.copy(alpha = .58f), fontSize = 11.sp)
            }
        }
    }
}

@Composable
fun GamePanel(title: String, cards: List<Card>, hideFirst: Boolean, cardWidth: androidx.compose.ui.unit.Dp, cardHeight: androidx.compose.ui.unit.Dp, modifier: Modifier) {
    Surface(modifier = modifier.animateContentSize(), shape = RoundedCornerShape(20.dp), color = Color.Black.copy(alpha = .18f), border = androidx.compose.foundation.BorderStroke(1.dp, Gold.copy(alpha = .22f))) {
        Column(Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(title, color = Color.White, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
            Spacer(Modifier.height(7.dp))
            CardsRow(cards, hideFirst, cardWidth, cardHeight)
            if (cards.isNotEmpty()) Text(if (hideFirst) "?" else "Total ${score(cards)}", color = Gold, fontSize = 13.sp, modifier = Modifier.padding(top = 6.dp))
        }
    }
}

@Composable
fun PlayerHandPanel(index: Int, hand: PlayerHand, active: Boolean, cardWidth: androidx.compose.ui.unit.Dp, cardHeight: androidx.compose.ui.unit.Dp) {
    val pulse by animateFloatAsState(if (active) 1.02f else 1f, tween(400), label = "handPulse")
    Surface(modifier = Modifier.fillMaxWidth().scale(pulse), shape = RoundedCornerShape(18.dp), color = if (active) Gold.copy(alpha = .08f) else Color.Black.copy(alpha = .14f), border = androidx.compose.foundation.BorderStroke(1.dp, if (active) Gold else Color.White.copy(alpha = .12f))) {
        Column(Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("HAND ${index + 1}", color = if (active) Gold else Color.White, fontWeight = FontWeight.Bold)
                Spacer(Modifier.width(10.dp))
                Text("${score(hand.cards)} • ${hand.wager} chips", color = Color.White.copy(alpha = .7f), fontSize = 12.sp)
            }
            Spacer(Modifier.height(6.dp))
            CardsRow(hand.cards, false, cardWidth, cardHeight)
        }
    }
}

@Composable
fun CardsRow(cards: List<Card>, hideFirst: Boolean, cardWidth: androidx.compose.ui.unit.Dp, cardHeight: androidx.compose.ui.unit.Dp) {
    val scroll = rememberScrollState()
    Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(scroll),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        cards.forEachIndexed { i, card ->
            key("${card}-${i}") {
                AnimatedVisibility(
                    visible = true,
                    enter = slideInVertically(initialOffsetY = { -it / 2 }, animationSpec = tween(280)) + fadeIn(tween(220))
                ) {
                    CardView(if (hideFirst && i == 0) "?" else card.toString(), cardWidth, cardHeight)
                }
            }
        }
    }
}

@Composable
fun CardView(text: String, width: androidx.compose.ui.unit.Dp, height: androidx.compose.ui.unit.Dp) {
    val red = text.contains("♥") || text.contains("♦")
    Surface(shape = RoundedCornerShape(12.dp), color = Color.White, shadowElevation = 10.dp, modifier = Modifier.size(width, height)) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.border(2.dp, if (text == "?") GoldDeep else Color.LightGray, RoundedCornerShape(12.dp))) {
            Text(text, fontSize = if (width >= 70.dp) 27.sp else 20.sp, fontWeight = FontWeight.Bold, color = if (red) CardRed else Color(0xFF171717))
        }
    }
}

@Composable
fun MessageBanner(message: String) {
    AnimatedContent(targetState = message, transitionSpec = { fadeIn(tween(180)) togetherWith fadeOut(tween(120)) }, label = "message") { text ->
        Surface(shape = RoundedCornerShape(50), color = Gold.copy(alpha = .12f), border = androidx.compose.foundation.BorderStroke(1.dp, Gold.copy(alpha = .35f))) {
            Text(text, color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.padding(horizontal = 18.dp, vertical = 9.dp))
        }
    }
}

@Composable
fun BettingPanel(game: BlackjackState, wide: Boolean) {
    Surface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp), color = Color.Black.copy(alpha = .28f), border = androidx.compose.foundation.BorderStroke(1.dp, Gold.copy(alpha = .25f))) {
        Column(Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("BET • ${game.bet} CHIPS", color = Gold, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(7.dp), modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                listOf(10, 25, 50, 100).forEach { n ->
                    Button(onClick = { game.addBet(n) }, enabled = !game.inRound && game.bet + n <= game.bankroll, modifier = Modifier.weight(1f)) { Text("+$n", fontSize = if (wide) 14.sp else 12.sp) }
                }
                OutlinedButton(onClick = { game.clearBet() }, enabled = !game.inRound) { Text("CLEAR") }
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(7.dp), modifier = Modifier.fillMaxWidth()) {
                GameButton("DEAL", !game.inRound && game.bet > 0, Modifier.weight(1f)) { game.deal() }
                GameButton("HIT", game.inRound && !game.finished, Modifier.weight(1f)) { game.hit() }
                GameButton("STAND", game.inRound && !game.finished, Modifier.weight(1f)) { game.stand() }
                GameButton("DOUBLE", game.inRound && !game.finished && game.hands.getOrNull(game.activeHand)?.cards?.size == 2 && game.bankroll >= (game.hands.getOrNull(game.activeHand)?.wager ?: Int.MAX_VALUE), Modifier.weight(1f)) { game.doubleDown() }
                GameButton("SPLIT", game.canSplit(), Modifier.weight(1f)) { game.split() }
                if (game.finished) GameButton("NEW", true, Modifier.weight(1f)) { game.newRound() }
            }
        }
    }
}

@Composable
fun GameButton(text: String, enabled: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Button(onClick = onClick, enabled = enabled, modifier = modifier, shape = RoundedCornerShape(13.dp), colors = ButtonDefaults.buttonColors(containerColor = GoldDeep, contentColor = Color.Black)) {
        Text(text, fontWeight = FontWeight.Black, fontSize = 11.sp)
    }
}
