package com.d1inthechamber.blackjack

import android.content.Context
import android.util.AtomicFile
import java.io.*

internal data class SavedHand(val cards: List<Card>, val wager: Int, val finished: Boolean,
    val doubled: Boolean, val fromSplit: Boolean) : Serializable
internal data class SavedGame(
    val version: Int = 1, val shoe: List<Card>, val bankroll: Double, val bet: Int,
    val dealer: List<Card>, val hands: List<SavedHand>, val active: Int, val message: String,
    val inRound: Boolean, val finished: Boolean, val dealing: Boolean, val dealerPlaying: Boolean,
    val dealStep: Int, val drawPulse: Int, val shuffling: Boolean, val roundNumber: Int,
    val lastRound: RoundSummary?
) : Serializable

internal fun BlackjackState.savedGame() = SavedGame(shoe = (deck as Deck).snapshot(), bankroll = bankroll,
    bet = bet, dealer = ArrayList(dealer), hands = hands.map { SavedHand(ArrayList(it.cards), it.wager, it.finished, it.doubled, it.fromSplit) },
    active = activeHand, message = message, inRound = inRound, finished = finished, dealing = dealing,
    dealerPlaying = dealerPlaying, dealStep = dealStep, drawPulse = drawPulse, shuffling = shuffling,
    roundNumber = roundNumber, lastRound = lastRound)
internal fun SavedGame.restore(): BlackjackState {
    require(version == 1 && bankroll >= 0 && bankroll.isFinite())
    return BlackjackState(Deck(shoe)).also { g ->
        g.bankroll = bankroll; g.bet = bet; g.dealer = dealer
        g.hands = hands.map { PlayerHand(it.cards, it.wager, it.finished, it.doubled, it.fromSplit) }
        g.activeHand = active; g.message = message; g.inRound = inRound; g.finished = finished
        g.dealing = dealing; g.dealerPlaying = dealerPlaying; g.dealStep = dealStep
        g.drawPulse = drawPulse; g.shuffling = shuffling; g.roundNumber = roundNumber; g.lastRound = lastRound
    }
}
class GameStore(context: Context) {
    private val file = AtomicFile(File(context.filesDir, "royal-felt-save-v1.bin"))
    fun exists() = file.baseFile.exists()
    fun save(game: BlackjackState) {
        val data = ByteArrayOutputStream().also { buffer ->
            ObjectOutputStream(buffer).use { it.writeObject(game.savedGame()) }
        }.toByteArray()
        val output = file.startWrite()
        try { output.write(data); file.finishWrite(output) }
        catch (error: Exception) { file.failWrite(output); throw error }
    }
    fun load(): BlackjackState? = try {
        file.openRead().use { input -> (ObjectInputStream(input).use { it.readObject() } as SavedGame).restore() }
    } catch (_: Exception) { null }
}
