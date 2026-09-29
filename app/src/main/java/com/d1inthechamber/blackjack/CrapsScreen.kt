package com.d1inthechamber.blackjack

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.*

@Composable
internal fun CrapsScreen(model: BlackjackViewModel, onBack: () -> Unit) {
    val game = model.craps
    val revision = model.revision
    var rolling by remember { mutableStateOf(false) }
    val throwProgress = remember { Animatable(0f) }
    val collection = remember { Animatable(0f) }
    val busy = rolling || game.winner != CrapsWinner.NONE
    val canShoot = !busy && (game.active || model.game.bankroll >= game.bet)
    val shoot: () -> Unit = {
        if (canShoot && model.beginCrapsHand()) rolling = true
    }
    RoomMusic(model.room, model.settings.music)
    HumanReactionVoice(game.collectionPulse, when (game.winner) {
        CrapsWinner.PLAYER -> HumanReaction.CHEER
        CrapsWinner.OPPONENT -> HumanReaction.FRUSTRATED
        else -> null
    }, model.settings.voices)

    LaunchedEffect(rolling) {
        if (rolling) {
            throwProgress.snapTo(0f)
            throwProgress.animateTo(.90f, tween(1000, easing = LinearEasing))
            model.shootCraps()
            throwProgress.animateTo(1f, tween(180))
            rolling = false
        }
    }
    LaunchedEffect(game.collectionPulse, rolling) {
        if (!rolling && game.winner != CrapsWinner.NONE) {
            collection.snapTo(0f)
            collection.animateTo(1f, tween(2100, easing = LinearEasing))
            model.finishCrapsCollection()
        }
    }

    BoxWithConstraints(Modifier.fillMaxSize().background(Color(0xFF100E10))) {
        val landscape = maxWidth > maxHeight && maxWidth >= 600.dp
        val sceneHeight = if (landscape) (maxHeight - 64.dp).coerceAtLeast(180.dp)
            else (maxHeight - 242.dp).coerceIn(280.dp, 440.dp)
        Column(
            Modifier.fillMaxSize().padding(horizontal = 16.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(Modifier.fillMaxWidth().heightIn(min = 44.dp), verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onBack) { Text("LOBBY") }
                Text("STREET CRAPS", Modifier.weight(1f), color = Color.White,
                    fontSize = 19.sp, fontWeight = FontWeight.Black)
                Text("$" + formatChips(model.game.bankroll), color = model.room.accent,
                    fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
            if (landscape) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    CrapsScene(model, rolling, throwProgress.value, collection.value, canShoot, shoot,
                        Modifier.weight(.60f).height(sceneHeight))
                    Column(Modifier.weight(.40f).heightIn(max = sceneHeight).verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        CrapsControls(model, rolling, canShoot, shoot)
                    }
                }
            } else {
                CrapsScene(model, rolling, throwProgress.value, collection.value, canShoot, shoot,
                    Modifier.fillMaxWidth().height(sceneHeight))
                CrapsControls(model, rolling, canShoot, shoot)
            }
            if (model.game.bankroll < 10 && !game.active && !busy) {
                Button(onClick = { model.refill() }, modifier = Modifier.fillMaxWidth()) {
                    Text("REFILL • 1,000 FREE VIRTUAL DOLLARS")
                }
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun ColumnScope.CrapsControls(model: BlackjackViewModel, rolling: Boolean,
    canShoot: Boolean, shoot: () -> Unit) {
    val revision = model.revision
    val game = model.craps
    var help by remember { mutableStateOf(false) }
    Text(when { rolling -> ""; game.winner == CrapsWinner.PLAYER -> "YOU WIN"; game.winner == CrapsWinner.OPPONENT -> "HAND LOST"; game.active -> "${game.dieOne + game.dieTwo} · POINT ${game.point}"; else -> "" }, color = Color(0xFFF5E8D1), fontSize = 14.sp,
        modifier = Modifier.fillMaxWidth().testTag("craps-message"), textAlign = TextAlign.Center)
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        listOf(10, 25, 50, 100).forEach { amount ->
            OutlinedButton(
                onClick = { model.setCrapsBet(amount) },
                enabled = !rolling && !game.active && game.winner == CrapsWinner.NONE && model.game.bankroll >= amount,
                modifier = Modifier.weight(1f), contentPadding = PaddingValues(horizontal = 2.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = if (game.bet == amount) model.room.accent.copy(alpha = .16f) else Color.Transparent)
            ) { Text("$" + amount, fontSize = 13.sp, fontWeight = FontWeight.Bold) }
        }
    }
    Button(onClick = shoot, enabled = canShoot,
        modifier = Modifier.fillMaxWidth().height(52.dp).testTag("craps-roll")) {
        Text(when {
            rolling -> "ROLLING…"
            game.winner != CrapsWinner.NONE -> "COLLECTING…"
            game.active -> "ROLL · POINT " + game.point
            else -> "ROLL · $" + game.bet
        }, fontSize = 13.sp, fontWeight = FontWeight.Black)
    }
    TextButton(onClick = { help = true }, modifier = Modifier.align(Alignment.CenterHorizontally)) { Text("RULES", fontSize = 11.sp) }
    if (help) AlertDialog(onDismissRequest = { help = false }, title = { Text("Street craps") }, text = {
        Text("Choose your stake and tap Roll or the dice hand. Your opponent matches the stake.\n\n7 or 11 wins the first roll; 2, 3 or 12 loses. Any other total sets the point. Make it again before rolling 7 to win.\n\nThe winner reaches in and collects the bills. Virtual money only.")
    }, confirmButton = { TextButton(onClick = { help = false }) { Text("CLOSE") } })
}

@Composable
private fun CrapsScene(model: BlackjackViewModel, rolling: Boolean, throwProgress: Float,
    collection: Float, canShoot: Boolean, shoot: () -> Unit, modifier: Modifier) {
    val revision = model.revision
    val game = model.craps
    val hands = ImageBitmap.imageResource(R.drawable.craps_hands)
    val bill = ImageBitmap.imageResource(R.drawable.craps_banknote)
    val direction = collectionDirection(game.winner)
    val collecting = !rolling && game.winner != CrapsWinner.NONE
    BoxWithConstraints(modifier.clip(RoundedCornerShape(20.dp)).border(1.dp,
        model.room.accent.copy(alpha = .65f), RoundedCornerShape(20.dp)).testTag("craps-street")) {
        Image(painterResource(R.drawable.craps_alley), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(
            Color.Black.copy(alpha = .28f), Color.Transparent, Color.Black.copy(alpha = .22f)))))
        val handWidth = minOf(maxWidth * .42f, maxHeight * .44f)
        val handHeight = handWidth * 4f / 3f
        val potY = maxHeight * .13f
        val travel = maxHeight * .63f + handHeight
        val billDistance = if (collecting) collectionBillDistance(collection) else 0f
        val handDistance = collectionHandDistance(collection)
        val opponentHeight = (maxHeight * .25f).coerceAtMost(100.dp)
        Column(Modifier.align(Alignment.TopCenter).padding(top = 5.dp),
            horizontalAlignment = Alignment.CenterHorizontally) {
            CrapsOpponent(model.room, game.winner, Modifier.size(112.dp, opponentHeight))

        }
        Text(if (game.point == 0) "COME OUT" else "POINT " + game.point,
            color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Black,
            modifier = Modifier.align(Alignment.TopStart).padding(12.dp)
                .background(Color(0xCC211B16), RoundedCornerShape(7.dp)).padding(7.dp))

        Canvas(Modifier.fillMaxSize().semantics {
            contentDescription = "Dice showing " + game.dieOne + " and " + game.dieTwo
        }.testTag("craps-dice")) {
            val flight = ((throwProgress - .46f) / .44f).coerceIn(0f, 1f)
            val airborne = rolling && throwProgress in .46f.. .90f
            val landingY = size.height * .44f
            val startY = size.height * .87f
            val y = if (airborne) startY + (landingY - startY) * flight - sin(flight * PI).toFloat() * size.height * .14f else landingY
            if (!rolling || throwProgress >= .46f) {
                repeat(2) { i ->
                    val x = size.width * (if (i == 0) .39f else .63f)
                    val side = min(size.width * .145f, size.height * .145f)
                    val value = if (airborne) ((throwProgress * 40).toInt() + i * 3) % 6 + 1
                        else if (i == 0) game.dieOne else game.dieTwo
                    drawDie(value, Offset(x, y + i * side * .13f), side,
                        if (airborne) flight * (if (i == 0) 570f else -650f) else if (i == 0) -13f else 17f)
                }
            }
        }
        Canvas(Modifier.align(Alignment.Center).offset(y = potY + travel * direction * billDistance)
            .size(maxWidth * .64f, maxHeight * .23f).testTag("craps-money-pile")
            .semantics {
                contentDescription = "Central pile of $" + game.centerPot + " in virtual American bills"
                stateDescription = if (billDistance == 0f) "Centered" else if (direction < 0) "Moving to opponent" else "Moving to player"
            }) {
            if (game.centerPot > 0) {
                val count = (game.centerPot / 25).coerceIn(2, 6)
                repeat(count) { i ->
                    val w = size.width * .83f
                    val h = w * bill.height / bill.width
                    val x = (size.width - w) / 2 + (i % 3 - 1) * size.width * .04f
                    val y = (size.height - h) / 2 + (i - count / 2f) * 3.dp.toPx()
                    rotate(-13f + i * 7f, Offset(size.width / 2, size.height / 2)) {
                        drawRoundRect(Color.Black.copy(alpha = .4f), Offset(x + 2.dp.toPx(), y + 4.dp.toPx()),
                            Size(w, h), CornerRadius(3.dp.toPx()))
                        drawImage(bill, srcSize = IntSize(bill.width, bill.height),
                            dstOffset = IntOffset(x.roundToInt(), y.roundToInt()),
                            dstSize = IntSize(w.roundToInt(), h.roundToInt()), filterQuality = FilterQuality.Medium)
                    }
                }
            }
        }
        if (game.centerPot > 0 && billDistance == 0f) {
            Text("$" + game.centerPot, color = Color(0xFFFFE9B0),
                fontSize = 10.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.Center).offset(y = potY + maxHeight * .14f)
                    .background(Color.Black.copy(alpha = .76f), RoundedCornerShape(5.dp)).padding(5.dp))
        }
        if (collecting) {
            val frame = if (collection < .48f) 1 else if (collection < .60f) 0 else 2
            val collectorModifier = Modifier.align(Alignment.Center)
                .offset(y = potY + travel * direction * handDistance - if (direction < 0) handHeight * .20f else 0.dp)
                .size(handWidth, handHeight)
                .testTag("craps-collector").semantics {
                    contentDescription = if (direction < 0) "Opponent hand collecting the money" else "Player hand collecting the money"
                    stateDescription = if (direction < 0) "Opponent: ${model.room.id}" else "Player: tattooed hand"
                }
            if (game.winner == CrapsWinner.OPPONENT) {
                OpponentHand(model.room, collection >= .48f, collectorModifier)
            } else HandSprite(hands, frame, collectorModifier)
        }
        val shake = if (rolling && throwProgress < .46f) sin(throwProgress * 70f) else 0f
        val frame = if (rolling && throwProgress >= .46f) 7 else 2
        HandSprite(hands, frame, Modifier.align(Alignment.BottomCenter)
            .offset(x = (shake * 6).dp, y = if (collecting) handHeight else 18.dp)
            .size(handWidth, handHeight).rotate(shake * 11f)
            .testTag("craps-hand").semantics { contentDescription = "Tattooed hand holding dice; tap to roll" }
            .clickable(enabled = canShoot, onClick = shoot))
    }
}

@Composable
private fun HandSprite(atlas: ImageBitmap, frame: Int, modifier: Modifier) {
    Canvas(modifier) {
        val width = atlas.width / 4
        val height = atlas.height / 2
        drawImage(atlas, srcOffset = IntOffset(frame % 4 * width, frame / 4 * height),
            srcSize = IntSize(width, height), dstSize = IntSize(size.width.roundToInt(), size.height.roundToInt()),
            filterQuality = FilterQuality.Medium)
    }
}

@Composable
private fun CrapsOpponent(room: RoomStyle, winner: CrapsWinner, modifier: Modifier) {
    val context = LocalContext.current
    var sheet by remember(room) { mutableStateOf<ImageBitmap?>(null) }
    LaunchedEffect(room) { sheet = withContext(Dispatchers.Default) { decodeDealer(context, room) } }
    Canvas(modifier.clipToBounds().testTag("craps-opponent").semantics {
        contentDescription = room.host
        stateDescription = if (sheet == null) "Loading" else "Ready"
    }) {
        sheet?.let { drawDealerPose(it, room, when (winner) {
            CrapsWinner.PLAYER -> 6
            CrapsWinner.OPPONENT -> 10
            else -> 0
        }, 0f, size.width) }
    }
}

private fun DrawScope.drawDie(value: Int, center: Offset, side: Float, angle: Float) {
    drawOval(Color.Black.copy(alpha = .6f), center + Offset(-side * .55f, side * .35f), Size(side * 1.2f, side * .35f))
    rotate(angle, center) {
        val top = center - Offset(side / 2, side / 2)
        drawRoundRect(Color(0xFF766851), top + Offset(2.dp.toPx(), 5.dp.toPx()), Size(side, side), CornerRadius(side * .13f))
        drawRoundRect(Brush.linearGradient(listOf(Color(0xFFFFFFED), Color(0xFFD4C5A4))),
            top, Size(side, side), CornerRadius(side * .13f))
        drawRoundRect(Color(0xFFC8B798), top, Size(side, side), CornerRadius(side * .13f), style = Stroke(1.dp.toPx()))
        val positions = when (value) {
            1 -> listOf(1 to 1)
            2 -> listOf(0 to 0, 2 to 2)
            3 -> listOf(0 to 0, 1 to 1, 2 to 2)
            4 -> listOf(0 to 0, 2 to 0, 0 to 2, 2 to 2)
            5 -> listOf(0 to 0, 2 to 0, 1 to 1, 0 to 2, 2 to 2)
            else -> listOf(0 to 0, 2 to 0, 0 to 1, 2 to 1, 0 to 2, 2 to 2)
        }
        positions.forEach { (x, y) ->
            drawCircle(Color(0xFF251C17), side * .068f, top + Offset(side * (.26f + x * .24f), side * (.26f + y * .24f)))
        }
    }
}


@Composable
private fun OpponentHand(room: RoomStyle, gripping: Boolean, modifier: Modifier) {
    val context = LocalContext.current
    var atlas by remember(room) { mutableStateOf<ImageBitmap?>(null) }
    LaunchedEffect(room) { atlas = withContext(Dispatchers.Default) { decodeDealer(context, room) } }
    Canvas(modifier) {
        atlas?.let { sheet ->
            // Isolate the forward arm of the approved dealing pose, including its
            // skin tone, cuff, jewelry and tattoos. Never reuse the player's atlas.
            val rows = when (room) {
                RoomStyle.CARNIVAL -> intArrayOf(356,702)
                RoomStyle.PUNK -> intArrayOf(362,716)
                RoomStyle.IRON -> intArrayOf(358,712)
                RoomStyle.EGYPT -> intArrayOf(366,728)
                RoomStyle.GREEN -> intArrayOf(362,724)
                else -> intArrayOf(364,723)
            }
            val cw = sheet.width / 4
            val rowTop = (rows[0] * sheet.height / 1086f).roundToInt()
            val rowHeight = ((rows[1]-rows[0]) * sheet.height / 1086f).roundToInt()
            // Continue the character's sleeve to the far edge of the scene so
            // the reaching hand stays attached to an arm instead of floating.
            val reachWidth = size.width * .48f
            drawImage(sheet,
                srcOffset = IntOffset((cw*.11f).roundToInt(), rowTop + (rowHeight*.51f).roundToInt()),
                srcSize = IntSize((cw*.16f).roundToInt(), (rowHeight*.15f).roundToInt()),
                dstOffset = IntOffset((size.width*.32f).roundToInt(), (-size.height*3f).roundToInt()),
                dstSize = IntSize(reachWidth.roundToInt(), (size.height*3.16f).roundToInt()),
                filterQuality = FilterQuality.Medium)
            // The outer forearm boundary differs by costume. These small masks
            // exclude adjacent torso pixels without altering the character art.
            val edge = when(room) {
                RoomStyle.VEGAS -> listOf(.84f to 0f,.68f to .49f,.69f to .65f,.81f to .83f,.78f to .94f,.66f to 1f)
                RoomStyle.CARNIVAL -> listOf(.91f to 0f,.73f to .48f,.77f to .69f,.86f to .87f,.83f to 1f)
                RoomStyle.EGYPT -> listOf(.92f to 0f,.73f to .46f,.72f to .62f,.94f to .84f,.91f to 1f)
                RoomStyle.IRON -> listOf(.91f to 0f,.74f to .48f,.71f to .61f,.95f to .87f,.90f to 1f)
                RoomStyle.WEST -> listOf(.85f to 0f,.67f to .47f,.65f to .65f,.98f to .86f,.96f to 1f)
                RoomStyle.PUNK -> listOf(.92f to 0f,.70f to .50f,.65f to .64f,.84f to .89f,.77f to 1f)
                RoomStyle.GREEN -> listOf(.87f to 0f,.57f to .35f,.58f to .54f,.70f to .73f,.72f to .85f,.59f to 1f)
            }
            val mask = Path().apply {
                moveTo(size.width*.22f,0f)
                edge.forEach { (x,y) -> lineTo(size.width*x,size.height*y) }
                lineTo(0f,size.height);lineTo(0f,0f);close()
            }
            clipPath(mask) {
                drawImage(sheet,
                    srcOffset = IntOffset(0, rowTop + (rowHeight*.50f).roundToInt()),
                    srcSize = IntSize((cw * if(room == RoomStyle.WEST) .44f else .39f).roundToInt(), (rowHeight*.50f).roundToInt()),
                    dstSize = IntSize(size.width.roundToInt(), (size.height * if(gripping) .94f else 1f).roundToInt()),
                    filterQuality = FilterQuality.Medium)
            }
        }
    }
}
