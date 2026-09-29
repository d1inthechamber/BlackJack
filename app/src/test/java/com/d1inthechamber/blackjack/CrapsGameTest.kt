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
    @Test fun opponentNaturalKeepsTheWholePotAndShootsNext() {
        val game=CrapsGame();game.begin();game.roll(1,1);game.finishCollection()
        assertTrue(game.opponentShooter)
        assertEquals(25,game.begin())
        val win=game.roll(5,6)
        assertEquals(CrapsWinner.OPPONENT,win.winner);assertEquals(0,win.payout)
        assertEquals(0,game.roll(5,6).payout)
        game.finishCollection();assertTrue(game.opponentShooter)
    }
    @Test fun opponentSevenOutPaysPlayerOnceAndReturnsTheDice() {
        val game=CrapsGame();game.opponentShooter=true;game.setBet(100);game.begin()
        game.roll(4,4)
        val bytes=ByteArrayOutputStream().also { b -> ObjectOutputStream(b).use { it.writeObject(game) } }.toByteArray()
        val restored=ObjectInputStream(ByteArrayInputStream(bytes)).use { it.readObject() as CrapsGame }
        assertTrue(restored.opponentShooter);assertEquals(8,restored.point)
        val result=restored.roll(3,4)
        assertEquals(CrapsWinner.PLAYER,result.winner);assertEquals(200,result.payout)
        assertEquals(0,restored.roll(3,4).payout)
        restored.finishCollection();assertFalse(restored.opponentShooter)
    }
    @Test fun opponentPointMadeAndComeOutCrapsUseOppositePayouts() {
        val game=CrapsGame();game.opponentShooter=true;game.begin();game.roll(2,2)
        assertEquals(CrapsWinner.OPPONENT,game.roll(1,3).winner)
        game.finishCollection();game.begin()
        val result=game.roll(1,2)
        assertEquals(CrapsWinner.PLAYER,result.winner);assertEquals(50,result.payout)
    }
}
