package com.d1inthechamber.blackjack

import androidx.test.core.app.ApplicationProvider
import java.io.File
import org.junit.Assert.*
import org.junit.Test

class UpgradeSmokeTest {
    @Test fun v32CasinoArchiveLoadsWithDefaultSidecarGame() {
        val application=ApplicationProvider.getApplicationContext<android.app.Application>()
        val files=application.filesDir
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
