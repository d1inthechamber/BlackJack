package com.d1inthechamber.blackjack

import org.junit.Assert.*
import org.junit.Test

class CardFlightsTest {
    @Test fun scoreWaitsForLandingAndPendingStartsBeforeEnqueue() {
        val game=BlackjackState()
        val flights=CardFlights(game)
        val first=Card("10","♠");val second=Card("8","♥")
        game.dealer=listOf(first,second)
        assertTrue(flights.pending)
        assertEquals(0,score(flights.visible(game.dealer)))
        flights.landed(first)
        assertEquals(10,score(flights.visible(game.dealer)))
        assertTrue(flights.pending)
        flights.landed(second)
        assertEquals(18,score(flights.visible(game.dealer)))
        assertFalse(flights.pending)
    }

    @Test fun equalCardsFromDifferentDecksEachReceiveAFlight() {
        val flights=CardFlights(BlackjackState())
        val a=Card("5","♠");val b=Card("5","♠")
        var arrivals=0
        flights.enqueue(CardFlight(a,{"5♠"},{null}){arrivals++})
        flights.enqueue(CardFlight(b,{"5♠"},{null}){arrivals++})
        assertEquals(2,flights.queue.size)
        assertEquals(0,arrivals)
        assertTrue(flights.busy)
    }
    @Test fun restoredAndRepositionedSplitCardsDoNotGetDealtAgain() {
        val a=Card("5","♠")
        val game=BlackjackState().apply { hands=listOf(PlayerHand(listOf(a),25)) }
        val flights=CardFlights(game)
        var arrived=false
        flights.enqueue(CardFlight(a,{"5♠"},{null}){arrived=true})
        assertTrue(arrived);assertFalse(flights.busy)
    }
}
