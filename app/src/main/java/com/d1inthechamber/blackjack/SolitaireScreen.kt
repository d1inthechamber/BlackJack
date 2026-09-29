package com.d1inthechamber.blackjack

import androidx.compose.foundation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import androidx.compose.ui.zIndex
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.PI
import kotlin.math.sin

@Composable
internal fun SolitaireScreen(model: BlackjackViewModel, onBack: () -> Unit) {
    val g = model.casino.solitaire
    val revision = model.revision
    val audio=LocalCasinoAudio.current
    val targets = remember(g) { mutableMapOf<Int, Rect>() }
    val drag = remember(g) { SolitaireDragState() }
    var selected by remember(g) { mutableStateOf<SolitairePick?>(null) }
    var hint by remember(g) { mutableStateOf<Pair<SolitairePick, Int>?>(null) }
    var note by remember(g) { mutableStateOf("") }
    var newGame by remember { mutableStateOf(false) }
    var drawChoice by remember { mutableIntStateOf(g.drawCount) }
    var help by remember { mutableStateOf(false) }
    var finishing by remember(g) { mutableStateOf(false) }
    var progress by remember(g) { mutableStateOf(SolitaireProgress.AVAILABLE) }
    var noMoves by remember(g) { mutableStateOf(false) }
    var warned by remember(g) { mutableStateOf(false) }
    val stuck=progress==SolitaireProgress.BLOCKED&&!g.won
    val celebration=remember(g) { Animatable(if(g.won)1f else 0f) }
    var hadWon by remember(g) { mutableStateOf(g.won) }
    LaunchedEffect(g,revision) {
        val snapshot=g.snapshot()
        progress=withContext(Dispatchers.Default) { solitaireProgress(snapshot,g.drawCount) }
        if(progress==SolitaireProgress.BLOCKED&&g.moves>0&&!warned) {
            warned=true;noMoves=true;audio?.play(CasinoSound.LOSE)
        } else if(progress==SolitaireProgress.AVAILABLE)warned=false
    }
    LaunchedEffect(g,g.won) {
        if(g.won&&!hadWon) {
            hadWon=true
            audio?.play(CasinoSound.WIN)
            celebration.snapTo(0f)
            celebration.animateTo(1f,tween(1900,easing=LinearEasing))
        }
    }
    fun reward() { if (g.won && !g.rewarded) { g.rewarded = true; model.game.bankroll += 100 } }
    fun move(pick: SolitairePick, dest: Int) {
        if (finishing) return
        model.change {
            if (g.move(pick, dest)) { audio?.play(CasinoSound.CARD);selected = null; hint = null; note = ""; reward() }
            else {audio?.play(CasinoSound.INVALID,.3f);note = "Try another pile"}
        }
    }
    fun select(p: SolitairePick) {
        if (finishing) return
        audio?.play(CasinoSound.TAP,.55f)
        val previous = selected
        if (previous != null && previous.pile != p.pile && g.legal(previous, p.pile)) { move(previous, p.pile); return }
        val dest = ((7..10).toList() + (0..6).toList()).firstOrNull {
            g.legal(p, it) && !(it < 7 && g.columns[it].isEmpty() && p.pile in 0..6 && p.index == 0)
        }
        if (dest != null) move(p, dest) else { selected = if (selected == p) null else p; hint = null }
    }
    fun draw() {
        if (!finishing) { model.change { if(g.draw())audio?.play(CasinoSound.CARD) }; selected = null; hint = null; note = "" }
    }
    LaunchedEffect(finishing) {
        if (finishing) {
            while (!g.won) {
                val next = g.nextFoundationMove() ?: break
                model.change { g.move(next.first, next.second); reward() };audio?.play(CasinoSound.CARD,.3f)
                delay(100)
            }
            finishing = false
        }
    }
    TableRoom(model, "SOLITAIRE", onBack, { help = true }) {
        // Observe mutations inside this composable content lambda, including controls.
        val screenRevision = model.revision
        Row(Modifier.fillMaxWidth().height(26.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("DRAW ${g.drawCount}", color = model.room.accent, fontSize = 11.sp)
            Text(when { g.won -> "+100 · COMPLETE"; stuck -> "NO MOVES · UNDO OR NEW"; else -> note }, color = if (stuck) Color(0xFFFFBCAC) else Color.White,
                fontSize = 11.sp, modifier = if (stuck) Modifier.testTag("solitaire-stuck") else Modifier)
            Text("${g.moves} MOVES", color = Color.White.copy(alpha = .7f), fontSize = 11.sp)
        }
        BoxWithConstraints(Modifier.fillMaxWidth().weight(1f).testTag("solitaire-table")) {
            // This board is subcomposed: observe the saved-state revision here as well.
            val board = remember(g, model.revision) { g.snapshot() }
            TableFelt(Modifier.fillMaxSize())
            val gap = 4.dp
            val inset = 10.dp
            val cardW = minOf(64.dp,(maxWidth - inset * 2 - gap * 6) / 7,(maxHeight - 40.dp) / 3.4f)
            val cardH = cardW * 1.42f
            val boardWidth = cardW * 7 + gap * 6
            val left = (maxWidth - boardWidth) / 2
            val top = 14.dp
            val tableauY = top + cardH + 18.dp
            val available = maxHeight - tableauY - 12.dp
            fun outlined(p: SolitairePick) = p == selected || p == hint?.first
            fun border(on: Boolean) = if (on) Modifier.border(2.dp, model.room.accent, RoundedCornerShape(5.dp)) else Modifier
            Box(Modifier.offset(left+(cardW+gap)*6, top).size(cardW, cardH).testTag("sol-stock")
                .semantics { contentDescription = if (board.stock.isEmpty()) "Recycle stock" else "Draw ${g.drawCount}; ${board.stock.size} cards left" }
                .clickable(enabled = !finishing && !g.won) { draw() }) {
                if (board.stock.isNotEmpty()) CardView("?", cardW, cardH) else SolitaireSlot("↻", cardW, cardH)
                if (board.stock.isNotEmpty()) Text("${board.stock.size}", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold,
                    modifier = Modifier.align(Alignment.BottomCenter).background(Color.Black.copy(alpha = .8f), RoundedCornerShape(3.dp)).padding(horizontal = 5.dp))
            }
            val fanCount = if (g.drawCount == 3) minOf(3, board.waste.size) else minOf(1, board.waste.size)
            if (fanCount == 0) Box(Modifier.offset(left + (cardW+gap)*5, top)) { SolitaireSlot("", cardW, cardH) }
            board.waste.takeLast(fanCount).forEachIndexed { i, card ->
                val p = SolitairePick(-1, board.waste.size - fanCount + i)
                val topCard = i == fanCount - 1
                Box(Modifier.offset(left + (cardW+gap)*5 - cardW * (.40f * (fanCount-1-i)), top)
                    .zIndex(if (drag.pick?.pile == -1) 40f else i.toFloat())
                    .testTag("sol-waste-$i")
                    .solitaireDrag(drag, topCard && !finishing, p, targets) { move(p, it) }
                    .then(border(outlined(p))).clickable(enabled = topCard && !finishing) { select(p) }) {
                    CardView(card.toString(), cardW, cardH)
                }
            }
            repeat(4) { f ->
                val p = SolitairePick(f + 7, board.foundations[f].lastIndex)
                Box(Modifier.offset(left + (cardW + gap) * f, top).size(cardW, cardH)
                    .zIndex(if (drag.pick?.pile == f + 7) 40f else 0f).testTag("sol-foundation-$f")
                    .onGloballyPositioned { targets[f + 7] = it.boundsInRoot() }
                    .solitaireDrag(drag, board.foundations[f].isNotEmpty() && !finishing, p, targets) { move(p, it) }
                    .then(border(outlined(p) || hint?.second == f + 7)).clickable(enabled = !finishing) {
                        audio?.play(CasinoSound.TAP,.55f)
                        selected?.let { move(it, f + 7) } ?: run { if (board.foundations[f].isNotEmpty()) selected = p }
                    }) {
                    if(g.won) SolitaireSlot("A",cardW,cardH)
                    else board.foundations[f].lastOrNull()?.let { CardView(it.toString(), cardW, cardH) } ?: SolitaireSlot("A", cardW, cardH)
                }
            }
            repeat(7) { c ->
                val pile = board.columns[c]
                val down = board.hidden[c]
                // Reflow every column from its actual card count, keeping its bottom card in view.
                val downIdeal = minOf(11.dp, cardH * .12f)
                val upIdeal = minOf(29.dp, cardH * .30f)
                val desired = downIdeal * down + upIdeal * maxOf(0, pile.size - down - 1)
                val compress = if (desired.value > 0) ((available - cardH).value / desired.value).coerceIn(.18f, 1f) else 1f
                val downStep = downIdeal * compress
                val upStep = upIdeal * compress
                Box(Modifier.offset(left + (cardW + gap) * c, tableauY).width(cardW).height(available)
                    .zIndex(if (drag.pick?.pile == c) 50f else 0f).testTag("sol-column-$c")
                    .onGloballyPositioned { targets[c] = it.boundsInRoot() }
                    .then(border(hint?.second == c))) {
                    if (pile.isEmpty()) Box(Modifier.clickable(enabled = !finishing) { selected?.let { move(it, c) } }) { SolitaireSlot("K", cardW, cardH) }
                    pile.forEachIndexed { i, card ->
                        val p = SolitairePick(c, i)
                        val y = downStep * minOf(i, down) + upStep * maxOf(0, i - down)
                        Box(Modifier.offset(y = y).testTag("sol-card-$c-$i")
                            .solitaireDrag(drag, i >= down && !finishing, p, targets) { move(p, it) }
                            .then(border(outlined(p))).clickable(enabled = i >= down && !finishing) { select(p) }) {
                            CardView(if (i < down) "?" else card.toString(), cardW, cardH)
                        }
                    }
                }
            }
            if(g.won) {
                Box(Modifier.fillMaxSize().testTag("sol-win-stack").semantics {
                    contentDescription="All 52 cards gathering into one winning stack"
                    stateDescription=if(celebration.value<1f)"Collecting" else "Stacked"
                }) {
                    board.foundations.forEachIndexed { f,cards -> cards.forEachIndexed { n,card ->
                        val index=n*4+f
                        val t=FastOutSlowInEasing.transform(((celebration.value-index*.005f)/.72f).coerceIn(0f,1f))
                        val startX=left+(cardW+gap)*f
                        val endX=(maxWidth-cardW)/2+0.6.dp*(index%3)
                        val endY=maxHeight*.43f+0.5.dp*(index%6)
                        Box(Modifier.offset(startX+(endX-startX)*t,top+(endY-top)*t-cardH*(sin(t*PI).toFloat()*.65f))
                            .graphicsLayer { rotationZ=(1f-t)*(f-1.5f)*12f }) {
                            CardView(if(t>.85f)"?" else card.toString(),cardW,cardH)
                        }
                    } }
                    Text("YOU WIN!",color=model.room.accent,fontWeight=FontWeight.Black,fontSize=25.sp,
                        modifier=Modifier.align(Alignment.Center).offset(y=cardH+32.dp))
                }
            }
        }
        Row(Modifier.fillMaxWidth().height(52.dp), horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = { model.change { g.undo() };audio?.play(CasinoSound.CARD); selected = null; hint = null; note = "" }, enabled = g.canUndo && !finishing, modifier = Modifier.weight(1f).testTag("sol-undo"), contentPadding = PaddingValues(0.dp)) { Text("↶ UNDO", fontSize = 11.sp) }
            TextButton(onClick = {
                audio?.play(CasinoSound.CLICK)
                hint = g.hint(); selected = hint?.first
                note = if (hint == null && !stuck) { if (g.stock.isEmpty()) "TAP ↻ TO RECYCLE" else "TAP THE STOCK" } else ""
            }, enabled = !g.won && !finishing, modifier = Modifier.weight(1f).testTag("sol-hint"), contentPadding = PaddingValues(0.dp)) { Text("◇ HINT", fontSize = 11.sp) }
            TextButton(onClick = { audio?.play(CasinoSound.CLICK);drawChoice = g.drawCount; newGame = true }, enabled = !finishing, modifier = Modifier.weight(1f).testTag("sol-new"), contentPadding = PaddingValues(0.dp)) { Text("+ NEW", fontSize = 11.sp) }
            if (g.canAutoFinish && !g.won) Button(onClick = { finishing = true }, enabled = !finishing, modifier = Modifier.weight(1.2f).testTag("sol-finish"), contentPadding = PaddingValues(0.dp)) { Text("FINISH", fontSize = 11.sp) }
            else Button(onClick = { draw() },enabled=!finishing&&!g.won,modifier=Modifier.weight(1.2f).testTag("sol-draw"),contentPadding=PaddingValues(0.dp)) { Text(if(g.stock.isEmpty())"RECYCLE" else "DRAW",fontSize=11.sp) }
        }
    }
    if(noMoves)AlertDialog(onDismissRequest={noMoves=false},modifier=Modifier.testTag("sol-no-moves-dialog"),
        title={Text("No moves left")},text={Text("No remaining move can reveal a card or advance this deal. Undo a move or start a new deal.")},
        confirmButton={TextButton(onClick={noMoves=false;drawChoice=g.drawCount;newGame=true}){Text("NEW DEAL")}},
        dismissButton={TextButton(onClick={noMoves=false;if(g.canUndo){model.change{g.undo()};audio?.play(CasinoSound.CARD)}}){Text(if(g.canUndo)"UNDO" else "CLOSE")}})
    if (newGame) AlertDialog(onDismissRequest = { newGame = false }, title = { Text("New deal?") }, text = {
        Column {
            Text("Replace this layout?")
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(1, 3).forEach { n -> FilterChip(selected = n == drawChoice, onClick = { drawChoice = n }, label = { Text("DRAW $n") }) }
            }
        }
    }, confirmButton = { TextButton(onClick = { model.change { model.casino.solitaire = SolitaireGame(drawChoice) };audio?.play(CasinoSound.SHUFFLE); newGame = false }) { Text("DEAL NEW") } },
        dismissButton = { TextButton(onClick = { newGame = false }) { Text("CANCEL") } })
    if (help) AlertDialog(onDismissRequest = { help = false }, title = { Text("Klondike") }, text = {
        Text("Tap a card for a legal move, or drag a card and every card below it to another pile.\n\nBuild down in alternating colours. Empty columns take kings. The four top piles build up from ace to king by suit. Use DRAW at the bottom right, or tap the stock at the top right. RECYCLE returns the waste to the stock.\n\nUndo reverses a move. Hint lights the source and destination. New lets you choose draw-one or draw-three. Finish appears when every remaining card is exposed. A completed deal awards 100 virtual chips once.")
    }, confirmButton = { TextButton(onClick = { help = false }) { Text("CLOSE") } })
}

@Composable
private fun SolitaireSlot(text: String, width: Dp, height: Dp) {
    Box(Modifier.size(width, height).border(1.dp, Color.White.copy(alpha = .22f), RoundedCornerShape(5.dp)).background(Color.Black.copy(alpha = .10f), RoundedCornerShape(5.dp)), contentAlignment = Alignment.Center) {
        Text(text, color = Color.White.copy(alpha = .30f), fontSize = 24.sp)
    }
}

private class SolitaireDragState {
    var pick by mutableStateOf<SolitairePick?>(null)
    var shift by mutableStateOf(Offset.Zero)
}

@Composable
private fun Modifier.solitaireDrag(state: SolitaireDragState, enabled: Boolean, pick: SolitairePick,
    targets: Map<Int, Rect>, onDrop: (Int) -> Unit): Modifier {
    var origin by remember { mutableStateOf(Offset.Zero) }
    var dragOrigin by remember { mutableStateOf(Offset.Zero) }
    val drop by rememberUpdatedState(onDrop)
    val audio=LocalCasinoAudio.current
    val follows = state.pick?.let { it.pile == pick.pile && pick.index >= it.index } == true
    return onGloballyPositioned { origin = it.boundsInRoot().topLeft }
        .zIndex(if (follows) 30f else 0f)
        .graphicsLayer { translationX = if (follows) state.shift.x else 0f; translationY = if (follows) state.shift.y else 0f; shadowElevation = if (follows) 12.dp.toPx() else 0f }
        .pointerInput(enabled, pick) { if (enabled) detectDragGestures(
            onDragStart = { audio?.play(CasinoSound.TAP);dragOrigin = origin; state.pick = pick; state.shift = Offset.Zero },
            onDragEnd = {
                val point = dragOrigin + state.shift + Offset(size.width / 2f, size.height / 2f)
                targets.entries.firstOrNull { it.key != pick.pile && it.value.contains(point) }?.let { drop(it.key) }
                state.shift = Offset.Zero; state.pick = null
            },
            onDragCancel = { state.shift = Offset.Zero; state.pick = null },
            onDrag = { change, amount -> change.consume(); state.shift += amount }) }
}
