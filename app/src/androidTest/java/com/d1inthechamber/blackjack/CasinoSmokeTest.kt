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
        clearLauncherDialog()
        assertTrue(a.rootInActiveWindow?.findAccessibilityNodeInfosByText("isn't responding")?.isEmpty()!=false)
        Thread.sleep(800)
        val b=a.takeScreenshot();val file=java.io.File(rule.activity.getExternalFilesDir(null),"chaos-$name.png")
        file.outputStream().use{b.compress(android.graphics.Bitmap.CompressFormat.PNG,100,it)};b.recycle()
        a.executeShellCommand("cp ${file.absolutePath} /sdcard/Download/chaos-$name.png").use{java.io.FileInputStream(it.fileDescriptor).use{it.readBytes()}}
    }
    @Test fun lobbySlotsAndSaveArePlayable(){
        reset();rule.onNodeWithText("CASINO CHAOS").assertIsDisplayed();shot("lobby")
        rule.onNodeWithText("SLOTS").performScrollTo().performClick()
        rule.onNodeWithContentDescription("Pull slot machine lever").performScrollTo().performClick()
        rule.mainClock.advanceTimeBy(2400);rule.waitForIdle();rule.onNodeWithTag("slot-reel-0").assertIsDisplayed();shot("slots")
        val m=ViewModelProvider(rule.activity)[BlackjackViewModel::class.java]
        rule.runOnIdle{assertEquals(1,m.casino.slots.spins);assertEquals(990.0+m.casino.slots.returned,m.game.bankroll,0.0)}
        rule.activityRule.scenario.recreate();rule.onNodeWithText("CHAOS SLOTS").assertExists()
        rule.runOnIdle{val saved=CasinoStore(rule.activity).load()!!;assertEquals(1,saved.casino.slots.spins)}
    }
    @Test fun solitaireDrawUndoAndRoomTheme(){
        reset();rule.onNodeWithText("SOLITAIRE").performScrollTo().performClick()
        rule.onNodeWithText("DRAW (24)").performClick();rule.onNodeWithText("DRAW (23)").assertExists()
        rule.onNodeWithText("UNDO").performClick();rule.onNodeWithText("DRAW (24)").assertExists();repeat(7){rule.onNodeWithTag("sol-column-$it").assertIsDisplayed()}
        shot("solitaire")
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
        rule.onNodeWithText("POT 15 • HAND 1").assertExists()
        rule.waitUntil(timeoutMillis=15000){rule.onAllNodes(SemanticsMatcher.expectValue(androidx.compose.ui.semantics.SemanticsProperties.StateDescription,"Ready")).fetchSemanticsNodes().size==3}
        rule.mainClock.advanceTimeByFrame();rule.waitForIdle()
        rule.onNodeWithText("LOBBY").performScrollTo();shot("poker")
        rule.runOnIdle{assertTrue(m.casino.poker.active);assertEquals(0,m.casino.poker.actor)}
        rule.activityRule.scenario.recreate();rule.onNodeWithText("NO-LIMIT TEXAS HOLD’EM • 5 / 10").assertExists()
    }
    @Test fun solitaireDragMovesAnEntireSequence(){
        reset();rule.onNodeWithText("SOLITAIRE").performScrollTo().performClick()
        val m=ViewModelProvider(rule.activity)[BlackjackViewModel::class.java]
        rule.runOnUiThread{m.change{
            val g=m.casino.solitaire
            g.columns.forEach{it.clear()};g.hidden.indices.forEach{g.hidden[it]=0}
            g.columns[0].addAll(listOf(Card("K","♠"),Card("Q","♥"),Card("J","♣")))
        }}
        val source=rule.onNodeWithTag("sol-card-0-0").fetchSemanticsNode().boundsInRoot
        val target=rule.onNodeWithTag("sol-column-1").fetchSemanticsNode().boundsInRoot
        rule.onNodeWithTag("sol-card-0-0").performTouchInput{
            down(androidx.compose.ui.geometry.Offset(center.x,8f))
            advanceEventTime(700)
            moveBy(androidx.compose.ui.geometry.Offset(target.center.x-source.center.x,0f))
            up()
        }
        rule.waitForIdle()
        rule.runOnIdle{
            assertTrue(m.casino.solitaire.columns[0].isEmpty())
            assertEquals(listOf("K","Q","J"),m.casino.solitaire.columns[1].map{it.rank})
        }
    }
    @Test fun settingsStayAccessibleAndVoiceFilesPlay(){
        reset()
        rule.onNodeWithTag("settings-button").assertIsDisplayed().performClick()
        rule.onNodeWithContentDescription("DEALER VOICES").assertExists()
        val m=ViewModelProvider(rule.activity)[BlackjackViewModel::class.java]
        rule.runOnUiThread{m.settings.set("voices",false)}
        rule.activityRule.scenario.recreate()
        rule.onNodeWithContentDescription("DEALER VOICES").assertIsOff()
        rule.onNodeWithText("The Green Room",substring=true).performScrollTo().performClick()
        rule.onNodeWithTag("settings-button").assertIsDisplayed()
        rule.onNodeWithText("BACK").performScrollTo().performClick()
        for(game in listOf("SLOTS","SOLITAIRE","POKER")){
            rule.onNodeWithText(game).performScrollTo().performClick()
            rule.onNodeWithTag("settings-button").assertIsDisplayed().performClick()
            rule.onNodeWithText("BACK").performClick()
            rule.onNodeWithText("LOBBY").performScrollTo().performClick()
        }
        listOf(R.raw.dealer_grunt,R.raw.dealer_groan,R.raw.dealer_laugh).forEach{id->
            val p=android.media.MediaPlayer.create(rule.activity,id);assertNotNull(p);assertTrue(p.duration>200);p.release()
        }
        rule.runOnUiThread{m.settings.set("voices",true);m.selectRoom(RoomStyle.VEGAS)}
    }

}
