package com.d1inthechamber.blackjack

import android.content.pm.ActivityInfo
import android.content.res.Configuration
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.ViewModelProvider
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import java.io.FileInputStream
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class FoldableSmokeTest {
    @get:Rule val rule=createAndroidComposeRule<MainActivity>()
    private val automation get()=InstrumentationRegistry.getInstrumentation().uiAutomation

    private fun shell(command:String){
        automation.executeShellCommand(command).use{FileInputStream(it.fileDescriptor).use{stream->stream.readBytes()}}
    }
    private fun resize(size:String,density:Int){
        shell("wm size $size");shell("wm density $density")
        rule.activityRule.scenario.recreate();rule.waitForIdle()
    }
    private fun visibleInsideRoot(tag:String){
        val root=rule.onRoot().fetchSemanticsNode().boundsInRoot
        val item=rule.onNodeWithTag(tag).assertIsDisplayed().fetchSemanticsNode().boundsInRoot
        assertTrue("$tag must remain horizontally visible",item.left>=root.left&&item.right<=root.right)
        assertTrue("$tag must remain vertically visible",item.top>=root.top&&item.bottom<=root.bottom)
    }
    private fun capture(name:String){
        if(rule.onAllNodesWithTag("craps-opponent").fetchSemanticsNodes().isNotEmpty()) rule.waitUntil(15000){
            rule.onAllNodes(hasTestTag("craps-opponent") and SemanticsMatcher.expectValue(
                androidx.compose.ui.semantics.SemanticsProperties.StateDescription,"Ready"))
                .fetchSemanticsNodes().size==1
        }
        rule.waitForIdle();clearLauncherDialog()
        // Compose can be idle before the emulator compositor presents its frame.
        Thread.sleep(800)
        val bitmap=automation.takeScreenshot()
        val file=File(rule.activity.getExternalFilesDir(null),"fold-$name.png")
        file.outputStream().use{bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG,100,it)};bitmap.recycle()
        shell("cp ${file.absolutePath} /sdcard/Download/fold-$name.png")
    }

    private fun checkCardTables(mode:String) {
        rule.onNodeWithTag("nav-games").performClick()
        rule.onNodeWithText("SOLITAIRE").performScrollTo().performClick()
        visibleInsideRoot("sol-stock");visibleInsideRoot("sol-new");visibleInsideRoot("sol-hint")
        repeat(7) { visibleInsideRoot("sol-column-$it") }
        capture("solitaire-$mode")
        rule.onNodeWithTag("nav-games").performClick()
        rule.onNodeWithText("POKER").performScrollTo().performClick()
        val m=ViewModelProvider(rule.activity)[BlackjackViewModel::class.java]
        rule.runOnUiThread { if(!m.casino.poker.seated)m.change { m.casino.poker.sit(500,m.room.ordinal);m.game.bankroll-=500 } }
        rule.waitUntil(15000) { rule.onAllNodes(SemanticsMatcher.expectValue(
            androidx.compose.ui.semantics.SemanticsProperties.StateDescription,"Ready")).fetchSemanticsNodes().size==3 }
        visibleInsideRoot("poker-table")
        if (!m.casino.poker.active) visibleInsideRoot("poker-deal")
        rule.runOnUiThread { m.change { m.casino.poker=PokerGame().apply { sit(500,m.room.ordinal);startHand() } } }
        rule.waitForIdle()
        visibleInsideRoot("poker-call")
        visibleInsideRoot("poker-board-cards");visibleInsideRoot("poker-human-cards")
        val board=rule.onNodeWithTag("poker-board-cards").fetchSemanticsNode().boundsInRoot
        val hand=rule.onNodeWithTag("poker-human-cards").fetchSemanticsNode().boundsInRoot
        assertTrue("Community cards and player hand must not overlap in $mode",board.bottom<=hand.top)
        capture("poker-$mode")
        rule.onNodeWithTag("nav-games").performClick()
        rule.onNodeWithText("CRAPS").performScrollTo().performClick()
    }

    @Test fun unfoldedFoldedAndLandscapeLayoutsRemainUsable(){
        try{
            rule.runOnUiThread{ViewModelProvider(rule.activity)[BlackjackViewModel::class.java].startNew()}
            resize("1840x2208",420)
            visibleInsideRoot("nav-games");visibleInsideRoot("nav-rooms");visibleInsideRoot("settings-button")
            rule.onNodeWithText("CRAPS").performScrollTo().performClick()
            visibleInsideRoot("craps-street");rule.onNodeWithTag("craps-roll").performScrollTo();visibleInsideRoot("craps-roll");capture("unfolded");checkCardTables("unfolded")

            resize("1080x2092",420)
            visibleInsideRoot("nav-games");visibleInsideRoot("craps-street");rule.onNodeWithTag("craps-roll").performScrollTo();visibleInsideRoot("craps-roll");capture("folded");checkCardTables("folded")

            rule.runOnUiThread{rule.activity.requestedOrientation=ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE}
            rule.waitUntil(10000){rule.activity.resources.configuration.orientation==Configuration.ORIENTATION_LANDSCAPE}
            visibleInsideRoot("nav-games");rule.onNodeWithTag("craps-roll").performScrollTo();visibleInsideRoot("craps-roll");capture("landscape");checkCardTables("landscape")
        }finally{
            rule.runOnUiThread{rule.activity.requestedOrientation=ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED}
            shell("wm size reset");shell("wm density reset")
        }
    }
}
