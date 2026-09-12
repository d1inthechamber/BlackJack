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
fun TableSounds(game: BlackjackState, enabled: Boolean) {
    val context = LocalContext.current
    val pool = remember { SoundPool.Builder().setMaxStreams(3).setAudioAttributes(
        AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build()).build() }
    var card by remember { mutableIntStateOf(0) }
    var coin by remember { mutableIntStateOf(0) }
    var loaded by remember { mutableIntStateOf(0) }
    DisposableEffect(pool) {
        pool.setOnLoadCompleteListener { _, _, status -> if (status == 0) loaded++ }
        card = pool.load(context, R.raw.card_slide, 1)
        coin = pool.load(context, R.raw.chip_clink, 1)
        onDispose { pool.release() }
    }
    var lastDraw by remember { mutableIntStateOf(game.drawPulse) }
    var lastBet by remember { mutableIntStateOf(game.bet) }
    var lastBank by remember { mutableDoubleStateOf(game.bankroll) }
    LaunchedEffect(game.drawPulse, game.bet, game.bankroll, enabled) {
        if (enabled && loaded == 2) {
            if (game.drawPulse != lastDraw) pool.play(card, .45f, .45f, 1, 0, 1f)
            if (game.bet > lastBet || game.bankroll != lastBank) pool.play(coin, .3f, .3f, 1, 0, 1f)
        }
        lastDraw = game.drawPulse; lastBet = game.bet; lastBank = game.bankroll
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
