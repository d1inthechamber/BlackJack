package com.d1inthechamber.blackjack

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.random.Random

data class Card(val rank: String, val suit: String) {
    val value: Int get() = when (rank) { "A" -> 11; "K", "Q", "J" -> 10; else -> rank.toInt() }
    override fun toString() = "$rank$suit"
}

class Deck {
    private val cards = mutableListOf<Card>()
    init { reset() }
    fun reset() { cards.clear(); listOf("♠", "♥", "♦", "♣").forEach { s -> (2..10).forEach { r -> cards += Card(r.toString(), s) }; listOf("J","Q","K","A").forEach { r -> cards += Card(r, s) } }; cards.shuffle() }
    fun draw(): Card { if (cards.size < 15) reset(); return cards.removeAt(cards.lastIndex) }
}

fun score(hand: List<Card>): Int { var total = hand.sumOf { it.value }; var aces = hand.count { it.rank == "A" }; while (total > 21 && aces-- > 0) total -= 10; return total }
fun blackjack(hand: List<Card>) = hand.size == 2 && score(hand) == 21

class BlackjackState {
    var bankroll by mutableIntStateOf(1000); var bet by mutableIntStateOf(0)
    var player by mutableStateOf(listOf<Card>()); var dealer by mutableStateOf(listOf<Card>())
    var message by mutableStateOf("Place your bet"); var inRound by mutableStateOf(false); var finished by mutableStateOf(false)
    private var deck = Deck()
    fun addBet(amount: Int) { if (!inRound && bet + amount <= bankroll) bet += amount }
    fun clearBet() { if (!inRound) bet = 0 }
    fun deal() { if (bet <= 0 || inRound) return; bankroll -= bet; player = listOf(deck.draw(), deck.draw()); dealer = listOf(deck.draw(), deck.draw()); inRound = true; finished = false; message = "Your move"; if (blackjack(player)) finish() }
    fun hit() { if (inRound && !finished) { player = player + deck.draw(); if (score(player) >= 21) finish() } }
    fun stand() { if (inRound && !finished) { while (score(dealer) < 17) dealer = dealer + deck.draw(); finish() } }
    fun doubleDown() { if (inRound && !finished && player.size == 2 && bankroll >= bet) { bankroll -= bet; bet *= 2; player = player + deck.draw(); if (score(player) < 21) while (score(dealer) < 17) dealer = dealer + deck.draw(); finish() } }
    private fun finish() { finished = true; inRound = false; val p = score(player); val d = score(dealer); when { blackjack(player) && !blackjack(dealer) -> { bankroll += (bet * 2.5).toInt(); message = "BLACKJACK! You win 3:2" }; p > 21 -> message = "Bust — dealer wins"; d > 21 || p > d -> { bankroll += bet * 2; message = "You win!" }; p == d -> { bankroll += bet; message = "Push — bet returned" }; else -> message = "Dealer wins" } }
    fun newRound() { bet = 0; player = emptyList(); dealer = emptyList(); message = "Place your bet"; finished = false }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) { super.onCreate(savedInstanceState); setContent { BlackjackApp() } }
}

@Composable fun BlackjackApp() {
    val game = remember { BlackjackState() }
    MaterialTheme { Surface(Modifier.fillMaxSize(), color = Color(0xFF10251B)) { Column(Modifier.fillMaxSize().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("BLACKJACK", fontSize = 30.sp, fontWeight = FontWeight.Bold, color = Color.White)
        Text("Virtual Chips • $${game.bankroll}", color = Color(0xFFFFD54F), fontSize = 18.sp)
        Spacer(Modifier.height(16.dp))
        Hand("DEALER", game.dealer, hideFirst = game.inRound && !game.finished)
        Spacer(Modifier.height(18.dp)); Text(game.message, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(12.dp)); Hand("YOU  ${if (game.player.isNotEmpty()) score(game.player) else ""}", game.player)
        Spacer(Modifier.weight(1f))
        Text("Bet: ${game.bet} chips", color = Color.White, fontSize = 18.sp)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { listOf(10,25,50,100).forEach { n -> Button(onClick={game.addBet(n)}, enabled=!game.inRound && game.bet+n<=game.bankroll) { Text("+$n") } }; OutlinedButton(onClick={game.clearBet}, enabled=!game.inRound) { Text("Clear") } }
        Spacer(Modifier.height(8.dp)); Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick={game.deal}, enabled=!game.inRound && game.bet>0) { Text("DEAL") }
            Button(onClick={game.hit}, enabled=game.inRound && !game.finished) { Text("HIT") }
            Button(onClick={game.stand}, enabled=game.inRound && !game.finished) { Text("STAND") }
            Button(onClick={game.doubleDown}, enabled=game.inRound && !game.finished && game.player.size==2 && game.bankroll>=game.bet) { Text("DOUBLE") }
        }
        Spacer(Modifier.height(8.dp)); if (game.finished) Button(onClick={game.newRound}) { Text("NEW ROUND") }
    } } }
}

@Composable fun Hand(title: String, cards: List<Card>, hideFirst: Boolean = false) { Column(horizontalAlignment = Alignment.CenterHorizontally) { Text(title, color=Color.White, fontWeight=FontWeight.Bold); Spacer(Modifier.height(6.dp)); Row(horizontalArrangement=Arrangement.spacedBy(6.dp)) { cards.forEachIndexed { i,c -> CardView(if (hideFirst && i==0) "?" else c.toString()) } } } }
@Composable fun CardView(text: String) { Surface(shape=RoundedCornerShape(10.dp), color=Color.White, modifier=Modifier.size(58.dp,82.dp)) { Box(contentAlignment=Alignment.Center) { Text(text, fontSize=20.sp, fontWeight=FontWeight.Bold, color=if (text.contains("♥")||text.contains("♦")) Color.Red else Color.Black) } } }
