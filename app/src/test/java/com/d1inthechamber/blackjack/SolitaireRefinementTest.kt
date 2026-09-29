package com.d1inthechamber.blackjack

import org.junit.Assert.*
import org.junit.Test
import java.util.Random

class SolitaireRefinementTest {
    @Test fun undoStillReachesTheOriginalDealAfterMoreThan150Draws() {
        val g=SolitaireGame(3,Random(31));val original=g.snapshot()
        repeat(200){assertTrue(g.draw())}
        repeat(200){assertTrue(g.undo())}
        assertEquals(original,g.snapshot())
        assertFalse(g.canUndo)
    }
    @Test fun autoFinishBuildsFoundationsLegallyAndConservesAll52Cards() {
        val g=SolitaireGame()
        g.stock.clear();g.waste.clear();g.hidden=MutableList(7){0};g.columns.forEach{it.clear()}
        val suits=listOf("♠","♥","♦","♣")
        for((i,s) in suits.withIndex())g.columns[i].addAll(singlePack().filter{it.suit==s}.reversed())
        // Move only top cards to the matching foundations; no tableau shortcut.
        assertTrue(g.canAutoFinish)
        var count=0
        while(!g.won){val next=g.nextFoundationMove()!!;assertTrue(g.move(next.first,next.second));count++}
        assertEquals(52,count)
        assertEquals(52,g.foundations.flatten().distinct().size)
        assertTrue(g.columns.all{it.isEmpty()})
        assertNull(g.nextFoundationMove())
    }
    @Test fun autoFinishIsUnavailableWhileAnyCardIsHidden() {
        val g=SolitaireGame();assertFalse(g.canAutoFinish)
        g.stock.clear();g.waste.clear();assertFalse(g.canAutoFinish)
    }
}
