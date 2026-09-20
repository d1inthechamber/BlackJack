package com.d1inthechamber.blackjack

import android.graphics.Paint
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

@Composable
internal fun CrapsScreen(model: BlackjackViewModel, onBack: () -> Unit) {
    val game = model.craps
    var rolling by remember { mutableStateOf(false) }
    val shake by animateFloatAsState(if (rolling) 8f else 0f, tween(90), label = "dice-hand-shake")
    val collector = remember { Animatable(0f) }
    HumanReactionVoice(game.collectionPulse,when(game.winner){CrapsWinner.PLAYER->HumanReaction.CHEER;CrapsWinner.OPPONENT->HumanReaction.FRUSTRATED;else->null},model.settings.voices)

    LaunchedEffect(game.collectionPulse) {
        if (game.collectionPulse > 0 && game.winner != CrapsWinner.NONE) {
            collector.snapTo(0f)
            collector.animateTo(1f, tween(520))
            delay(420)
            collector.animateTo(0f, tween(360))
            model.finishCrapsCollection()
        }
    }

    fun shoot() {
        if (rolling || game.winner != CrapsWinner.NONE) return
        if (!game.active && model.game.bankroll < game.bet) return
        rolling = true
    }

    LaunchedEffect(rolling) {
        if (rolling) {
            delay(560)
            model.shootCraps()
            rolling = false
        }
    }

    CasinoFrame(model, "STREET CRAPS", onBack) {
        Text("ALLEY RULES • PASS-LINE SHOOTING DICE", color = model.room.accent, fontSize = 12.sp)
        Text("You against ${model.room.host}", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Black)
        Text(game.message, color = Color(0xFFF7F1E6), fontSize = 15.sp, textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().testTag("craps-message"))

        Surface(
            color = Color(0xE8141618),
            shape = RoundedCornerShape(26.dp),
            border = androidx.compose.foundation.BorderStroke(2.dp, model.room.accent),
            modifier = Modifier.fillMaxWidth().heightIn(min = 440.dp).testTag("craps-street")
        ) {
            Box(Modifier.fillMaxWidth().background(Color(0xFF161A1C))) {
                StreetChalk(Modifier.fillMaxSize())

                Column(
                    Modifier.align(Alignment.TopCenter).padding(top = 14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(model.room.host.uppercase(), color = model.room.accent, fontWeight = FontWeight.Black, fontSize = 13.sp)
                    Text(if (game.point == 0) "COME OUT" else "POINT ${game.point}", color = Color.White, fontSize = 21.sp, fontWeight = FontWeight.Black)
                }

                Row(
                    Modifier.align(Alignment.Center).offset(y = (-48).dp),
                    horizontalArrangement = Arrangement.spacedBy(18.dp)
                ) {
                    DieFace(game.dieOne, rolling, Modifier.testTag("craps-die-1"))
                    DieFace(game.dieTwo, rolling, Modifier.testTag("craps-die-2"))
                }

                MoneyPile(
                    amount = game.centerPot,
                    collection = collector.value,
                    winner = game.winner,
                    modifier = Modifier.align(Alignment.Center).offset(y = 54.dp).size(220.dp, 105.dp).testTag("craps-money-pile")
                )

                if (game.winner != CrapsWinner.NONE) {
                    CollectingHand(
                        winner = game.winner,
                        progress = collector.value,
                        modifier = Modifier.fillMaxSize().testTag("craps-collector")
                    )
                }

                TattooedDiceHand(
                    rolling = rolling,
                    modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 8.dp)
                        .size(188.dp, 142.dp).rotate(if (rolling) shake else 0f)
                        .semantics { contentDescription = "Tattooed hand holding dice; tap to roll" }
                        .testTag("craps-hand")
                        .clickable(enabled = game.winner == CrapsWinner.NONE && (game.active || model.game.bankroll >= game.bet)) { shoot() }
                )
            }
        }

        Text(
            if (game.active) "The bills stay in the centre until the point is made or a 7 is rolled."
            else "${formatChips(model.game.bankroll)} virtual dollars available",
            color = Color(0xFFE1DCE4), fontSize = 12.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            listOf(10, 25, 50, 100).forEach { amount ->
                OutlinedButton(
                    onClick = { model.setCrapsBet(amount) },
                    enabled = !game.active && game.winner == CrapsWinner.NONE && model.game.bankroll >= amount,
                    modifier = Modifier.weight(1f), contentPadding = PaddingValues(horizontal = 2.dp)
                ) { Text(if (game.bet == amount) "● \$$amount" else "\$$amount", fontSize = 11.sp) }
            }
        }
        Button(
            onClick = { shoot() },
            enabled = !rolling && game.winner == CrapsWinner.NONE && (game.active || model.game.bankroll >= game.bet),
            modifier = Modifier.fillMaxWidth().height(58.dp).testTag("craps-roll")
        ) { Text(if (rolling) "SHAKING…" else if (game.active) "THROW AGAIN" else "PUT \$${game.bet} IN • SHOOT", fontWeight = FontWeight.Black) }
        Text("Come-out: 7 or 11 wins; 2, 3 or 12 loses. Any other total becomes the point. Make the point before a 7 to win the matched centre pile.", color = Color(0xFFE1DCE4), fontSize = 12.sp)
    }
}

@Composable
private fun StreetChalk(modifier: Modifier) {
    Canvas(modifier) {
        drawRect(Color(0xFF15191B))
        repeat(9) { i ->
            val y = size.height * (.12f + i * .11f)
            drawLine(Color.White.copy(alpha = .035f), Offset(0f, y), Offset(size.width, y - 28f), 4f)
        }
        drawCircle(Color.White.copy(alpha = .16f), size.minDimension * .23f, Offset(size.width / 2f, size.height * .43f), style = Stroke(3f))
        drawLine(Color(0xFFE5D9BD).copy(alpha = .22f), Offset(size.width * .14f, size.height * .68f), Offset(size.width * .86f, size.height * .68f), 5f)
    }
}

@Composable
private fun DieFace(value: Int, rolling: Boolean, modifier: Modifier = Modifier) {
    val angle by animateFloatAsState(if (rolling) 315f else 0f, tween(520), label = "die-spin-$value")
    Canvas(modifier.size(76.dp).rotate(angle).semantics { contentDescription = "Die showing $value" }) {
        drawRoundRect(Color(0xFFF8F1DF), cornerRadius = androidx.compose.ui.geometry.CornerRadius(14f, 14f))
        drawRoundRect(Color(0xFFD5C8AE), style = Stroke(4f), cornerRadius = androidx.compose.ui.geometry.CornerRadius(14f, 14f))
        val left = size.width * .28f
        val right = size.width * .72f
        val top = size.height * .28f
        val bottom = size.height * .72f
        val center = Offset(size.width / 2f, size.height / 2f)
        val spots = when (value) {
            1 -> listOf(center)
            2 -> listOf(Offset(left, top), Offset(right, bottom))
            3 -> listOf(Offset(left, top), center, Offset(right, bottom))
            4 -> listOf(Offset(left, top), Offset(right, top), Offset(left, bottom), Offset(right, bottom))
            5 -> listOf(Offset(left, top), Offset(right, top), center, Offset(left, bottom), Offset(right, bottom))
            else -> listOf(Offset(left, top), Offset(right, top), Offset(left, center.y), Offset(right, center.y), Offset(left, bottom), Offset(right, bottom))
        }
        spots.forEach { drawCircle(Color(0xFF19191B), size.minDimension * .075f, it) }
    }
}

@Composable
private fun TattooedDiceHand(rolling: Boolean, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val skin = Color(0xFFB77955)
        val ink = Color(0xFF18232B)
        drawRoundRect(skin, Offset(size.width * .24f, size.height * .35f), Size(size.width * .55f, size.height * .58f), androidx.compose.ui.geometry.CornerRadius(size.width * .18f))
        repeat(4) { i ->
            val x = size.width * (.25f + i * .145f)
            val length = size.height * (if (i == 1 || i == 2) .40f else .34f)
            drawRoundRect(skin, Offset(x, size.height * .10f), Size(size.width * .13f, length), androidx.compose.ui.geometry.CornerRadius(size.width * .07f))
        }
        drawRoundRect(skin, Offset(size.width * .08f, size.height * .48f), Size(size.width * .30f, size.height * .20f), androidx.compose.ui.geometry.CornerRadius(size.width * .10f))
        // Abstract lightning, star and snake tattoos only: deliberately no letters and no heart.
        val bolt = Path().apply { moveTo(size.width*.38f,size.height*.48f);lineTo(size.width*.48f,size.height*.39f);lineTo(size.width*.45f,size.height*.51f);lineTo(size.width*.57f,size.height*.45f);lineTo(size.width*.43f,size.height*.65f);lineTo(size.width*.46f,size.height*.53f);close() }
        drawPath(bolt, ink.copy(alpha=.82f))
        drawCircle(ink.copy(alpha=.72f), size.width*.045f, Offset(size.width*.67f,size.height*.56f), style=Stroke(3f))
        drawLine(ink.copy(alpha=.72f),Offset(size.width*.62f,size.height*.56f),Offset(size.width*.72f,size.height*.56f),3f)
        drawLine(ink.copy(alpha=.72f),Offset(size.width*.67f,size.height*.51f),Offset(size.width*.67f,size.height*.61f),3f)
        if (rolling) {
            drawCircle(Color.White.copy(alpha=.24f), size.width*.44f, Offset(size.width*.50f,size.height*.46f), style=Stroke(5f))
        }
    }
}

@Composable
private fun MoneyPile(amount: Int, collection: Float, winner: CrapsWinner, modifier: Modifier = Modifier) {
    Canvas(modifier.graphicsLayer {
        val direction = if (winner == CrapsWinner.OPPONENT) -1f else 1f
        translationY = direction * collection * size.height * .55f
        translationX = collection * size.width * .32f
        scaleX = 1f - collection * .32f
        scaleY = 1f - collection * .32f
        alpha = 1f - collection * .45f
    }.semantics { contentDescription = "Central pile of \$$amount in virtual American bills" }) {
        if (amount <= 0) return@Canvas
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { textAlign = Paint.Align.CENTER; typeface = android.graphics.Typeface.DEFAULT_BOLD }
        val count = (amount / 20).coerceIn(2, 7)
        repeat(count) { i ->
            val dx = (i % 3) * 8f + (i / 3) * 3f
            val dy = (i % 2) * 6f + (i / 2) * 4f
            val top = Offset(10f + dx, 8f + dy)
            val bill = Size(size.width * .82f, size.height * .63f)
            drawRoundRect(Color(0xFFB7C7A1), top, bill, androidx.compose.ui.geometry.CornerRadius(7f))
            drawRoundRect(Color(0xFF38533A), top, bill, androidx.compose.ui.geometry.CornerRadius(7f), style = Stroke(3f))
            drawRoundRect(Color(0xFF7F9974), top + Offset(8f, 7f), Size(bill.width - 16f, bill.height - 14f), androidx.compose.ui.geometry.CornerRadius(5f), style = Stroke(2f))
            // Small, unmistakably fictional Trump game portrait.
            val face = Offset(top.x + bill.width * .5f, top.y + bill.height * .47f)
            drawOval(Color(0xFFE7B986), Offset(face.x - 19f, face.y - 24f), Size(38f, 48f))
            val hair = Path().apply { moveTo(face.x-22f,face.y-17f);quadraticBezierTo(face.x-4f,face.y-34f,face.x+22f,face.y-19f);lineTo(face.x+13f,face.y-9f);quadraticBezierTo(face.x-3f,face.y-19f,face.x-22f,face.y-17f);close() }
            drawPath(hair, Color(0xFFE6C04E))
            drawLine(Color(0xFF593E32),Offset(face.x-9f,face.y+8f),Offset(face.x+9f,face.y+8f),2f)
            drawContext.canvas.nativeCanvas.apply {
                paint.color = android.graphics.Color.rgb(31, 62, 37)
                paint.textSize = 13f
                drawText("TRUMP", face.x, top.y + bill.height - 10f, paint)
                paint.textSize = 11f
                drawText("CASINO CHAOS", top.x + bill.width * .22f, top.y + 16f, paint)
                drawText("NOT LEGAL TENDER", top.x + bill.width * .75f, top.y + 16f, paint)
            }
        }
    }
}

@Composable
private fun CollectingHand(winner: CrapsWinner, progress: Float, modifier: Modifier = Modifier) {
    Canvas(modifier.semantics { contentDescription = if (winner == CrapsWinner.PLAYER) "Player hand collecting the money" else "Opponent hand collecting the money" }) {
        val fromTop = winner == CrapsWinner.OPPONENT
        val x = size.width * (.88f - progress * .36f)
        val startY = if (fromTop) -size.height * .18f else size.height * 1.18f
        val targetY = size.height * .57f
        val y = startY + (targetY - startY) * progress
        val skin = if (fromTop) Color(0xFF8E5A3C) else Color(0xFFC28762)
        drawRoundRect(skin, Offset(x - 54f, y - 24f), Size(150f, 48f), androidx.compose.ui.geometry.CornerRadius(24f))
        repeat(4) { i ->
            drawRoundRect(skin, Offset(x - 22f + i * 20f, y - 55f), Size(17f, 63f), androidx.compose.ui.geometry.CornerRadius(9f))
        }
        drawLine(Color(0xFF26323A),Offset(x+15f,y-18f),Offset(x+42f,y+7f),4f)
        drawCircle(Color(0xFF26323A),8f,Offset(x+52f,y-7f),style=Stroke(3f))
    }
}
