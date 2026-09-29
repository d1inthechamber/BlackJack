package com.d1inthechamber.blackjack

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

@Composable
internal fun PokerScreen(model: BlackjackViewModel, onBack: () -> Unit) {
    val g = model.casino.poker
    val revision = model.revision
    var rules by remember { mutableStateOf(false) }
    var raiseTarget by remember(g.hand, g.actor, g.currentBet) { mutableIntStateOf(g.currentBet + g.minRaise) }
    LaunchedEffect(g, g.actor, g.active, revision) {
        if (g.active && g.actor > 0) { delay(1100); model.change { g.botStep() } }
    }
    HumanReactionVoice(g.hand, if (g.showdown && g.seats.isNotEmpty()) {
        if (g.seats[0].expression == 3) HumanReaction.FRUSTRATED else HumanReaction.CHEER
    } else null, model.settings.voices)

    TableRoom(model, "POKER", onBack, { rules = true }) {
        BoxWithConstraints(Modifier.weight(1f).fillMaxWidth()) {
            val landscape = maxWidth > maxHeight * 1.25f && maxWidth >= 600.dp
            val controls: @Composable ColumnScope.() -> Unit = {
                if (!g.seated) {
                    Text("TEXAS HOLD’EM · 5 / 10", color = model.room.accent, fontSize = 12.sp)
                    Button(onClick = {
                        val amount = minOf(500, model.game.bankroll.toInt())
                        if (amount >= 100) model.change { g.sit(amount, model.room.ordinal); model.game.bankroll -= amount }
                    }, enabled = model.game.bankroll >= 100,
                        modifier = Modifier.fillMaxWidth().height(50.dp).testTag("poker-buyin")) {
                        Text("TAKE A SEAT · $" + minOf(500, model.game.bankroll.toInt()))
                    }
                    if (model.game.bankroll < 100) Button(onClick = { model.refill() }, modifier = Modifier.fillMaxWidth()) { Text("REFILL") }
                } else {
                    val p = g.seats[0]
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                        ChipStack(p.stack, active = true)
                        Text(when { !g.active -> "BLINDS 5 / 10"; g.actor == 0 -> "YOUR TURN"; else -> "" }, color = model.room.accent, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                    if (g.active) {
                        val yourTurn = g.actor == 0
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            OutlinedButton(onClick = { model.change { g.act(PokerAction.FOLD) } }, enabled = yourTurn, modifier = Modifier.weight(1f)) { Text("FOLD") }
                            Button(onClick = { model.change { g.act(PokerAction.CALL) } }, enabled = yourTurn, modifier = Modifier.weight(1f)) { Text(if (g.toCall(0) == 0) "CHECK" else "CALL $" + minOf(g.toCall(0), p.stack)) }
                        }
                        if (yourTurn && g.canRaise(0)) {
                            val cap = p.stack + p.streetBet
                            val minimum = minOf(cap, g.currentBet + g.minRaise)
                            val target = raiseTarget.coerceIn(minimum, cap)
                            if (cap > minimum) Slider(value = target.toFloat(), onValueChange = { raiseTarget = it.toInt() }, valueRange = minimum.toFloat()..cap.toFloat(), modifier = Modifier.height(32.dp))
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Button(onClick = { model.change { g.act(PokerAction.RAISE, target) } }, modifier = Modifier.weight(1f)) { Text("RAISE $" + target) }
                                OutlinedButton(onClick = { model.change { g.act(PokerAction.RAISE, cap) } }, modifier = Modifier.weight(1f)) { Text("ALL-IN") }
                            }
                        }
                    } else {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Button(onClick = { model.change { g.startHand() } }, enabled = p.stack > 0, modifier = Modifier.weight(1f).testTag("poker-deal")) { Text(if (g.hand == 0) "DEAL" else "NEXT HAND") }
                            OutlinedButton(onClick = { model.change { model.game.bankroll += g.cashOut() } }, modifier = Modifier.weight(1f).testTag("poker-cashout"), contentPadding = PaddingValues(horizontal = 6.dp)) { Text("CASH OUT", maxLines = 1, fontSize = 12.sp) }
                        }
                    }
                }
            }
            if (landscape) Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                PokerTable(model, Modifier.weight(.67f).fillMaxHeight())
                Column(Modifier.weight(.33f).fillMaxHeight().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterVertically), content = controls)
            } else Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                PokerTable(model, Modifier.fillMaxWidth().weight(1f))
                Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(2.dp), content = controls)
            }
        }
    }
    if (rules) AlertDialog(onDismissRequest = { rules = false }, title = { Text("Texas Hold’em") }, text = {
        Text("Make the best five-card hand from your two cards and the board. Blinds are 5 / 10. The D marker moves each hand.\n\nCheck or call, fold, or use the slider to raise. All-ins create side pots; tied hands split the pot. Cash out between hands.\n\nThe glow marks the acting seat. Watch the characters’ reactions. Virtual chips only.")
    }, confirmButton = { TextButton(onClick = { rules = false }) { Text("CLOSE") } })
}

@Composable
private fun PokerTable(model: BlackjackViewModel, modifier: Modifier) {
    val revision = model.revision
    val g = model.casino.poker
    // The same approved cast is seated behind the rail, without portrait boxes.
    val preview = remember(model.room) { PokerGame().apply { sit(500, model.room.ordinal) } }
    val seats = if (g.seated) g.seats else preview.seats
    BoxWithConstraints(modifier.testTag("poker-table")) {
        val tableRevision = model.revision
        val w = maxWidth
        val h = maxHeight
        val compact = h < 340.dp
        val cardW = minOf(w / 7.4f, h * if (compact) .115f else .15f).coerceAtLeast(24.dp)
        val cardH = cardW * 1.40f
        val actorW = w * .32f
        // Torso bases sit at the back rail; the rail occludes their lower edge.
        seats.drop(1).forEachIndexed { j, p ->
            val x = w * (.17f + j * .33f) - actorW / 2
            val y = if (j == 1) 0.dp else h * .035f
            PokerPortrait(if (g.showdown) p.copy(expression = if (p.expression == 3) 4 else 3) else p, j, g.active && g.actor == j + 1,
                Modifier.offset(x, y).size(actorW, h * if (compact) .27f else .34f).testTag("poker-opponent-${j+1}"))
        }
        TableFelt(Modifier.offset(y = h * if (compact) .21f else .27f).fillMaxWidth().height(h * if (compact) .78f else .72f), oval = true)
        seats.drop(1).forEachIndexed { j, p ->
            val acting = g.active && g.actor == j + 1
            val x = w * (.17f + j * .33f) - actorW / 2
            Column(Modifier.offset(x, h * if (compact) .23f else .30f).width(actorW), horizontalAlignment = Alignment.CenterHorizontally) {
                Row(horizontalArrangement = Arrangement.spacedBy(3.dp), modifier = Modifier.graphicsLayer { alpha = if (p.folded && g.hand > 0) .38f else 1f }) {
                    repeat(2) { i ->
                        val face = p.hole.getOrNull(i)?.let { if (g.showdown && !p.folded) it.toString() else "?" } ?: "?"
                        TableCard(face, cardW * .64f, cardH * .64f, if (i == 0) -8f else 8f)
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    ChipStack(p.stack, active = acting)
                    if (g.seated && g.button == j + 1) DealerButton()
                }
                Text(if (p.folded) "FOLD" else p.lastAction.take(18), color = if (acting) model.room.accent else Color.White.copy(alpha = .72f), fontSize = 10.sp,
                    textAlign = TextAlign.Center, maxLines = 1, modifier = Modifier.testTag("poker-reaction-${j+1}"))
                if (acting) Box(Modifier.width(44.dp).height(2.dp).background(model.room.accent))
            }
        }
        Column(Modifier.align(Alignment.TopCenter).offset(y = h * if (compact) .52f else .55f), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("POT $" + g.pot, color = model.room.accent, fontSize = 15.sp, fontWeight = FontWeight.Black, modifier = Modifier.testTag("poker-pot"))
            Spacer(Modifier.height(5.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                repeat(5) { i ->
                    g.board.getOrNull(i)?.let { TableCard(it.toString(), cardW, cardH, (i - 2) * .8f) }
                        ?: Box(Modifier.size(cardW, cardH).border(1.dp, Color.White.copy(alpha = .12f), RoundedCornerShape(5.dp)))
                }
            }
        }
        if (g.seated) {
            val p = g.seats[0]
            Row(Modifier.align(Alignment.BottomCenter).padding(bottom = 12.dp), horizontalArrangement = Arrangement.spacedBy(7.dp), verticalAlignment = Alignment.CenterVertically) {
                if (g.button == 0) DealerButton()
                p.hole.forEachIndexed { i, card -> TableCard(card.toString(), cardW * 1.06f, cardH * 1.06f, if (i == 0) -5f else 5f) }
                if (p.streetBet > 0) ChipStack(p.streetBet)
            }
            if (!g.active && g.hand > 0) {
                val won = if (g.showdown) p.expression == 3 else g.message.startsWith("${p.name} wins ")
                val result = if (won) "HAND WON" else "HAND LOST"
                Text(result, color = model.room.accent, fontWeight = FontWeight.Black, fontSize = 12.sp,
                    modifier = Modifier.align(Alignment.BottomEnd).padding(14.dp).background(Color.Black.copy(alpha = .8f), RoundedCornerShape(5.dp)).padding(5.dp))
            }
        }
    }
}

@Composable
private fun DealerButton() {
    Box(Modifier.padding(start = 5.dp).size(19.dp).background(Color(0xFFEDE6D2), RoundedCornerShape(50)), contentAlignment = Alignment.Center) {
        Text("D", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Black)
    }
}
