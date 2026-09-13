package com.d1inthechamber.blackjack

import androidx.activity.compose.setContent
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import org.junit.Rule
import org.junit.Test

class TableSmokeTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()
    @Test fun launchesWithArtworkAndSurvivesRecreation() {
        rule.onNodeWithText("ROYAL FELT").assertIsDisplayed()
        rule.onNodeWithText("START NEW GAME").performClick()
        if(rule.onAllNodesWithText("START FRESH").fetchSemanticsNodes().isNotEmpty()) rule.onNodeWithText("START FRESH").performClick()
        rule.onNodeWithText("MENU").assertIsDisplayed()
        rule.onNodeWithText("SOUND ON").performClick()
        rule.onNodeWithText("SOUND OFF").assertExists()
        rule.activityRule.scenario.recreate()
        rule.onNodeWithText("MENU").assertIsDisplayed()
    }
    @Test fun menuContinuePreservesBetAndBuyInResetsBankroll() {
        val model=androidx.lifecycle.ViewModelProvider(rule.activity)[BlackjackViewModel::class.java]
        rule.runOnUiThread { model.startNew(); model.game.addBet(100) }
        rule.onNodeWithText("CONTINUE").performClick()
        rule.onNodeWithText("100 CHIPS").assertExists()
        rule.onNodeWithText("MENU").performClick()
        rule.onNodeWithText("CONTINUE").performClick()
        rule.onNodeWithText("100 CHIPS").assertExists()
        rule.runOnUiThread { model.game.bankroll=0.0; model.game.bet=0; model.save() }
        rule.onNodeWithText("BUY BACK IN • 1,000 FREE CHIPS").performClick()
        rule.onNodeWithText("1000 CHIPS").assertExists()
        rule.onNodeWithText("BUY BACK IN • 1,000 FREE CHIPS").assertDoesNotExist()
    }
    @Test fun diskSaveLoadsIntoANewStateWithTheSameNextCard() {
        val context=rule.activity.applicationContext
        val game=BlackjackState()
        game.addBet(25);game.beginDeal();game.dealNextCard()
        GameStore(context).save(game)
        val restored=GameStore(context).load()!!
        org.junit.Assert.assertEquals(game.savedGame(),restored.savedGame())
        game.dealNextCard();restored.dealNextCard()
        org.junit.Assert.assertEquals(game.savedGame(),restored.savedGame())
    }
    @Test fun centeredCartoonDealerAndShoeRemainVisible() {
        rule.onNodeWithText("START NEW GAME").performClick()
        if(rule.onAllNodesWithText("START FRESH").fetchSemanticsNodes().isNotEmpty()) rule.onNodeWithText("START FRESH").performClick()
        rule.onNodeWithTag("cartoon-dealer").assertIsDisplayed()
        rule.onNodeWithTag("visible-shoe").assertIsDisplayed()
        val bounds=rule.onNodeWithTag("cartoon-dealer").fetchSemanticsNode().boundsInRoot
        val root=rule.onRoot().fetchSemanticsNode().boundsInRoot
        org.junit.Assert.assertTrue(kotlin.math.abs(bounds.center.x-root.center.x)<root.width*.08)
        rule.onNodeWithText("AMBIENCE ON").performClick()
        rule.onNodeWithText("AMBIENCE OFF").assertExists()
        rule.onNodeWithTag("visible-shoe").assertIsDisplayed()
    }
    private fun screenshot(name: String) {
        rule.waitForIdle()
        rule.onNodeWithTag("cartoon-dealer").assertIsDisplayed()
        rule.onNodeWithTag("visible-shoe").assertIsDisplayed()
        // Atlas decoding runs off the main thread.
        Thread.sleep(350)
        rule.mainClock.advanceTimeBy(1000)
        rule.waitForIdle()
        val bitmap=androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot()
        val dir=java.io.File(rule.activity.getExternalFilesDir(null),"screenshots").apply { mkdirs() }
        java.io.File(dir,"$name.png").outputStream().use { bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG,100,it) }
        bitmap.recycle()
        // Gradle uninstalls the app after tests; keep visual QA outside its data directory.
        val source=java.io.File(dir,"$name.png").absolutePath
        androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().uiAutomation
            .executeShellCommand("cp $source /sdcard/Download/royal-felt-$name.png").use { pipe ->
                java.io.FileInputStream(pipe.fileDescriptor).use { it.readBytes() }
            }
    }
    @Test fun hitCardStaysVisibleInPortraitAndLandscape() {
        val g=BlackjackState(Deck(List(100){Card("2","♥")} + listOf("10","2","5","3","4").reversed().map{Card(it,"♠")}))
        g.addBet(25);g.beginDeal();repeat(4){g.dealNextCard()}
        val model=androidx.lifecycle.ViewModelProvider(rule.activity)[BlackjackViewModel::class.java]
        rule.runOnUiThread { model.startNew();model.game=g }
        rule.onNodeWithText("CONTINUE").performClick()
        rule.onNodeWithText("HIT").performClick()
        rule.onAllNodesWithText("4♠")[0].assertIsDisplayed()
        rule.onNodeWithText("SHOE 100").assertExists()
        rule.onNodeWithText("AVAILABLE 975 CHIPS").assertIsDisplayed()
        screenshot("portrait-hit")
        rule.runOnUiThread { rule.activity.requestedOrientation=android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE }
        rule.waitUntil(10000) { rule.activity.resources.configuration.orientation==android.content.res.Configuration.ORIENTATION_LANDSCAPE }
        rule.onAllNodesWithText("4♠")[0].assertIsDisplayed()
        rule.onNodeWithText("STAND").assertIsDisplayed()
        screenshot("landscape-hit")
        rule.runOnUiThread { rule.activity.requestedOrientation=android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT }
    }
    @Test fun initialDealAndSplitFinishBothFlightsBeforePlayResumes() {
        val g=BlackjackState(Deck(List(100){Card("2","♥")} + listOf("5","10","5","7","3","4").reversed().map{Card(it,"♠")}))
        g.addBet(25)
        val model=androidx.lifecycle.ViewModelProvider(rule.activity)[BlackjackViewModel::class.java]
        rule.runOnUiThread { model.startNew();model.game=g }
        rule.onNodeWithText("CONTINUE").performClick()
        rule.onNodeWithText("DEAL").performClick()
        rule.mainClock.advanceTimeBy(8000)
        rule.waitForIdle()
        rule.onNodeWithText("SPLIT").assertIsEnabled().performClick()
        rule.mainClock.advanceTimeBy(4000)
        rule.waitForIdle()
        rule.onNodeWithText("HIT").assertIsEnabled()
        rule.onNodeWithTag("dealing-card").assertDoesNotExist()
        rule.onNodeWithText("AVAILABLE 950 CHIPS").assertIsDisplayed()
        rule.onAllNodesWithText("3♠")[0].assertIsDisplayed()
        rule.runOnIdle {
            org.junit.Assert.assertEquals(listOf("5","3"),g.hands[0].cards.map { it.rank })
            org.junit.Assert.assertEquals(listOf("5","4"),g.hands[1].cards.map { it.rank })
        }
    }
    @Test fun hitTravelsFromDealerTowardPlayerBeforeActionsResume() {
        val g=BlackjackState(Deck(List(100){Card("2","♥")} + listOf("10","2","5","3","4").reversed().map{Card(it,"♠")}))
        g.addBet(25);g.beginDeal();repeat(4){g.dealNextCard()}
        val model=androidx.lifecycle.ViewModelProvider(rule.activity)[BlackjackViewModel::class.java]
        rule.runOnUiThread { model.startNew();model.game=g }
        rule.onNodeWithText("CONTINUE").performClick()
        rule.onNodeWithText("HIT").assertIsEnabled()
        rule.mainClock.autoAdvance=false
        try {
            rule.onNodeWithText("HIT").performClick()
            rule.mainClock.advanceTimeBy(850)
            rule.waitForIdle()
            val early=rule.onNodeWithTag("dealing-card").assertIsDisplayed().fetchSemanticsNode().boundsInRoot.center
            rule.onNodeWithText("HIT").assertIsNotEnabled()
            rule.mainClock.advanceTimeBy(110)
            rule.waitForIdle()
            val later=rule.onNodeWithTag("dealing-card").fetchSemanticsNode().boundsInRoot.center
            org.junit.Assert.assertTrue("Card must move down from the dealer to the player",later.y>early.y)
            rule.mainClock.advanceTimeBy(800)
            rule.waitForIdle()
            rule.onNodeWithTag("dealing-card").assertDoesNotExist()
            rule.onNodeWithText("HIT").assertIsEnabled()
        } finally { rule.mainClock.autoAdvance=true }
    }
    @Test fun roomSelectionPreservesGameAndRendersEveryDealer() {
        val model=androidx.lifecycle.ViewModelProvider(rule.activity)[BlackjackViewModel::class.java]
        rule.runOnUiThread { model.startNew();model.game.addBet(25) }
        val before=model.game.savedGame()
        for(room in RoomStyle.entries.filter { it!=RoomStyle.VEGAS }) {
            rule.onNodeWithText("CHOOSE ROOM").performClick()
            rule.onNodeWithText(room.title,substring=true).performScrollTo().performClick()
            rule.runOnIdle {
                org.junit.Assert.assertEquals(room,model.room)
                org.junit.Assert.assertEquals(room,RoomPreferences(rule.activity).load())
                org.junit.Assert.assertEquals(before,model.game.savedGame())
                listOfNotNull(room.background,room.dealer,room.cardBack).forEach { id ->
                    val bitmap=android.graphics.BitmapFactory.decodeResource(rule.activity.resources,id)
                    org.junit.Assert.assertNotNull("Room asset must decode",bitmap)
                    bitmap?.recycle()
                }
            }
            rule.onNodeWithText("CONTINUE").performClick()
            rule.onNodeWithText("AVAILABLE 1000 CHIPS").assertIsDisplayed()
            screenshot("room-${room.id}")
            rule.onNodeWithText("MENU").performClick()
        }
        rule.activityRule.scenario.recreate()
        val restored=androidx.lifecycle.ViewModelProvider(rule.activity)[BlackjackViewModel::class.java]
        rule.runOnIdle { org.junit.Assert.assertEquals(RoomStyle.PUNK,restored.room) }
        rule.runOnUiThread { restored.selectRoom(RoomStyle.VEGAS) }
    }
    @Test fun musicChoicePersistsAndEveryTrackIsPlayable() {
        val model=androidx.lifecycle.ViewModelProvider(rule.activity)[BlackjackViewModel::class.java]
        rule.runOnUiThread { model.startNew() }
        rule.onNodeWithText("CONTINUE").performClick()
        if(rule.onAllNodesWithText("MUSIC ON").fetchSemanticsNodes().isNotEmpty())rule.onNodeWithText("MUSIC ON").performClick()
        rule.onNodeWithText("MUSIC OFF").assertIsDisplayed()
        rule.activityRule.scenario.recreate()
        rule.onNodeWithText("MUSIC OFF").assertIsDisplayed()
        RoomStyle.entries.forEach { room ->
            val player=android.media.MediaPlayer.create(rule.activity,roomMusicResource(room))
            org.junit.Assert.assertNotNull(player)
            org.junit.Assert.assertTrue(player.duration>20000)
            player.release()
        }
    }
    @Test fun completedHandSummaryShowsExactPayoutAndCloses() {
        val ranks = mutableListOf("A", "9", "K", "8")
        val game = BlackjackState(object : CardShoe {
            override fun draw() = Card(ranks.removeAt(0), "♠")
            override fun remaining() = ranks.size
        })
        game.addBet(25); game.beginDeal(); repeat(4) { game.dealNextCard() }
        rule.runOnUiThread { rule.activity.setContent { BlackjackApp(game) } }
        rule.onNodeWithText("LAST HAND").performClick()
        rule.onNodeWithText("Last completed hand").assertIsDisplayed()
        rule.onNodeWithText("Returned: 62.5 chips (includes wager)").assertExists()
        rule.onNodeWithText("Round net: 37.5 chips").assertExists()
        rule.onNodeWithText("Back to table").performClick()
        rule.onNodeWithText("Last completed hand").assertDoesNotExist()
    }
}
