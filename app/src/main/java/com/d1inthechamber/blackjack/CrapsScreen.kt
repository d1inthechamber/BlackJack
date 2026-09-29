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
import kotlinx.coroutines.delay
import kotlin.math.*

@Composable
internal fun CrapsScreen(model: BlackjackViewModel, onBack: () -> Unit) {
    val game = model.craps
    val audio = LocalCasinoAudio.current
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
            audio?.play(CasinoSound.DICE_SHAKE)
            throwProgress.snapTo(0f)
            throwProgress.animateTo(.46f, tween(450, easing = LinearEasing))
            audio?.play(CasinoSound.DICE_ROLL)
            throwProgress.animateTo(.90f, tween(550, easing = LinearEasing))
            model.shootCraps()
            throwProgress.animateTo(1f, tween(180))
            rolling = false
        }
    }
    LaunchedEffect(rolling, game.active, game.opponentShooter, game.winner) {
        if(!rolling && game.active && game.opponentShooter && game.winner == CrapsWinner.NONE) {
            delay(850)
            rolling=true
        }
    }
    LaunchedEffect(game.collectionPulse, rolling) {
        if (!rolling && game.winner != CrapsWinner.NONE) {
            audio?.play(if(game.winner==CrapsWinner.PLAYER)CasinoSound.WIN else CasinoSound.LOSE,.4f)
            collection.snapTo(0f)
            collection.animateTo(.60f, tween(1260, easing = LinearEasing))
            audio?.play(CasinoSound.MONEY)
            collection.animateTo(1f, tween(840, easing = LinearEasing))
            model.finishCrapsCollection()
        }
    }

    BoxWithConstraints(Modifier.fillMaxSize().background(Color(0xFF100E10))) {
        // Preserve one portrait composition on both Fold screens, including the
        // wide inner screen. Extra width becomes margins instead of a new layout.
        val playWidth = minOf(maxWidth - 32.dp, 430.dp,
            ((maxHeight - 242.dp) / 1.17f).coerceAtLeast(280.dp))
        val sceneHeight = playWidth * 1.17f
        Column(
            Modifier.align(Alignment.TopCenter).width(playWidth).fillMaxHeight().verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(Modifier.fillMaxWidth().heightIn(min = 44.dp), verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onBack) { Text("LOBBY") }
                Text("STREET CRAPS", Modifier.weight(1f), color = Color.White,
                    fontSize = 19.sp, fontWeight = FontWeight.Black)
                Text("$" + formatChips(model.game.bankroll), color = model.room.accent,
                    fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
            CrapsScene(model, rolling, throwProgress.value, collection.value, canShoot, shoot,
                Modifier.fillMaxWidth().height(sceneHeight))
            CrapsControls(model, rolling, canShoot, shoot)
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
    val audio=LocalCasinoAudio.current
    Text(when { rolling -> ""; game.winner == CrapsWinner.PLAYER -> "YOU WIN"; game.winner == CrapsWinner.OPPONENT -> "HAND LOST"; game.active -> "${game.dieOne + game.dieTwo} · POINT ${game.point}"; else -> "" }, color = Color(0xFFF5E8D1), fontSize = 14.sp,
        modifier = Modifier.fillMaxWidth().testTag("craps-message"), textAlign = TextAlign.Center)
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        listOf(10, 25, 50, 100).forEach { amount ->
            OutlinedButton(
                onClick = { audio?.play(CasinoSound.MONEY,.4f);model.setCrapsBet(amount) },
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
            game.opponentShooter && game.active -> "OPPONENT · POINT " + game.point
            game.opponentShooter -> "OPPONENT ROLLS · $" + game.bet
            game.active -> "ROLL · POINT " + game.point
            else -> "ROLL · $" + game.bet
        }, fontSize = 13.sp, fontWeight = FontWeight.Black)
    }
    TextButton(onClick = { help = true }, modifier = Modifier.align(Alignment.CenterHorizontally)) { Text("RULES", fontSize = 11.sp) }
    if (help) AlertDialog(onDismissRequest = { help = false }, title = { Text("Street craps") }, text = {
        Text("Choose your stake and tap Roll or the dice hand. Your opponent matches the stake.\n\nThe shooter wins on 7 or 11 on the first roll, and loses on 2, 3 or 12. Any other total sets the point. Make it before a 7 to win.\n\nThe winner collects the bills and shoots next. When your opponent shoots, you cover their bet; they keep rolling automatically until the hand is decided. Virtual money only.")
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
        val sceneRevision = model.revision
        val centerPot = game.centerPot
        Image(painterResource(R.drawable.craps_alley), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(
            Color.Black.copy(alpha = .28f), Color.Transparent, Color.Black.copy(alpha = .22f)))))
        val handWidth = minOf(maxWidth * .42f, maxHeight * .44f)
        val handHeight = handWidth * 4f / 3f
        val potY = maxHeight * .13f
        val opponentHeight = (maxHeight * .25f).coerceAtMost(100.dp)
        val opponentThrowing = game.opponentShooter && !collecting && (!rolling || throwProgress < .50f)
        val opponentHandWidth = minOf(maxWidth * .28f, maxHeight * .27f)
        val opponentHandHeight = opponentHandWidth * 4f / 3f
        val opponentHandTop = 5.dp + opponentHeight * .24f
        val opponentPalmY = opponentHandTop + opponentHandHeight * .82f
        // The opponent reaches from the lower torso and receives the pot there.
        // Perspective shrinks the hand and bills together as they move away.
        val travel = if (direction < 0) maxHeight * .63f - (5.dp + opponentHeight * 1.35f)
            else maxHeight * .63f + handHeight
        val billDistance = if (collecting) collectionBillDistance(collection) else 0f
        val handDistance = collectionHandDistance(collection)
        val handScale = if (direction < 0) 1f - .66f * handDistance else 1f
        val billScale = if (direction < 0) 1f - .66f * billDistance else 1f
        Column(Modifier.align(Alignment.TopCenter).padding(top = 5.dp),
            horizontalAlignment = Alignment.CenterHorizontally) {
            CrapsOpponent(model.room, game.winner, Modifier.size(112.dp, opponentHeight), opponentThrowing)

        }
        Text(if (game.point == 0) { if(game.opponentShooter) "OPPONENT ROLLS" else "YOUR ROLL" } else "POINT " + game.point,
            color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Black,
            modifier = Modifier.align(Alignment.TopStart).padding(12.dp)
                .background(Color(0xCC211B16), RoundedCornerShape(7.dp)).padding(7.dp))

        Canvas(Modifier.fillMaxSize().semantics {
            contentDescription = "Dice showing " + game.dieOne + " and " + game.dieTwo
        }.testTag("craps-dice")) {
            val flight = ((throwProgress - .46f) / .44f).coerceIn(0f, 1f)
            val airborne = rolling && throwProgress in .46f.. .90f
            val landingY = size.height * .44f
            val startY = if(game.opponentShooter) opponentPalmY.toPx() else size.height * .87f
            val y = if (airborne) startY + (landingY - startY) * flight - sin(flight * PI).toFloat() * size.height * .14f else landingY
            if ((!rolling && !opponentThrowing) || (rolling && throwProgress >= .46f)) {
                repeat(2) { i ->
                    val landingX = size.width * (if (i == 0) .39f else .63f)
                    val startX = size.width * (if(i==0) .465f else .535f)
                    val x = if(airborne&&game.opponentShooter) startX+(landingX-startX)*flight else landingX
                    val side = min(size.width * .145f, size.height * .145f) *
                        if(airborne&&game.opponentShooter) .38f+.62f*flight else 1f
                    val value = if (airborne) ((throwProgress * 40).toInt() + i * 3) % 6 + 1
                        else if (i == 0) game.dieOne else game.dieTwo
                    drawDie(value, Offset(x, y + i * side * .13f), side,
                        if (airborne) flight * (if (i == 0) 570f else -650f) else if (i == 0) -13f else 17f)
                }
            }
        }
        Canvas(Modifier.align(Alignment.Center).offset(y = potY + travel * direction * billDistance)
            .size(maxWidth * .64f, maxHeight * .23f).graphicsLayer { scaleX = billScale; scaleY = billScale }.testTag("craps-money-pile")
            .semantics {
                contentDescription = "Central pile of $" + centerPot + " in virtual American bills"
                stateDescription = if (billDistance == 0f) "Centered" else if (direction < 0) "Moving to opponent" else "Moving to player"
            }) {
            if (centerPot > 0) {
                val count = (centerPot / 25).coerceIn(2, 6)
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
        if (centerPot > 0 && billDistance == 0f) {
            Text("$" + centerPot, color = Color(0xFFFFE9B0),
                fontSize = 10.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.Center).offset(y = potY + maxHeight * .14f)
                    .background(Color.Black.copy(alpha = .76f), RoundedCornerShape(5.dp)).padding(5.dp))
        }
        if (collecting) {
            val frame = if (collection < .48f) 1 else if (collection < .60f) 0 else 2
            val collectorY = potY + travel * direction * handDistance - if (direction < 0) handHeight * .30f * handScale else 0.dp
            val collectorModifier = Modifier.align(Alignment.Center)
                .offset(y = collectorY)
                .size(handWidth, handHeight)
                .graphicsLayer { scaleX = handScale; scaleY = handScale }
                .testTag("craps-collector").semantics {
                    contentDescription = if (direction < 0) "Opponent hand collecting the money" else "Player hand collecting the money"
                    stateDescription = if (direction < 0) "Opponent: ${model.room.id}" else "Player: tattooed hand"
                }
            if (game.winner == CrapsWinner.OPPONENT) {
                val drawnWidth = handWidth * handScale
                val drawnHeight = handHeight * handScale
                val handTop = maxHeight / 2 + collectorY - drawnHeight / 2
                // The arm starts at the exact cut line in the matching body pose.
                // One continuous piece of artwork connects that line to the pot.
                OpponentHand(model.room, collection >= .48f, (maxWidth-drawnWidth)/2, handTop,
                    drawnWidth, drawnHeight, (maxWidth-opponentHeight)/2,
                    5.dp + opponentHeight * .50f,
                    opponentHeight * if (model.room == RoomStyle.WEST) .44f else .39f,
                    Modifier.fillMaxSize().testTag("craps-collector").semantics {
                        stateDescription = "Opponent: ${model.room.id}"
                    })
            } else HandSprite(hands, frame, collectorModifier)
        }
        val shake = if (rolling && throwProgress < .46f) sin(throwProgress * 70f) else 0f
        val frame = if (rolling && throwProgress >= .46f) 7 else 2
        if(!game.opponentShooter) HandSprite(hands, frame, Modifier.align(Alignment.BottomCenter)
            .offset(x = (shake * 6).dp, y = if (collecting) handHeight else 18.dp)
            .size(handWidth, handHeight).rotate(shake * 11f)
            .testTag("craps-hand").semantics { contentDescription = "Tattooed hand holding dice; tap to roll" }
            .clickable(enabled = canShoot, onClick = shoot))
        if(opponentThrowing) {
            OpponentHand(model.room,false,(maxWidth-opponentHandWidth)/2+(shake*3).dp,
                opponentHandTop+(shake*2).dp,opponentHandWidth,opponentHandHeight,
                (maxWidth-opponentHeight)/2,5.dp+opponentHeight*.50f,
                opponentHeight * if(model.room==RoomStyle.WEST) .44f else .39f,
                Modifier.fillMaxSize().testTag("craps-opponent-dice-hand").clickable(enabled=canShoot,onClick=shoot),
                description="Opponent holding and throwing dice")
            if(!rolling||throwProgress<.46f) Canvas(Modifier.fillMaxSize().testTag("craps-held-dice")) {
                repeat(2) { i -> drawDie(i+2,Offset(size.width*(if(i==0).465f else .535f)+shake*3.dp.toPx(),
                    opponentPalmY.toPx()+shake*2.dp.toPx()),size.width*.056f,shake*14f+i*15f) }
            }
        }
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
private fun CrapsOpponent(room: RoomStyle, winner: CrapsWinner, modifier: Modifier, reaching:Boolean=false) {
    val context = LocalContext.current
    var sheet by remember(room) { mutableStateOf<ImageBitmap?>(null) }
    LaunchedEffect(room) { sheet = withContext(Dispatchers.Default) { decodeDealer(context, room) } }
    Canvas(modifier.clipToBounds().testTag("craps-opponent").semantics {
        contentDescription = "Opponent"
        stateDescription = if (sheet == null) "Loading" else "Ready"
    }) {
        sheet?.let {
            val side = minOf(size.width, size.height)
            val left = (size.width - side) / 2
            if (winner == CrapsWinner.OPPONENT || reaching) {
                // The moving foreground arm replaces this pose's original arm.
                // Keeping the matching shoulder avoids a crossed-arm third hand.
                val body = Path().apply {
                    fillType = PathFillType.EvenOdd
                    addRect(androidx.compose.ui.geometry.Rect(0f, 0f, size.width, size.height))
                    addRect(androidx.compose.ui.geometry.Rect(left, side * .50f,
                        left + side * if (room == RoomStyle.WEST) .44f else .39f, side))
                }
                clipPath(body) { drawDealerPose(it, room, 4, left, side) }
            } else drawDealerPose(it, room, if (winner == CrapsWinner.PLAYER) 6 else 0, left, side)
        }
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
private fun OpponentHand(room: RoomStyle, gripping: Boolean, handLeft: Dp, handTop: Dp,
    handWidth: Dp, handHeight: Dp, originLeft: Dp, originTop: Dp, originWidth: Dp, modifier: Modifier,
    description:String="Opponent hand collecting the money") {
    val context = LocalContext.current
    var arm by remember(room) { mutableStateOf<android.graphics.Bitmap?>(null) }
    val paint = remember { android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG or android.graphics.Paint.FILTER_BITMAP_FLAG) }
    val rows = when (room) {
        RoomStyle.CARNIVAL -> intArrayOf(356,702)
        RoomStyle.PUNK -> intArrayOf(362,716)
        RoomStyle.IRON -> intArrayOf(358,712)
        RoomStyle.EGYPT -> intArrayOf(366,728)
        RoomStyle.GREEN -> intArrayOf(362,724)
        else -> intArrayOf(364,723)
    }
    // Crop and mask the actual forward arm once. Preserve its original cuff,
    // tattoos, fingers and transparency instead of adding a stretched texture.
    LaunchedEffect(room) {
        arm = withContext(Dispatchers.Default) {
            val atlas = decodeDealer(context, room)
            val cw = atlas.width / 4
            val rowTop = (rows[0] * atlas.height / 1086f).roundToInt()
            val rowHeight = ((rows[1]-rows[0]) * atlas.height / 1086f).roundToInt()
            val width = (cw * if (room == RoomStyle.WEST) .44f else .39f).roundToInt()
            val height = (rowHeight * .50f).roundToInt()
            val bitmap = android.graphics.Bitmap.createBitmap(width, height, android.graphics.Bitmap.Config.ARGB_8888)
            val canvas = android.graphics.Canvas(bitmap)
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
            val mask = android.graphics.Path().apply {
                moveTo(width*.22f,0f)
                edge.forEach { (x,y) -> lineTo(width*x,height*y) }
                lineTo(0f,height.toFloat());lineTo(0f,0f);close()
            }
            canvas.clipPath(mask)
            canvas.drawBitmap(atlas.asAndroidBitmap(),
                android.graphics.Rect(0,rowTop+height,width,rowTop+height*2),
                android.graphics.Rect(0,0,width,height),null)
            bitmap
        }
    }
    Canvas(modifier.semantics {
        contentDescription = if (arm == null) "Loading opponent hand" else description
    }) {
        arm?.let { bitmap ->
            val width = handWidth.toPx()
            val height = handHeight.toPx() * if (gripping) .94f else 1f
            val left = handLeft.toPx()
            val top = handTop.toPx()
            val columns = 8
            val rowsCount = 24
            val vertices = FloatArray((columns+1)*(rowsCount+1)*2)
            var index = 0
            for (row in 0..rowsCount) {
                val v = row.toFloat()/rowsCount
                // Only the sleeve bends. The lower forearm and whole hand retain
                // their artwork proportions while the cash moves with them.
                val blend = ((.58f-v)/.58f).coerceIn(0f,1f)
                val bend = blend*blend*(3f-2f*blend)
                for (column in 0..columns) {
                    val u = column.toFloat()/columns
                    val x = left+u*width
                    vertices[index++] = x+(originLeft.toPx()+u*originWidth.toPx()-x)*bend
                    vertices[index++] = top+v*height+(originTop.toPx()-top)*bend
                }
            }
            drawContext.canvas.nativeCanvas.drawBitmapMesh(bitmap,columns,rowsCount,vertices,0,null,0,paint)
        }
    }
}
