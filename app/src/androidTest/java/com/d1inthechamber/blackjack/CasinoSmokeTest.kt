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
        rule.onNodeWithTag("sol-stock").performClick();rule.onNodeWithContentDescription("Draw 1; 23 cards left").assertExists()
        rule.onNodeWithTag("sol-undo").performClick();rule.onNodeWithContentDescription("Draw 1; 24 cards left").assertExists();repeat(7){rule.onNodeWithTag("sol-column-$it").assertIsDisplayed()}
        shot("solitaire")
        rule.activityRule.scenario.recreate();rule.onNodeWithContentDescription("Draw 1; 24 cards left").assertExists()
    }
    @Test fun pokerBuyInCashOutAndBotTurn(){
        reset();rule.onNodeWithText("POKER").performScrollTo().performClick()
        rule.onNodeWithTag("poker-buyin").assertIsDisplayed().performClick()
        val m=ViewModelProvider(rule.activity)[BlackjackViewModel::class.java]
        rule.runOnIdle{assertEquals(500.0,m.game.bankroll,0.0);assertEquals(500,m.casino.poker.seats[0].stack)}
        rule.onNodeWithTag("poker-cashout").assertIsDisplayed().performClick()
        rule.runOnIdle{assertEquals(1000.0,m.game.bankroll,0.0);assertFalse(m.casino.poker.seated)}
        rule.onNodeWithTag("poker-buyin").assertIsDisplayed().performClick();rule.onNodeWithTag("poker-deal").assertIsDisplayed().performClick()
        rule.mainClock.advanceTimeBy(4500);rule.waitForIdle()
        rule.onNodeWithText("POT $15").assertExists()
        rule.waitUntil(timeoutMillis=15000){rule.onAllNodes(SemanticsMatcher.expectValue(androidx.compose.ui.semantics.SemanticsProperties.StateDescription,"Ready")).fetchSemanticsNodes().size==3}
        rule.mainClock.advanceTimeByFrame();rule.waitForIdle()
        (0..2).forEach{rule.onNodeWithTag("poker-portrait-$it").assertIsDisplayed()}
        rule.onNodeWithText("LOBBY").assertIsDisplayed();shot("poker")
        rule.runOnIdle{assertTrue(m.casino.poker.active);assertEquals(0,m.casino.poker.actor)}
        rule.activityRule.scenario.recreate();rule.onNodeWithTag("poker-table").assertExists()
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
            moveBy(androidx.compose.ui.geometry.Offset(12f,0f))
            moveBy(androidx.compose.ui.geometry.Offset(target.center.x-source.center.x-12f,0f))
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
        rule.onNodeWithTag("settings-content").assertIsDisplayed()
        rule.onNodeWithContentDescription("DEALER VOICES").assertExists()
        rule.onNodeWithText("The Green Room",substring=true).assertDoesNotExist()
        shot("settings")
        val m=ViewModelProvider(rule.activity)[BlackjackViewModel::class.java]
        rule.runOnUiThread{m.settings.set("voices",false)}
        rule.activityRule.scenario.recreate()
        rule.onNodeWithContentDescription("DEALER VOICES").assertIsOff()

        rule.onNodeWithTag("nav-rooms").performClick()
        rule.onNodeWithTag("rooms-content").assertIsDisplayed()
        rule.onNodeWithText("The Green Room",substring=true).performScrollTo().performClick()
        shot("rooms")
        rule.onNodeWithTag("nav-games").performClick()
        for(game in listOf("SLOTS","SOLITAIRE","POKER","CRAPS")){
            rule.onNodeWithText(game).performScrollTo().performClick()
            rule.onNodeWithTag("settings-button").assertIsDisplayed().performClick()
            rule.onNodeWithTag("settings-content").assertIsDisplayed()
            rule.onNodeWithText("BACK").performClick()
            rule.onNodeWithTag("nav-games").performClick()
        }
        listOf(R.raw.dealer_grunt,R.raw.dealer_groan,R.raw.dealer_laugh).forEach{id->
            val p=android.media.MediaPlayer.create(rule.activity,id);assertNotNull(p);assertTrue(p.duration>200);p.release()
        }
        rule.runOnUiThread{m.settings.set("voices",true);m.selectRoom(RoomStyle.VEGAS)}
    }

    @Test fun streetCrapsKeepsThePileCenteredAndShowsWinnerCollection(){
        reset();rule.onNodeWithText("CRAPS").performScrollTo().performClick()
        rule.onNodeWithTag("craps-street").assertIsDisplayed()
        rule.onNodeWithTag("craps-hand").assertIsDisplayed()
        val m=ViewModelProvider(rule.activity)[BlackjackViewModel::class.java]
        rule.runOnUiThread{
            m.shootCraps(4,4)
            assertEquals(8,m.craps.point)
            assertEquals(50,m.craps.centerPot)
            assertEquals(975.0,m.game.bankroll,0.0)
        }
        rule.onNodeWithText("POINT 8").assertIsDisplayed()
        rule.onNodeWithTag("craps-money-pile").assertIsDisplayed()
        shot("craps")

        rule.mainClock.autoAdvance=false
        try{
            rule.runOnUiThread{
                m.shootCraps(4,4)
                assertEquals(CrapsWinner.PLAYER,m.craps.winner)
                assertEquals(1025.0,m.game.bankroll,0.0)
                assertEquals(1,m.craps.collectionPulse)
            }
            rule.mainClock.advanceTimeByFrame();rule.waitForIdle()
            rule.onNodeWithTag("craps-collector").assertExists()
            rule.mainClock.advanceTimeBy(1100);rule.waitForIdle()
            shot("craps-win")
            rule.mainClock.advanceTimeBy(320);rule.waitForIdle()
            shot("craps-player-pull")
            rule.mainClock.advanceTimeBy(900);rule.waitForIdle()
            rule.runOnUiThread { m.shootCraps(1,1) }
            rule.mainClock.advanceTimeByFrame()
            rule.mainClock.advanceTimeBy(1420);rule.waitForIdle()
            rule.onNodeWithContentDescription("Opponent hand collecting the money").assertExists()
            shot("craps-opponent-pull")
        }finally{rule.mainClock.autoAdvance=true}
    }

    @Test fun everyRoomUsesItsApprovedSlotArtwork() {
        reset();rule.onNodeWithText("SLOTS").performScrollTo().performClick()
        val m=ViewModelProvider(rule.activity)[BlackjackViewModel::class.java]
        for(room in RoomStyle.entries) {
            rule.runOnUiThread { m.selectRoom(room) }
            rule.onNodeWithTag("slot-reel-1").performScrollTo()
            rule.onAllNodesWithContentDescription(room.title + ": " + slotSymbols(room)[1]).onFirst().assertExists()
            val root=rule.onRoot().fetchSemanticsNode().boundsInRoot
            val spin=rule.onNodeWithTag("slot-spin").assertIsDisplayed().fetchSemanticsNode().boundsInRoot
            assertTrue("Spin must fit below the reels",spin.top>=root.top && spin.bottom<=root.bottom)
            shot("slots-" + room.name.lowercase())
        }
    }

    @Test fun solitaireDrawThreeHintAndNewDealControls() {
        reset(); rule.onNodeWithText("SOLITAIRE").performScrollTo().performClick()
        rule.onNodeWithTag("sol-new").performClick()
        rule.onNodeWithText("DRAW 3").performClick()
        rule.onNodeWithText("DEAL NEW").performClick()
        rule.onNodeWithTag("sol-stock").performClick()
        repeat(3) { rule.onNodeWithTag("sol-waste-$it").assertIsDisplayed() }
        val m=ViewModelProvider(rule.activity)[BlackjackViewModel::class.java]
        rule.runOnIdle { assertEquals(3,m.casino.solitaire.waste.size) }
        rule.onNodeWithTag("sol-undo").performClick()
        rule.runOnIdle { assertEquals(24,m.casino.solitaire.stock.size) }
        rule.onNodeWithTag("sol-hint").performClick()
        rule.onNodeWithTag("sol-new").performClick()
        rule.onNodeWithText("CANCEL").performClick()
        rule.runOnIdle { assertEquals(3,m.casino.solitaire.drawCount) }
        shot("solitaire-draw-three")
    }

    @Test fun crapsUsesEveryOpponentsOwnHand() {
        reset();rule.onNodeWithText("CRAPS").performScrollTo().performClick()
        val m=ViewModelProvider(rule.activity)[BlackjackViewModel::class.java]
        for(room in RoomStyle.entries) {
            rule.runOnUiThread { m.finishCrapsCollection();m.selectRoom(room) }
            rule.waitUntil(15000) {
                rule.onAllNodes(hasTestTag("craps-opponent") and SemanticsMatcher.expectValue(
                    androidx.compose.ui.semantics.SemanticsProperties.StateDescription,"Ready")).fetchSemanticsNodes().size==1
            }
            rule.mainClock.autoAdvance=false
            try {
                rule.runOnUiThread { m.shootCraps(1,1) }
                rule.mainClock.advanceTimeByFrame();rule.mainClock.advanceTimeBy(1050)
                rule.onNodeWithTag("craps-collector").assert(SemanticsMatcher.expectValue(
                    androidx.compose.ui.semantics.SemanticsProperties.StateDescription,"Opponent: ${room.id}"))
                Thread.sleep(500)
                shot("craps-hand-${room.id}")
            } finally { rule.mainClock.autoAdvance=true }
        }
    }

}
