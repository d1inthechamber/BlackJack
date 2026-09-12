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

// Small silhouettes stay at the room edges; no touch input or gameplay effects.
@Composable
fun RoomCreatures() {
    var time by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(Unit) {
        while (true) { delay(50); time += .05f }
    }
    Canvas(Modifier.fillMaxSize()) {
        repeat(3) { i ->
            val phase = (time + i * 13f) % 45f
            if (phase < 9f) {
                val x = size.width * (if (i % 2 == 0) .025f else .975f)
                val y = size.height * (.18f + phase / 9f * .5f)
                val c = Color(0xFF271A10)
                val r = 3.dp.toPx()
                drawOval(c, Offset(x-r, y-r*1.6f), androidx.compose.ui.geometry.Size(r*2,r*3.2f))
                repeat(3) { leg ->
                    val wiggle = sin(time * 18 + leg).toFloat() * r
                    drawLine(c, Offset(x,y+(leg-1)*r),Offset(x-r*2.5f,y+(leg-1)*r+wiggle),1.dp.toPx())
                    drawLine(c, Offset(x,y+(leg-1)*r),Offset(x+r*2.5f,y+(leg-1)*r-wiggle),1.dp.toPx())
                }
            }
        }
        val phase = time % 65f
        if (phase > 28f && phase < 40f) {
            val x = size.width * .955f
            val y = size.height * (.72f - (phase-28f)/12f*.45f)
            val r = 4.dp.toPx(); val c = Color(0xFF687043)
            drawLine(c,Offset(x,y+r*2),Offset(x+sin(time*4)*r*2,y+r*7),r*.6f)
            drawOval(c,Offset(x-r,y-r*2),androidx.compose.ui.geometry.Size(r*2,r*4))
            drawCircle(c,r*.85f,Offset(x,y-r*2.5f))
            repeat(2) { j ->
                val w = sin(time*9+j)*r
                drawLine(c,Offset(x,y+(j*3-1)*r),Offset(x-r*2.5f,y+(j*3-1)*r+w),r*.5f)
                drawLine(c,Offset(x,y+(j*3-1)*r),Offset(x+r*2.5f,y+(j*3-1)*r-w),r*.5f)
            }
        }
    }
}
