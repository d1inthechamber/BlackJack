package com.d1inthechamber.blackjack

import androidx.compose.runtime.Composable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.ui.Modifier

data class HandSummary(val cards: List<Card>, val wager: Int, val result: HandResult, val returned: Double) : java.io.Serializable {
    val net get() = returned - wager
}
data class RoundSummary(val dealer: List<Card>, val hands: List<HandSummary>) : java.io.Serializable {
    val returned get() = hands.sumOf { it.returned }
    val net get() = hands.sumOf { it.net }
}
fun summarizeRound(hands: List<PlayerHand>, dealer: List<Card>): RoundSummary = RoundSummary(
    ArrayList(dealer), hands.map { hand ->
        val result = resolveHand(hand.cards, dealer, !hand.fromSplit)
        val returned = hand.wager * when(result) {
            HandResult.BLACKJACK -> 2.5
            HandResult.WIN -> 2.0
            HandResult.PUSH -> 1.0
            HandResult.LOSE -> 0.0
        }
        HandSummary(ArrayList(hand.cards), hand.wager, result, returned)
    }
)
@Composable
fun LastHandDialog(summary: RoundSummary, onClose: () -> Unit) {
    AlertDialog(onDismissRequest = onClose, title = { Text("Last completed hand") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                Text("Dealer: ${summary.dealer.joinToString(" ")} • ${score(summary.dealer)}")
                summary.hands.forEachIndexed { index, hand ->
                    HorizontalDivider()
                    Text("Hand ${index + 1}: ${hand.cards.joinToString(" ")} • ${score(hand.cards)}")
                    Text(when(hand.result) {
                        HandResult.BLACKJACK -> "Blackjack • 3:2"
                        HandResult.WIN -> "Win"
                        HandResult.PUSH -> "Push"
                        HandResult.LOSE -> if (score(hand.cards) > 21) "Bust" else "Loss"
                    })
                    Text("Wager: ${hand.wager} chips")
                    Text("Returned: ${formatChips(hand.returned)} chips (includes wager)")
                    Text("Net: ${formatChips(hand.net)} chips")
                }
                HorizontalDivider()
                Text("Round net: ${formatChips(summary.net)} chips")
            }
        }, confirmButton = { TextButton(onClick = onClose) { Text("Back to table") } })
}
