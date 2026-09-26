package com.d1inthechamber.blackjack

import androidx.test.core.app.ApplicationProvider
import java.io.File
import org.junit.Assert.*
import org.junit.Test

class UpgradeSmokeTest {
    @Test fun walletAndActivePotRestoreFromTheSameAtomicSnapshot() {
        val application=ApplicationProvider.getApplicationContext<android.app.Application>()
        val model=BlackjackViewModel(application)
        model.startNew()
        assertTrue(model.beginCrapsHand())
        val beforeRoll=BlackjackViewModel(application)
        assertEquals(975.0,beforeRoll.game.bankroll,0.0)
        assertEquals(50,beforeRoll.craps.centerPot)
        assertTrue(beforeRoll.craps.active)
        assertTrue(beforeRoll.beginCrapsHand())
        assertEquals(975.0,beforeRoll.game.bankroll,0.0)
        beforeRoll.shootCraps(3,4)
        val paid=BlackjackViewModel(application)
        assertEquals(1025.0,paid.game.bankroll,0.0)
        assertEquals(CrapsWinner.PLAYER,paid.craps.winner)
        paid.shootCraps(3,4)
        assertEquals(1025.0,paid.game.bankroll,0.0)
        paid.finishCrapsCollection()
        val collected=BlackjackViewModel(application)
        assertEquals(0,collected.craps.centerPot)
        assertEquals(1025.0,collected.game.bankroll,0.0)
    }

    @Test fun suppliedAtlasesDecodeWithUsableTransparency() {
        val context=ApplicationProvider.getApplicationContext<android.app.Application>()
        for(id in listOf(R.drawable.craps_hands,R.drawable.craps_banknote,R.drawable.slot_symbols)) {
            val bitmap=android.graphics.BitmapFactory.decodeResource(context.resources,id)
            assertNotNull(bitmap)
            assertTrue(bitmap.hasAlpha())
            assertTrue(android.graphics.Color.alpha(bitmap.getPixel(0,0))<8)
            val alphaSamples=(0 until bitmap.height step 8).sumOf { y ->
                (0 until bitmap.width step 8).count { x -> android.graphics.Color.alpha(bitmap.getPixel(x,y))>200 }
            }
            assertTrue("Artwork must contain real visible pixels",alphaSamples>1000)
            bitmap.recycle()
        }
    }

    @Test fun v32CasinoArchiveLoadsWithDefaultSidecarGame() {
        val application=ApplicationProvider.getApplicationContext<android.app.Application>()
        val files=application.filesDir
        listOf("casino-chaos-session-v2.bin","casino-chaos-session-v2.bin.bak","casino-chaos-session-v2.bin.new").forEach {
            File(files,it).delete()
        }
        listOf("casino-chaos-craps-v1.bin","casino-chaos-craps-v1.bin.bak","casino-chaos-craps-v1.bin.new").forEach{
            File(files,it).delete()
        }
        val blackjack=BlackjackState().also{it.bankroll=713.5;it.addBet(25)}
        val casino=CasinoState().also{it.lastGame="POKER";it.slots.spins=9}
        CasinoStore(application).save(blackjack,casino)

        val upgraded=BlackjackViewModel(application)

        assertEquals(713.5,upgraded.game.bankroll,0.0)
        assertEquals(25,upgraded.game.bet)
        assertEquals("POKER",upgraded.casino.lastGame)
        assertEquals(9,upgraded.casino.slots.spins)
        assertEquals(25,upgraded.craps.bet)
        assertEquals(0,upgraded.craps.rolls)
        assertFalse(upgraded.craps.active)
    }
}
