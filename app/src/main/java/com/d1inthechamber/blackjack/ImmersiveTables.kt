package com.d1inthechamber.blackjack

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*

/** One room, one physical playing surface. Controls sit outside the felt. */
@Composable
internal fun TableRoom(model: BlackjackViewModel, title: String, onBack: () -> Unit,
    onHelp: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    RoomMusic(model.room, model.settings.music)
    Box(Modifier.fillMaxSize()) {
        Image(painterResource(model.room.background), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = .43f)))
        Column(Modifier.fillMaxSize().padding(horizontal = 12.dp, vertical = 4.dp)) {
            Row(Modifier.fillMaxWidth().height(44.dp), verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onBack, contentPadding = PaddingValues(horizontal = 8.dp)) { Text("LOBBY", fontSize = 11.sp) }
                Text(title, Modifier.weight(1f), color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Black)
                Text("$" + formatChips(model.game.bankroll), color = model.room.accent, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                TextButton(onClick = onHelp, modifier = Modifier.width(48.dp).semantics { contentDescription = "Help" }, contentPadding = PaddingValues(0.dp)) { Text("?", fontSize = 20.sp) }
            }
            content()
        }
    }
}

@Composable
internal fun TableFelt(modifier: Modifier = Modifier, oval: Boolean = false) {
    val room = LocalRoomStyle.current
    val felt = when (room) {
        RoomStyle.CARNIVAL -> Color(0xFF542535)
        RoomStyle.EGYPT -> Color(0xFF173E50)
        RoomStyle.IRON -> Color(0xFF343E41)
        RoomStyle.WEST -> Color(0xFF243D42)
        RoomStyle.PUNK -> Color(0xFF263444)
        else -> Color(0xFF204334)
    }
    Canvas(modifier.testTag("table-felt")) {
        val radius = if (oval) size.height * .27f else 22.dp.toPx()
        val corner = androidx.compose.ui.geometry.CornerRadius(radius)
        drawRoundRect(Color.Black.copy(alpha = .5f), Offset(0f, 7.dp.toPx()), size, corner)
        drawRoundRect(Brush.verticalGradient(listOf(Color(0xFF594033), Color(0xFF1D1412))), cornerRadius = corner)
        val inset = 10.dp.toPx()
        drawRoundRect(Brush.radialGradient(listOf(felt, felt.copy(red = felt.red * .42f, green = felt.green * .42f, blue = felt.blue * .42f)),
            Offset(size.width * .5f, size.height * .34f), size.maxDimension * .8f), Offset(inset, inset),
            Size(size.width - inset * 2, size.height - inset * 2), androidx.compose.ui.geometry.CornerRadius((radius - inset).coerceAtLeast(1f)))
        drawRoundRect(room.accent.copy(alpha = .48f), Offset(inset, inset), Size(size.width - inset * 2, size.height - inset * 2),
            androidx.compose.ui.geometry.CornerRadius((radius - inset).coerceAtLeast(1f)), style = Stroke(1.dp.toPx()))
        // Fine felt grain, never large enough to compete with card faces.
        var y = inset + 4f
        while (y < size.height - inset) {
            drawLine(Color.White.copy(alpha = .016f), Offset(inset + 8f, y), Offset(size.width - inset - 8f, y), .6f)
            y += 4.dp.toPx()
        }
    }
}

@Composable
internal fun TableCard(text: String, width: Dp, height: Dp, angle: Float = 0f, modifier: Modifier = Modifier) {
    val arrival = remember { Animatable(0f) }
    LaunchedEffect(text) { arrival.snapTo(0f); arrival.animateTo(1f, tween(210, easing = FastOutSlowInEasing)) }
    Box(modifier.graphicsLayer {
        rotationZ = angle
        translationY = (1f - arrival.value) * -16.dp.toPx()
        alpha = .25f + .75f * arrival.value
    }) { CardView(text, width, height) }
}

@Composable
internal fun ChipStack(amount: Int, modifier: Modifier = Modifier, active: Boolean = false) {
    val accent = LocalRoomStyle.current.accent
    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Canvas(Modifier.size(22.dp, 24.dp)) {
            repeat(if (amount > 0) 3 else 1) { n ->
                val y = size.height * .65f - n * 4.dp.toPx()
                drawOval(Color(0xFF080908), Offset(1f, y + 2.dp.toPx()), Size(size.width - 2f, 9.dp.toPx()))
                drawOval(if (active) accent else Color(0xFFAA5D47), Offset(0f, y), Size(size.width, 9.dp.toPx()))
                drawOval(Color.White.copy(alpha = .55f), Offset(3.dp.toPx(), y + 1.dp.toPx()), Size(size.width - 6.dp.toPx(), 6.dp.toPx()), style = Stroke(1.dp.toPx()))
            }
        }
        Text("$" + amount, color = if (active) accent else Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}
