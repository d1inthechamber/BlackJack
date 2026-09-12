package com.d1inthechamber.blackjack
import org.junit.Assert.*
import org.junit.Test

class RoundSummaryTest {
    private fun cards(vararg ranks: String) = ranks.map { Card(it, "♠") }
    @Test fun mixedSplitResultsAndDoubleWagersBalance() {
        val summary = summarizeRound(listOf(
            PlayerHand(cards("K", "Q"), 50, fromSplit = true),
            PlayerHand(cards("10", "8"), 25, fromSplit = true),
            PlayerHand(cards("10", "6", "K"), 25, fromSplit = true)
        ), cards("10", "8"))
        assertEquals(listOf(HandResult.WIN, HandResult.PUSH, HandResult.LOSE), summary.hands.map { it.result })
        assertEquals(125.0, summary.returned, 0.0)
        assertEquals(25.0, summary.net, 0.0)
    }
    @Test fun naturalPaysHalfChipsAndSnapshotDoesNotChange() {
        val hand = PlayerHand(cards("A", "K"),25)
        val dealer = cards("10", "7").toMutableList()
        val summary = summarizeRound(listOf(hand),dealer)
        hand.cards.clear(); dealer.clear()
        assertEquals(62.5, summary.returned, 0.0)
        assertEquals(37.5, summary.net, 0.0)
        assertEquals(2, summary.hands[0].cards.size)
        assertEquals(2, summary.dealer.size)
    }
    @Test fun summarySurvivesNewRoundAndMatchesBankrollChange() {
        val queue = cards("10", "10", "8", "7").toMutableList()
        val game = BlackjackState(object: CardShoe {
            override fun draw() = queue.removeAt(0)
            override fun remaining() = queue.size
        })
        game.addBet(25); game.beginDeal(); repeat(4) { game.dealNextCard() }
        assertNull(game.lastRound)
        game.stand(); game.playDealerStep()
        val summary = game.lastRound!!
        assertEquals(game.bankroll - 1000.0, summary.net, 0.0)
        game.newRound()
        assertSame(summary,game.lastRound)
    }
}
