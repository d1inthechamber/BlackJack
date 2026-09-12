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
    @Test fun centeredDealerRendersRealGeometry() {
        rule.onNodeWithText("START NEW GAME").performClick()
        if(rule.onAllNodesWithText("START FRESH").fetchSemanticsNodes().isNotEmpty()) rule.onNodeWithText("START FRESH").performClick()
        var surface: DealerSurface?=null
        fun find(view:android.view.View):DealerSurface? {
            if(view is DealerSurface) return view
            if(view is android.view.ViewGroup) for(i in 0 until view.childCount) find(view.getChildAt(i))?.let{return it}
            return null
        }
        rule.runOnUiThread { surface=find(rule.activity.window.decorView) }
        rule.waitUntil(10000) { (surface?.actor?.framesRendered ?: 0)>2 }
        org.junit.Assert.assertEquals(0,surface!!.actor.lastGlError)
        val pos=IntArray(2)
        rule.runOnUiThread { surface!!.getLocationOnScreen(pos) }
        val screen=rule.activity.resources.displayMetrics.widthPixels
        org.junit.Assert.assertTrue(kotlin.math.abs(pos[0]+surface!!.width/2-screen/2)<screen*.08)
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
