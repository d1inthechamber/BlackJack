package com.d1inthechamber.blackjack

import org.junit.Assert.*
import org.junit.Test

class SolitaireProgressTest {
    private fun empty()=SolitaireGame().apply {
        stock.clear();waste.clear();columns.forEach { it.clear() };hidden.fill(0);foundations.forEach { it.clear() }
    }
    private fun reversible()=empty().apply {
        columns[1].add(Card("4","♥"))
        foundations[0].addAll(listOf(Card("A","♣"),Card("2","♣"),Card("3","♣")))
    }
    @Test fun foundationReversalsAloneDoNotKeepADeadDealAlive() {
        val g=reversible();val before=g.snapshot()
        assertEquals(SolitaireProgress.BLOCKED,solitaireProgress(before,1))
        assertEquals(before,g.snapshot())
    }
    @Test fun usefulFoundationReversalCanUnlockAHiddenCard() {
        val g=reversible();g.columns[0].addAll(listOf(Card("5","♠"),Card("2","♥")));g.hidden[0]=1
        assertEquals(SolitaireProgress.AVAILABLE,solitaireProgress(g.snapshot(),1))
    }
    @Test fun searchLimitNeverFalselyDeclaresADeadDeal() {
        assertEquals(SolitaireProgress.UNKNOWN,solitaireProgress(reversible().snapshot(),1,1))
    }
    @Test fun drawThreeDoesNotPretendBuriedStockCardsAreReachable() {
        val g=empty();repeat(7) { g.columns[it].add(Card("${it+2}","♠")) }
        g.stock.addAll(listOf(Card("9","♣"),Card("10","♣"),Card("A","♥")))
        assertEquals(SolitaireProgress.BLOCKED,solitaireProgress(g.snapshot(),3))
        assertEquals(SolitaireProgress.AVAILABLE,solitaireProgress(g.snapshot(),1))
        g.stock.reverse()
        assertEquals(SolitaireProgress.AVAILABLE,solitaireProgress(g.snapshot(),3))
    }
    @Test fun movingAnEntireKingStackBetweenEmptyPilesIsNotProgress() {
        val g=empty();g.columns[0].addAll(listOf(Card("K","♠"),Card("Q","♥")))
        assertEquals(SolitaireProgress.BLOCKED,solitaireProgress(g.snapshot(),1))
    }
}
