package com.d1inthechamber.blackjack

import org.junit.Assert.*
import org.junit.Test

class CrapsAnimationTest {
    @Test fun billsWaitForContactAndThenRemainAttachedToTheWinner() {
        for (step in 0..60) assertEquals(0f, collectionBillDistance(step / 100f), .0001f)
        assertEquals(0f, collectionHandDistance(.50f), .0001f)
        for (step in 60..100) {
            val p = step / 100f
            assertEquals(collectionHandDistance(p), collectionBillDistance(p), .0001f)
        }
        assertEquals(1f, collectionBillDistance(1f), .0001f)
        assertEquals(-1f, collectionDirection(CrapsWinner.OPPONENT), 0f)
        assertEquals(1f, collectionDirection(CrapsWinner.PLAYER), 0f)
    }

    @Test fun collectionNeverTeleportsOrPullsMoneyBackIntoTheStreet() {
        var previous = 0f
        for (step in 0..1000) {
            val distance = collectionBillDistance(step / 1000f)
            assertTrue(distance >= previous)
            assertTrue(distance - previous < .003f)
            previous = distance
        }
    }
}
