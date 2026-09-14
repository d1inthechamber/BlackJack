package com.d1inthechamber.blackjack

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.ViewModelProvider
import org.junit.Rule
import org.junit.Test
import org.junit.Assert.*

class CasinoSmokeTest {
    @get:Rule val rule=createAndroidComposeRule<MainActivity>()
    private fun reset(){rule.runOnUiThread{ViewModelProvider(rule.activity)[BlackjackViewModel::class.java].startNew()}}
    private fun shot(name:String){
        rule.waitForIdle()
        val a=androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().uiAutomation
        a.rootInActiveWindow?.let{if(it.findAccessibilityNodeInfosByText("Quickstep isn't responding").isNotEmpty()){it.findAccessibilityNodeInfosByText("Close app").firstOrNull()?.performAction(android.view.accessibility.AccessibilityNodeInfo.ACTION_CLICK);Thread.sleep(500)}}
        assertTrue(a.rootInActiveWindow?.findAccessibilityNodeInfosByText("isn't responding")?.isEmpty()!=false)
        val b=a.takeScreenshot();val file=java.io.File(rule.activity.getExternalFilesDir(null),"chaos-$name.png")
        file.outputStream().use{b.compress(android.graphics.Bitmap.CompressFormat.PNG,100,it)};b.recycle()
        a.executeShellCommand("cp ${file.absolutePath} /sdcard/Download/chaos-$name.png").use{java.io.FileInputStream(it.fileDescriptor).use{it.readBytes()}}
    }
    @Test fun lobbySlotsAndSaveArePlayable(){
        reset();rule.onNodeWithText("CASINO CHAOS").assertIsDisplayed();shot("lobby")
        rule.onNodeWithText("SLOTS").performScrollTo().performClick()
        rule.onNodeWithTag("slot-spin").performScrollTo().performClick()
        rule.mainClock.advanceTimeBy(2400);rule.waitForIdle();rule.onNodeWithTag("slot-reel-0").assertIsDisplayed();shot("slots")
        val m=ViewModelProvider(rule.activity)[BlackjackViewModel::class.java]
        rule.runOnIdle{assertEquals(1,m.casino.slots.spins);assertEquals(990.0+m.casino.slots.returned,m.game.bankroll,0.0)}
        rule.activityRule.scenario.recreate();rule.onNodeWithText("CHAOS SLOTS").assertExists()
        rule.runOnIdle{val saved=CasinoStore(rule.activity).load()!!;assertEquals(1,saved.casino.slots.spins)}
    }
    @Test fun solitaireDrawUndoAndRoomTheme(){
        reset();rule.onNodeWithText("SOLITAIRE").performScrollTo().performClick()
        rule.onNodeWithText("DRAW (24)").performClick();rule.onNodeWithText("DRAW (23)").assertExists()
        rule.onNodeWithText("UNDO").performClick();rule.onNodeWithText("DRAW (24)").assertExists();shot("solitaire")
        rule.activityRule.scenario.recreate();rule.onNodeWithText("DRAW (24)").assertExists()
    }
    @Test fun pokerBuyInCashOutAndBotTurn(){
        reset();rule.onNodeWithText("POKER").performScrollTo().performClick()
        rule.onNodeWithTag("poker-buyin").performScrollTo().performClick()
        val m=ViewModelProvider(rule.activity)[BlackjackViewModel::class.java]
        rule.runOnIdle{assertEquals(500.0,m.game.bankroll,0.0);assertEquals(500,m.casino.poker.seats[0].stack)}
        rule.onNodeWithText("CASH OUT • 500 CHIPS").performScrollTo().performClick()
        rule.runOnIdle{assertEquals(1000.0,m.game.bankroll,0.0);assertFalse(m.casino.poker.seated)}
        rule.onNodeWithTag("poker-buyin").performScrollTo().performClick();rule.onNodeWithTag("poker-deal").performScrollTo().performClick()
        rule.mainClock.advanceTimeBy(4500);rule.waitForIdle()
        rule.onNodeWithText("LOBBY").performScrollTo();shot("poker")
        rule.runOnIdle{assertTrue(m.casino.poker.active);assertEquals(0,m.casino.poker.actor)}
        rule.activityRule.scenario.recreate();rule.onNodeWithText("NO-LIMIT TEXAS HOLD’EM • 5 / 10").assertExists()
    }
}
