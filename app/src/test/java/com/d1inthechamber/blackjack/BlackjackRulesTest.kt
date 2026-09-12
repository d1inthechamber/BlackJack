package com.d1inthechamber.blackjack

import org.junit.Assert.assertEquals
import org.junit.Test

class BlackjackRulesTest {
    @Test
    fun player18BeatsDealer23() {
        val player = listOf(Card("10", "♠"), Card("8", "♥"))
        val dealer = listOf(Card("10", "♦"), Card("7", "♣"), Card("6", "♠"))

        assertEquals(18, score(player))
        assertEquals(23, score(dealer))
        assertEquals(HandResult.WIN, resolveHand(player, dealer))
    }

    @Test
    fun playerBustLosesEvenWhenDealerAlsoBusts() {
        val player = listOf(Card("10", "♠"), Card("8", "♥"), Card("5", "♦"))
        val dealer = listOf(Card("10", "♦"), Card("7", "♣"), Card("6", "♠"))

        assertEquals(HandResult.LOSE, resolveHand(player, dealer))
    }

    @Test
    fun equalTotalsPush() {
        val player = listOf(Card("10", "♠"), Card("8", "♥"))
        val dealer = listOf(Card("9", "♦"), Card("9", "♣"))

        assertEquals(HandResult.PUSH, resolveHand(player, dealer))
    }
}
