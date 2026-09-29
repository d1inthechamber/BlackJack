package com.d1inthechamber.blackjack

import android.media.AudioAttributes
import android.media.SoundPool
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlin.math.sin

@Composable
fun TableSounds(game: BlackjackState, enabled: Boolean, cardPulse:Int=game.drawPulse) {
    val audio=LocalCasinoAudio.current
    var lastDraw by remember { mutableIntStateOf(cardPulse) }
    var lastBet by remember { mutableIntStateOf(game.bet) }
    var lastBank by remember { mutableDoubleStateOf(game.bankroll) }
    var wasFinished by remember { mutableStateOf(game.finished) }
    var wasShuffling by remember { mutableStateOf(game.shuffling) }
    LaunchedEffect(cardPulse, game.bet, game.bankroll, game.finished, game.shuffling, enabled) {
        if (enabled) {
            if (cardPulse != lastDraw) audio?.play(CasinoSound.CARD)
            if (game.bet > lastBet || game.bankroll != lastBank) audio?.play(CasinoSound.CHIPS,.45f)
            if(game.shuffling&&!wasShuffling)audio?.play(CasinoSound.SHUFFLE)
            if(game.finished&&!wasFinished) {
                val net=game.lastRound?.net ?: 0.0
                audio?.play(when {net>0->CasinoSound.WIN;net<0->CasinoSound.LOSE;else->CasinoSound.CLICK},.45f)
            }
        }
        lastDraw = cardPulse; lastBet = game.bet; lastBank = game.bankroll
        wasFinished=game.finished;wasShuffling=game.shuffling
    }
}

// Soft overlapping wisps drift up the room edges behind the cards.
@Composable
fun RoomSmoke() {
    var time by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(Unit) { while (true) { delay(40); time += .04f } }
    Canvas(Modifier.fillMaxSize()) {
        repeat(16) { i ->
            val progress = (time * .025f + i / 16f) % 1f
            val edge = if (i % 2 == 0) .08f else .92f
            val x = size.width * (edge + sin(time * .28f + i * 1.7f) * .10f)
            val y = size.height * (1.15f - progress * 1.4f)
            val radius = size.minDimension * (.10f + progress * .13f)
            val alpha = sin(progress * Math.PI).toFloat().coerceAtLeast(0f) * .12f
            drawCircle(androidx.compose.ui.graphics.Brush.radialGradient(
                listOf(Color(0xFFCDC8B7).copy(alpha=alpha), Color.Transparent),
                center=Offset(x,y), radius=radius),radius,Offset(x,y))
        }
    }
}
