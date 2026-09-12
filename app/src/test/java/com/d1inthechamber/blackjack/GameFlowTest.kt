package com.d1inthechamber.blackjack

import org.junit.Assert.*
import org.junit.Test

class GameFlowTest {
    private fun game(vararg ranks: String): BlackjackState {
        val queue = ranks.map { Card(it, "♠") }.toMutableList()
        return BlackjackState(object : CardShoe {
            override fun draw() = queue.removeAt(0)
            override fun remaining() = queue.size
        })
    }
    private fun deal(g: BlackjackState, bet: Int = 25) {
        g.addBet(bet)
        g.beginDeal()
        repeat(4) { g.dealNextCard() }
    }
    @Test fun blackjackPaysHalfChipExactly() {
        val g = game("A", "9", "K", "8")
        deal(g)
        assertTrue(g.finished)
        assertEquals(1037.5, g.bankroll, 0.0)
    }
    @Test fun repeatDealCannotOverdraw() {
        val g = game("10", "10", "8", "9")
        deal(g, 1000)
        g.stand()
        g.playDealerStep()
        assertEquals(0.0, g.bankroll, 0.0)
        assertFalse(g.canDeal())
        g.beginDeal()
        assertEquals(0.0, g.bankroll, 0.0)
        assertTrue(g.finished)
    }
    @Test fun dealerDrawsOneCardPerStepAndBlocksPlayerActions() {
        val g = game("10", "2", "8", "3", "4", "K")
        deal(g)
        g.stand()
        assertTrue(g.dealerPlaying)
        assertFalse(g.canAct())
        g.hit()
        assertEquals(2, g.hands[0].cards.size)
        g.playDealerStep()
        assertEquals(3, g.dealer.size)
        assertFalse(g.finished)
        g.playDealerStep()
        assertEquals(4, g.dealer.size)
        assertTrue(g.finished)
    }
    @Test fun actualSplitFivesAndHitUpdateHand() {
        val g = game("5", "10", "5", "7", "3", "4", "2")
        deal(g)
        assertTrue(g.canSplit())
        g.split()
        assertEquals(2, g.hands.size)
        assertEquals(950.0, g.bankroll, 0.0)
        g.hit()
        assertEquals(3, g.hands[0].cards.size)
        assertEquals(10, g.hands[0].total())
    }
    @Test fun splitTwentyOneFinishesAutomatically() {
        val g = game("K", "10", "Q", "7", "A", "5")
        deal(g)
        g.split()
        assertTrue(g.hands[0].finished)
        assertEquals(1, g.activeHand)
        g.stand()
        g.playDealerStep()
        assertEquals(1000.0, g.bankroll, 0.0)
    }
}
