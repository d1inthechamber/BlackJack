package com.d1inthechamber.blackjack

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import org.junit.Assert.*
import org.junit.Test

class CrapsGameTest {
    @Test fun comeOutNaturalPaysTheMatchedCentralPile() {
        val game=CrapsGame()
        game.setBet(25)
        assertEquals(25,game.begin())
        assertEquals(50,game.centerPot)

        val outcome=game.roll(3,4)

        assertEquals(CrapsWinner.PLAYER,outcome.winner)
        assertEquals(50,outcome.payout)
        assertFalse(game.active)
        assertEquals(1,game.collectionPulse)
        game.finishCollection()
        assertEquals(0,game.centerPot)
        assertEquals(CrapsWinner.NONE,game.winner)
    }

    @Test fun comeOutCrapsLosesWithoutARefund() {
        val game=CrapsGame()
        game.begin()
        val outcome=game.roll(1,1)
        assertEquals(CrapsWinner.OPPONENT,outcome.winner)
        assertEquals(0,outcome.payout)
        assertFalse(game.active)
    }

    @Test fun pointAndPileStayPutUntilPointIsMade() {
        val game=CrapsGame()
        game.begin()
        assertEquals(CrapsWinner.NONE,game.roll(3,5).winner)
        assertEquals(8,game.point)
        assertEquals(50,game.centerPot)

        assertEquals(CrapsWinner.NONE,game.roll(2,3).winner)
        assertEquals(8,game.point)
        assertEquals(50,game.centerPot)

        val outcome=game.roll(4,4)
        assertEquals(CrapsWinner.PLAYER,outcome.winner)
        assertEquals(50,outcome.payout)
    }

    @Test fun sevenOutAwardsThePileToTheOpponent() {
        val game=CrapsGame()
        game.begin()
        game.roll(2,2)
        val outcome=game.roll(3,4)
        assertEquals(CrapsWinner.OPPONENT,outcome.winner)
        assertEquals(0,outcome.payout)
        assertEquals(50,game.centerPot)
    }

    @Test fun activeBetIsLockedAndGameSerializes() {
        val game=CrapsGame()
        assertTrue(game.setBet(100))
        game.begin()
        game.roll(2,3)
        assertFalse(game.setBet(10))

        val bytes=ByteArrayOutputStream().also{buffer->
            ObjectOutputStream(buffer).use{it.writeObject(game)}
        }.toByteArray()
        val restored=ObjectInputStream(ByteArrayInputStream(bytes)).use{it.readObject() as CrapsGame}

        assertEquals(100,restored.bet)
        assertEquals(5,restored.point)
        assertEquals(200,restored.centerPot)
        assertTrue(restored.active)
    }
}
