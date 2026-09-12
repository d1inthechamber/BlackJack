package com.d1inthechamber.blackjack

import org.junit.Assert.assertEquals
import org.junit.Test

class BlackjackRulesTest {
    @Test
    fun hitCardImmediatelyAppearsInObservableHand() {
        val hand = PlayerHand(listOf(Card("7", "♠"), Card("4", "♥")), wager = 25)
        hand.cards += Card("5", "♦")

        assertEquals(3, hand.cards.size)
        assertEquals(16, hand.total())
    }

    @Test
    fun shoeStartsWithSixDecks() {
        assertEquals(312, Deck().remaining())
    }

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

    @Test
    fun tenValueCardsCanBeSplit() {
        assertEquals(true, isPair(listOf(Card("K", "♠"), Card("Q", "♥"))))
    }

    @Test
    fun twoFivesCanBeSplitWhenMatchingWagerIsAvailable() {
        val fives = listOf(Card("5", "♠"), Card("5", "♥"))
        assertEquals(true, canSplitHand(fives, handCount = 1, availableBankroll = 900, wager = 100))
    }

    @Test
    fun dealerStandsOnEvery17() {
        assertEquals(false, dealerMustHit(listOf(Card("A", "♠"), Card("6", "♥"))))
        assertEquals(false, dealerMustHit(listOf(Card("A", "♠"), Card("A", "♦"), Card("5", "♥"))))
        assertEquals(false, dealerMustHit(listOf(Card("10", "♠"), Card("7", "♥"))))
    }
}
