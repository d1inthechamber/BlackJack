package com.d1inthechamber.blackjack

import org.junit.Assert.*
import org.junit.Test

class CardFlightsTest {
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
