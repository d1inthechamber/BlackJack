package com.d1inthechamber.blackjack
import org.junit.Assert.*
import org.junit.Test
class DealerAnimationTest {
    @Test fun reactsFromPlayersNetNotAmbiguousMessageText() {
        assertEquals(DealerMood.ANGRY,dealerMood(25.0))
        assertEquals(DealerMood.LAUGH,dealerMood(-25.0))
        assertEquals(DealerMood.SMUG,dealerMood(0.0))
        assertEquals(DealerMood.IDLE,dealerMood(null))
    }
    @Test fun handTouchesShoeBeforePushingToCorrectSideThenReturns() {
        assertEquals(1,dealingPose(.1f,false));assertEquals(2,dealingPose(.25f,false))
        assertEquals(3,dealingPose(.45f,false));assertEquals(4,dealingPose(.6f,false))
        assertEquals(5,dealingPose(.6f,true));assertEquals(0,dealingPose(.9f,true))
        val flights=CardFlights(BlackjackState())
        flights.spriteLocal=androidx.compose.ui.geometry.Rect(0f,0f,100f,100f)
        assertTrue(flights.localHand(.32f).x>flights.localHand(.68f).x)
        flights.pushRight=true
        assertTrue(flights.localHand(.32f).x<flights.localHand(.68f).x)
    }
}
