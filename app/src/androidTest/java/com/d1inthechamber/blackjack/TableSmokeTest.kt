package com.d1inthechamber.blackjack

import androidx.activity.compose.setContent
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import org.junit.Rule
import org.junit.Test

class TableSmokeTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()
    @Test fun launchesWithArtworkAndSurvivesRecreation() {
        rule.onNodeWithText("BLACKJACK").assertIsDisplayed()
        rule.onNodeWithText("SOUND ON").performClick()
        rule.onNodeWithText("SOUND OFF").assertExists()
        rule.activityRule.scenario.recreate()
        rule.onNodeWithText("BLACKJACK").assertIsDisplayed()
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
