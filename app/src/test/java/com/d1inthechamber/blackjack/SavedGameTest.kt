package com.d1inthechamber.blackjack

import java.io.*
import org.junit.Assert.*
import org.junit.Test

class SavedGameTest {
    private fun game(vararg ranks: String): BlackjackState = BlackjackState(Deck(
        List(100) { Card("2", "♥") } + ranks.reversed().map { Card(it,"♠") }))
    private fun restore(g: BlackjackState): BlackjackState {
        val bytes=ByteArrayOutputStream()
        ObjectOutputStream(bytes).use { it.writeObject(g.savedGame()) }
        return (ObjectInputStream(ByteArrayInputStream(bytes.toByteArray())).use { it.readObject() } as SavedGame).restore()
    }
    private fun deal(g: BlackjackState) { g.addBet(25); g.beginDeal(); repeat(4){g.dealNextCard()} }
    @Test fun interruptedDealKeepsShoeAndDoesNotChargeTwice() {
        val g=game("5","10","5","7","3","4","2")
        g.addBet(25);g.beginDeal();g.dealNextCard()
        val resumed=restore(g)
        resumed.beginDeal()
        repeat(3){resumed.dealNextCard()}
        assertEquals(975.0,resumed.bankroll,0.0)
        assertTrue(resumed.canSplit())
        resumed.split();resumed.hit()
        assertEquals(listOf("5","3","2"),resumed.hands[0].cards.map{it.rank})
    }
    @Test fun splitAndDoubleSurviveSerialization() {
        val g=game("5","10","5","7","3","4","2")
        deal(g);g.split();g.doubleDown()
        val resumed=restore(g)
        assertEquals(925.0,resumed.bankroll,0.0)
        assertEquals(1,resumed.activeHand)
        assertTrue(resumed.hands[0].doubled)
        assertTrue(resumed.hands[0].fromSplit)
        assertEquals(50,resumed.hands[0].wager)
        assertTrue(resumed.canAct())
    }
    @Test fun resumedDealerAndSettledRoundNeverPayTwice() {
        val g=game("10","2","8","3","4","K")
        deal(g);g.stand();g.playDealerStep()
        val resumed=restore(g)
        assertTrue(resumed.dealerPlaying)
        resumed.playDealerStep()
        assertTrue(resumed.finished)
        val settled=restore(resumed)
        val bank=settled.bankroll
        repeat(5){settled.playDealerStep();settled.dealNextCard();settled.stand()}
        assertEquals(bank,settled.bankroll,0.0)
        assertEquals(resumed.lastRound,settled.lastRound)
    }
    @Test fun shuffleIsBetweenHandsAndResumesBeforeChargingBet() {
        val g=BlackjackState(Deck(List(20){Card("2","♠")}))
        g.addBet(25);g.beginDeal()
        assertTrue(g.shuffling)
        assertFalse(g.canAct());assertFalse(g.canDeal())
        assertEquals(1000.0,g.bankroll,0.0)
        g.clearBet();g.addBet(100);g.newRound()
        assertEquals(25,g.bet)
        val resumed=restore(g);resumed.finishShuffle()
        assertFalse(resumed.shuffling);assertTrue(resumed.dealing)
        assertEquals(312,resumed.deckRemaining)
        assertEquals(975.0,resumed.bankroll,0.0)
        resumed.finishShuffle()
        assertEquals(975.0,resumed.bankroll,0.0)
    }
    @Test fun shoeDoesNotSilentlyRefillDuringHand() {
        val shoe=Deck(List(80){Card("2","♠")})
        val g=BlackjackState(shoe);deal(g)
        assertTrue(shoe.needsShuffle());assertFalse(g.shuffling)
        g.hit()
        assertEquals(75,shoe.remaining())
        assertEquals(6,g.hands[0].total())
    }
    @Test fun freshGameHasNoOldWagerCardsOrSummary() {
        val g=BlackjackState()
        assertEquals(1000.0,g.bankroll,0.0);assertEquals(312,g.deckRemaining)
        assertEquals(0,g.bet);assertTrue(g.hands.isEmpty());assertNull(g.lastRound)
    }
}
