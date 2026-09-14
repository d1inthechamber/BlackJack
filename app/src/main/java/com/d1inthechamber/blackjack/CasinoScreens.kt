package com.d1inthechamber.blackjack

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics

@Composable
fun GameRoot(model:BlackjackViewModel) {
    val revision=model.revision
    var screen by rememberSaveable { mutableStateOf("LOBBY") }
    var confirmNew by remember {mutableStateOf(false)}
    var beforeSettings by rememberSaveable {mutableStateOf("LOBBY")}
    var foreground by remember {mutableStateOf(true)}
    val owner=LocalLifecycleOwner.current
    DisposableEffect(owner){val observer=LifecycleEventObserver{_,event->if(event==Lifecycle.Event.ON_STOP)foreground=false else if(event==Lifecycle.Event.ON_START)foreground=true};owner.lifecycle.addObserver(observer);onDispose{owner.lifecycle.removeObserver(observer)}}
    CompositionLocalProvider(LocalRoomStyle provides model.room, LocalCasinoSettings provides model.settings) {
        MaterialTheme(colorScheme=darkColorScheme(primary=model.room.accent,secondary=model.room.button)) {
            val lobby={model.save();screen="LOBBY"}
            val enter:(String)->Unit={model.enter(it);screen=it}
            BackHandler(screen!="LOBBY"){if(screen=="SETTINGS")screen=beforeSettings else lobby()}
            Box(Modifier.fillMaxSize().background(Color(0xFF100D14))) {
            Box(Modifier.fillMaxSize().padding(top=48.dp)) {
            if(!foreground)Box(Modifier.fillMaxSize().background(Color.Black)) else when(screen){
                "SETTINGS"->SettingsPage(model){screen=beforeSettings}
                "BLACKJACK"->BlackjackApp(model.game,lobby,{model.refill()})
                "SLOTS"->SlotsScreen(model,lobby)
                "SOLITAIRE"->SolitaireScreen(model,lobby)
                "POKER"->PokerScreen(model,lobby)
                else->CasinoFrame(model,"THE CASINO FLOOR",null){
                    Text("CASINO CHAOS",fontSize=32.sp,fontWeight=FontWeight.Black,color=model.room.accent,letterSpacing=2.sp,textAlign=TextAlign.Center,modifier=Modifier.fillMaxWidth().testTag("casino-title"))
                    Text("A little luck. A lot of bad company.",color=Color.LightGray,fontSize=13.sp,modifier=Modifier.fillMaxWidth(),textAlign=TextAlign.Center)
                    Spacer(Modifier.height(12.dp))
                    Button(enabled=model.hasSaved,onClick={enter(model.casino.lastGame)},modifier=Modifier.fillMaxWidth()){Text("CONTINUE")}
                    val locked=model.game.inRound||model.game.shuffling
                    if(locked)Text("Your blackjack hand is waiting. Finish it before entering another game.",color=model.room.accent)
                    val entries=listOf(Triple("BLACKJACK","♠","Your original table • animated dealers"),Triple("SLOTS","7","Themed reels • fixed odds • instant chaos"),Triple("SOLITAIRE","♣","Classic solitaire • draw 1 or 3 • win 100 chips"),Triple("POKER","♦","Texas Hold’em • three distinct rivals"))
                    for((name,symbol,detail) in entries) {
                        Card(onClick={enter(name)},enabled=!locked||name=="BLACKJACK",colors=CardDefaults.cardColors(containerColor=Color(0xE516141C)),border=BorderStroke(1.dp,model.room.accent.copy(alpha=.5f)),modifier=Modifier.fillMaxWidth().padding(vertical=4.dp)){
                            Row(Modifier.padding(16.dp),verticalAlignment=Alignment.CenterVertically){Text(symbol,color=model.room.accent,fontSize=34.sp,modifier=Modifier.width(50.dp));Column(Modifier.weight(1f)){Text(name,fontWeight=FontWeight.Black,color=Color.White,fontSize=20.sp);Text(detail,color=Color.LightGray,fontSize=12.sp)}}
                        }
                    }
                    if(model.casino.poker.seated)Text("Poker table: ${model.casino.poker.seats[0].stack} chips held • return to poker to cash out",color=model.room.accent,fontSize=12.sp)
                    TextButton(onClick={if(model.hasSaved)confirmNew=true else {model.startNew();enter("BLACKJACK")}},modifier=Modifier.fillMaxWidth()){Text("START NEW GAME")}
                    Text("Virtual chips only • Free refills • No purchases",color=Color.LightGray,fontSize=12.sp)
                }
            }
            }
            RoomBorder(model.room,Modifier.safeDrawingPadding())
            OutlinedButton(onClick={if(screen!="SETTINGS"){beforeSettings=screen;model.save();screen="SETTINGS"}},modifier=Modifier.align(Alignment.TopEnd).safeDrawingPadding().padding(end=12.dp).height(46.dp).testTag("settings-button"),colors=ButtonDefaults.outlinedButtonColors(containerColor=Color(0xF019141D))) {Text("⚙ SETTINGS",fontSize=12.sp)}
            }
            if(confirmNew)AlertDialog(onDismissRequest={confirmNew=false},title={Text("Start a fresh casino?")},text={Text("This replaces all saved games and table chips with a new 1,000-chip bankroll.")},confirmButton={TextButton(onClick={model.startNew();confirmNew=false;enter("BLACKJACK")}){Text("START FRESH")}},dismissButton={TextButton(onClick={confirmNew=false}){Text("CANCEL")}})
        }
    }
}

@Composable
internal fun CasinoFrame(model:BlackjackViewModel,title:String,onBack:(()->Unit)?,content:@Composable ColumnScope.(Int)->Unit){
    RoomMusic(model.room,model.settings.music)
    Box(Modifier.fillMaxSize()){
        Image(painterResource(model.room.background),null,Modifier.fillMaxSize(),contentScale=ContentScale.Crop)
        Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha=.65f)))
        Column(Modifier.fillMaxSize().safeDrawingPadding().padding(horizontal=16.dp).verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(8.dp)){
            Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){
                if(onBack!=null)TextButton(onClick=onBack){Text(if(title=="SETTINGS")"BACK" else "LOBBY")}
                Text("${formatChips(model.game.bankroll)} CHIPS",Modifier.weight(1f),fontSize=15.sp,color=model.room.accent,fontWeight=FontWeight.Bold)

            }
            Text(model.room.title.uppercase(),color=model.room.accent,fontSize=11.sp,letterSpacing=3.sp)
            if(onBack!=null)Text(title,color=Color.White,fontSize=27.sp,fontWeight=FontWeight.Black)
            content(model.revision)
            if(model.game.bankroll<10&&!model.game.inRound&&!model.game.shuffling)Button(onClick={model.refill()},modifier=Modifier.fillMaxWidth()){Text("REFILL • 1,000 FREE CHIPS")}
            Spacer(Modifier.height(20.dp))
        }
    }
}
internal fun slotSymbols(room:RoomStyle):List<String> = when(room){
    RoomStyle.VEGAS->listOf("CHERRY","BAR","BELL","STAR","7")
    RoomStyle.CARNIVAL->listOf("TICKET","MOON","MASK","SKULL","JOKER")
    RoomStyle.EGYPT->listOf("ANKH","SCARAB","EYE","COBRA","PHARAOH")
    RoomStyle.IRON->listOf("BOLT","GEAR","COAL","STEAM","GOLD")
    RoomStyle.WEST->listOf("TAPE","VINYL","BOOMBOX","PALM","CROWN")
    RoomStyle.PUNK->listOf("PIN","BOOT","PICK","SKULL","RIOT")
    RoomStyle.GREEN->listOf("LEAF","AMBER","MOON","BLOOM","CHAMP")
}
@Composable
internal fun SlotsScreen(model:BlackjackViewModel,onBack:()->Unit){
    val game=model.casino.slots
    var spinning by remember{mutableStateOf(false)}
    var tick by remember{mutableIntStateOf(0)}
    var settled by remember{mutableIntStateOf(3)}
    var help by remember{mutableStateOf(false)}
    val symbols=slotSymbols(model.room)
    LaunchedEffect(spinning){if(spinning){repeat(24){tick++;settled=when{it<12->0;it<18->1;else->2};delay(65)};settled=3;spinning=false}}
    val pull:()->Unit={if(!spinning&&model.game.bankroll>=game.bet){model.change{model.game.bankroll-=game.bet;model.game.bankroll+=game.spin()};settled=0;spinning=true}}
    CasinoFrame(model,"CHAOS SLOTS",onBack){
        Text("${model.room.title} • ONE PAYLINE",color=model.room.accent)
        Row(Modifier.fillMaxWidth().padding(vertical=8.dp),verticalAlignment=Alignment.CenterVertically){
        Surface(color=Color(0xFF302019),shape=RoundedCornerShape(topStart=36.dp,topEnd=36.dp,bottomStart=12.dp,bottomEnd=12.dp),border=BorderStroke(4.dp,model.room.accent),modifier=Modifier.weight(1f)){
            Column(Modifier.background(Brush.verticalGradient(listOf(Color(0xFF463329),Color(0xFF100F15),Color(0xFF35251F)))).padding(10.dp),horizontalAlignment=Alignment.CenterHorizontally){
                Text("★ CASINO CHAOS ★",color=model.room.accent,fontSize=18.sp,fontWeight=FontWeight.Black)
                Text("ONE ARM BANDIT",color=Color(0xFFF0D7A0),fontSize=10.sp,letterSpacing=2.sp)
                Spacer(Modifier.height(14.dp))
                Row(Modifier.fillMaxWidth().border(4.dp,Color(0xFFB4AAA0),RoundedCornerShape(8.dp)).padding(6.dp),horizontalArrangement=Arrangement.spacedBy(4.dp)){
                    repeat(3){i->val n=if(spinning&&i>=settled)(tick+i*2)%5 else game.reels[i]
                        Box(Modifier.weight(1f).height(174.dp).testTag("slot-reel-$i").clipToBounds().background(Color(0xFFFFF0D7))){
                            Column(Modifier.fillMaxWidth().offset(y=if(spinning&&i>=settled)((tick%3)*8-8).dp else 0.dp),horizontalAlignment=Alignment.CenterHorizontally){
                                SlotGlyph((n+4)%5,model.room,Modifier.size(40.dp))
                                Box(Modifier.fillMaxWidth().height(84.dp).border(1.dp,Color(0xFFA93338)),contentAlignment=Alignment.Center){SlotGlyph(n,model.room,Modifier.size(58.dp))}
                                SlotGlyph((n+1)%5,model.room,Modifier.size(40.dp))
                            }
                            Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Black.copy(alpha=.55f),Color.Transparent,Color.Transparent,Color.Black.copy(alpha=.55f)))))
                        }
                    }
                }
                Text("◀  WIN LINE  ▶",color=model.room.accent,fontSize=12.sp)
                Spacer(Modifier.height(12.dp))
                Row(Modifier.fillMaxWidth().background(Color(0xFF090909),RoundedCornerShape(5.dp)).padding(8.dp),horizontalArrangement=Arrangement.SpaceBetween){
                    Text("BET ${game.bet}",color=Color(0xFFEDB958),fontSize=12.sp)
                    Text(if(spinning)"WIN —" else "WIN ${game.returned}",color=Color(0xFFEDB958),fontSize=12.sp)
                }
                Spacer(Modifier.height(12.dp))
                Box(Modifier.fillMaxWidth().height(38.dp).border(3.dp,Color(0xFF797575),RoundedCornerShape(9.dp)).background(Color(0xFF09090B)),contentAlignment=Alignment.Center){Text("COIN RETURN",color=Color.Gray,fontSize=9.sp)}
            }
        }
        BanditLever(spinning,!spinning&&model.game.bankroll>=game.bet,pull,Modifier.width(46.dp).height(290.dp))
        }
        Text(if(spinning)"Let them roll…" else game.message,color=if(game.returned>0)model.room.accent else Color.White,fontSize=18.sp,modifier=Modifier.fillMaxWidth(),textAlign=TextAlign.Center)
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)){listOf(10,25,50).forEach{n->OutlinedButton(onClick={model.change{game.bet=n}},enabled=!spinning,modifier=Modifier.weight(1f)){Text(if(n==game.bet)"● $n" else "$n")}}}
        Button(onClick=pull,enabled=!spinning&&model.game.bankroll>=game.bet,modifier=Modifier.fillMaxWidth().height(60.dp).testTag("slot-spin")){Text(if(spinning)"SPINNING…" else "SPIN • ${game.bet} CHIPS",fontSize=18.sp,fontWeight=FontWeight.Black)}
        TextButton(onClick={help=true}){Text("PAY TABLE & RULES")}
        Text("Spin ${game.spins} • Payout includes your stake",fontSize=12.sp,color=Color.LightGray)
    }
    if(help)AlertDialog(onDismissRequest={help=false},title={Text("One line. Fixed odds.")},text={Column{symbols.forEachIndexed{i,s->Text("3 × $s = ${SlotsGame.triples[i]}× stake")};Text("Exactly two ${symbols[0]} = 2× stake. Everything else = 0.\nIndependent 20-stop reels: symbol weights 6 / 5 / 4 / 3 / 2. Theoretical return: 91.925%. Every spin is independent; no guaranteed wins.")}},confirmButton={TextButton(onClick={help=false}){Text("GOT IT")}})
}
@Composable
private fun SlotGlyph(n:Int,room:RoomStyle,modifier:Modifier){
    // Bold room-specific emblems with a shared hand-inked cabinet treatment.
    val marks=when(room){RoomStyle.VEGAS->listOf("●","▰","♧","★","7");RoomStyle.CARNIVAL->listOf("▥","☾","◈","☠","♠");RoomStyle.EGYPT->listOf("☥","◆","◉","ϟ","♛");RoomStyle.IRON->listOf("ϟ","⚙","◆","≋","▰");RoomStyle.WEST->listOf("▣","◉","▤","♠","♛");RoomStyle.PUNK->listOf("ϟ","▟","▼","☠","★");RoomStyle.GREEN->listOf("♣","◆","☾","❋","♛")}
    Box(modifier,contentAlignment=Alignment.Center){Text(marks[n],fontSize=45.sp,fontWeight=FontWeight.Black,color=if(n==4)Color(0xFFC72C43) else Color(0xFF29202F))}
}

@Composable
internal fun SolitaireScreen(model:BlackjackViewModel,onBack:()->Unit){
    val g=model.casino.solitaire
    var selected by remember(g){mutableStateOf<SolitairePick?>(null)}
    var note by remember{mutableStateOf("Tap a card to move it. Tap a destination to place a selected stack.")}
    var newDraw by remember{mutableStateOf<Int?>(null)}
    fun move(dest:Int){val p=selected?:return;model.change{if(g.move(p,dest)){selected=null;note="Good move.";if(g.won&&!g.rewarded){g.rewarded=true;model.game.bankroll+=100;note="Complete! +100 chips"}}else note="That card cannot move there."}}
    fun select(p:SolitairePick){
        if(selected==p){selected=null;return}
        val dest=((7..10).toList()+(0..6).toList()).firstOrNull{it!=p.pile&&g.legal(p,it)}
        selected=p
        if(dest!=null)move(dest)
    }
    CasinoFrame(model,"SOLITAIRE",onBack){
        Text("CLASSIC • DRAW ${g.drawCount} • ${g.moves} MOVES",color=model.room.accent,fontSize=12.sp)
        Text(if(g.won)"YOU CLEARED THE TABLE • +100 CHIPS" else note,color=Color.White,fontSize=13.sp)
        Row(horizontalArrangement=Arrangement.spacedBy(6.dp)){
            OutlinedButton(onClick={model.change{g.draw()};selected=null},enabled=!g.won){Text(if(g.stock.isEmpty())"RECYCLE" else "DRAW (${g.stock.size})")}
            OutlinedButton(onClick={model.change{g.undo()};selected=null},enabled=g.canUndo){Text("UNDO")}
            TextButton(onClick={val h=g.hint();if(h!=null){selected=h.first;note="Try ${if(h.second>=7)"foundation ${h.second-6}" else "column ${h.second+1}"}."}else note=if(g.stock.isNotEmpty()||g.waste.isNotEmpty())"Try drawing or recycling the stock." else "No available moves. Try undo or a new deal."}){Text("HINT")}
        }
        BoxWithConstraints(Modifier.fillMaxWidth()){
        val cardWidth=(maxWidth-24.dp)/7
        val cardHeight=cardWidth*1.42f
        Column(Modifier.fillMaxWidth()){
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(4.dp)){
                Column(Modifier.width(cardWidth),horizontalAlignment=Alignment.CenterHorizontally){Text("STOCK",color=Color.LightGray,fontSize=10.sp);Box(Modifier.clickable{model.change{g.draw()};selected=null}){if(g.stock.isNotEmpty())CardView("?",cardWidth,cardHeight) else CardSlot("↻",cardWidth,cardHeight)}}
                Column(Modifier.width(cardWidth),horizontalAlignment=Alignment.CenterHorizontally){Text("WASTE",color=Color.LightGray,fontSize=10.sp);val p=SolitairePick(-1,g.waste.lastIndex);Box(Modifier.border(if(selected==p)3.dp else 0.dp,model.room.accent).clickable{if(g.waste.isNotEmpty())select(p)}){g.waste.lastOrNull()?.let{CardView(it.toString(),cardWidth,cardHeight)}?:CardSlot("—",cardWidth,cardHeight)}}
                Spacer(Modifier.width(cardWidth))
                repeat(4){f->Column(horizontalAlignment=Alignment.CenterHorizontally){Text("HOME ${f+1}",color=model.room.accent,fontSize=10.sp);Box(Modifier.clickable{if(selected!=null)move(f+7)else if(g.foundations[f].isNotEmpty())selected=SolitairePick(f+7,g.foundations[f].lastIndex)}){g.foundations[f].lastOrNull()?.let{CardView(it.toString(),cardWidth,cardHeight)}?:CardSlot("A",cardWidth,cardHeight)}}}
            }
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement=Arrangement.spacedBy(4.dp)){
                repeat(7){c->Column(Modifier.width(cardWidth)){
                    Text("${c+1}",color=model.room.accent,modifier=Modifier.fillMaxWidth(),textAlign=TextAlign.Center)
                    val count=g.columns[c].size
                    Box(Modifier.width(cardWidth).height((maxOf(0,count-1)*30+100).dp).testTag("sol-column-$c")){
                        if(count==0)Box(Modifier.clickable{move(c)}){CardSlot("K",cardWidth,cardHeight)}
                        g.columns[c].forEachIndexed{i,card->val p=SolitairePick(c,i)
                            Box(Modifier.offset(y=(i*30).dp).border(if(selected==p)3.dp else 0.dp,model.room.accent,RoundedCornerShape(12.dp)).clickable{
                                if(selected!=null&&selected!!.pile!=c)move(c)else if(i>=g.hidden[c])select(p)
                            }){CardView(if(i<g.hidden[c])"?" else card.toString(),cardWidth,cardHeight)}
                        }
                    }
                }}
            }
        }
        }
        Text("Build down in alternating colours; empty columns take kings. Foundations build A → K by suit. Unlimited stock recycling.",fontSize=12.sp,color=Color.LightGray)
        Row{TextButton(onClick={newDraw=1}){Text("NEW • DRAW 1")};TextButton(onClick={newDraw=3}){Text("NEW • DRAW 3")}}
    }
    if(newDraw!=null)AlertDialog(onDismissRequest={newDraw=null},title={Text("Deal new solitaire?")},text={Text("Replaces this solitaire layout. Your other games and chips stay saved.")},confirmButton={TextButton(onClick={model.change{model.casino.solitaire=SolitaireGame(newDraw!!)};selected=null;newDraw=null;note="New deal. Good luck."}){Text("DEAL NEW")}},dismissButton={TextButton(onClick={newDraw=null}){Text("CANCEL")}})
}
@Composable
private fun CardSlot(text:String,width:androidx.compose.ui.unit.Dp,height:androidx.compose.ui.unit.Dp=94.dp){Box(Modifier.size(width,height).border(1.dp,LocalRoomStyle.current.accent.copy(alpha=.5f),RoundedCornerShape(12.dp)).background(Color.Black.copy(alpha=.3f)),contentAlignment=Alignment.Center){Text(text,color=LocalRoomStyle.current.accent,fontSize=25.sp)}}

@Composable
internal fun PokerScreen(model:BlackjackViewModel,onBack:()->Unit){
    val g=model.casino.poker
    var rules by remember{mutableStateOf(false)}
    var raiseTarget by remember(g.hand,g.actor,g.currentBet){mutableIntStateOf(g.currentBet+g.minRaise)}
    LaunchedEffect(g,g.actor,g.active,model.revision){if(g.active&&g.actor>0){delay(1100);model.change{g.botStep()}}}
    CasinoFrame(model,"POKER",onBack){
        Text("NO-LIMIT TEXAS HOLD’EM • 5 / 10",color=model.room.accent,fontSize=12.sp)
        if(!g.seated){
            Text("Three rivals. Three ways to read the room.",fontSize=23.sp,color=Color.White)
            Text("Buy in with 100–500 virtual chips from your bankroll. Your stack stays at this table until you cash out between hands. Opponents have individual betting habits and imperfect tells.",color=Color.LightGray)
            Button(onClick={val amount=minOf(500,model.game.bankroll.toInt());if(amount>=100)model.change{g.sit(amount,model.room.ordinal);model.game.bankroll-=amount}},enabled=model.game.bankroll>=100,modifier=Modifier.fillMaxWidth().testTag("poker-buyin")){Text("TAKE A SEAT • ${minOf(500,model.game.bankroll.toInt())} CHIPS")}
            if(model.game.bankroll in 10.0..99.99)Text("This table needs at least 100 chips. Solitaire is free and awards 100 for a win.",color=model.room.accent)
        }else{
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(6.dp)){
                g.seats.drop(1).forEachIndexed{j,p->val i=j+1
                    Surface(Modifier.weight(1f).testTag("poker-opponent-$i"),color=Color(0xD91A1420),shape=RoundedCornerShape(16.dp),border=BorderStroke(if(g.actor==i)2.dp else 1.dp,if(g.actor==i)model.room.accent else Color.DarkGray)){
                        Column(Modifier.padding(7.dp),horizontalAlignment=Alignment.CenterHorizontally){
                            PokerPortrait(p,j,g.actor==i,Modifier.fillMaxWidth().height(120.dp))
                            Text(RoomStyle.entries[(model.room.ordinal+j)%RoomStyle.entries.size].host,color=model.room.accent,fontWeight=FontWeight.Black,fontSize=14.sp)
                            Text(p.personality.label,color=Color.LightGray,fontSize=10.sp)
                            Text("${p.stack} chips",color=Color.White,fontSize=12.sp)
                            Text(if(p.folded)"FOLDED" else p.tell,color=Color.LightGray,fontSize=10.sp,textAlign=TextAlign.Center,modifier=Modifier.heightIn(min=30.dp))
                            Text(p.lastAction,color=model.room.accent,fontSize=10.sp)
                            if(p.hole.isNotEmpty())Row(horizontalArrangement=Arrangement.spacedBy(2.dp)){p.hole.forEach{CardView(if(g.showdown&&!p.folded)it.toString() else "?",35.dp,51.dp)}}
                        }
                    }
                }
            }
            Surface(color=Color(0xD90A2620),shape=RoundedCornerShape(30.dp),border=BorderStroke(2.dp,model.room.accent),modifier=Modifier.fillMaxWidth()){
                Column(Modifier.padding(14.dp),horizontalAlignment=Alignment.CenterHorizontally){
                    Text("POT ${g.pot} • HAND ${g.hand}",color=model.room.accent,fontSize=18.sp,fontWeight=FontWeight.Black)
                    Row(Modifier.padding(vertical=12.dp),horizontalArrangement=Arrangement.spacedBy(5.dp)){repeat(5){i->g.board.getOrNull(i)?.let{CardView(it.toString(),49.dp,72.dp)}?:Box(Modifier.size(49.dp,72.dp).border(1.dp,Color.White.copy(alpha=.15f),RoundedCornerShape(8.dp)))}}
                    Text(g.message,color=Color.White,fontSize=13.sp,textAlign=TextAlign.Center)
                }
            }
            val p=g.seats[0]
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.Center,verticalAlignment=Alignment.CenterVertically){Column(Modifier.weight(1f)){Text("YOUR STACK ${p.stack}",color=model.room.accent,fontWeight=FontWeight.Black);Text("In this street: ${p.streetBet}",color=Color.LightGray,fontSize=12.sp);Text("Button: ${g.seats[g.button].name}",color=Color.LightGray,fontSize=12.sp)};p.hole.forEach{CardView(it.toString(),60.dp,86.dp);Spacer(Modifier.width(5.dp))}}
            if(g.active){
                val yourTurn=g.actor==0
                Text(if(yourTurn)"YOUR MOVE" else "${g.seats[g.actor].name} is thinking…",color=model.room.accent,fontWeight=FontWeight.Bold)
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)){
                    OutlinedButton(onClick={model.change{g.act(PokerAction.FOLD)}},enabled=yourTurn,modifier=Modifier.weight(1f)){Text("FOLD")}
                    Button(onClick={model.change{g.act(PokerAction.CALL)}},enabled=yourTurn,modifier=Modifier.weight(1f)){Text(if(g.toCall(0)==0)"CHECK" else "CALL ${minOf(g.toCall(0),p.stack)}")}
                }
                if(yourTurn&&g.canRaise(0)){
                    val cap=p.stack+p.streetBet;val minimum=minOf(cap,g.currentBet+g.minRaise)
                    val target=raiseTarget.coerceIn(minimum,cap)
                    if(cap>minimum)Slider(value=target.toFloat(),onValueChange={raiseTarget=it.toInt()},valueRange=minimum.toFloat()..cap.toFloat())
                    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)){
                        Button(onClick={model.change{g.act(PokerAction.RAISE,target)}},modifier=Modifier.weight(1f)){Text("RAISE TO $target")}
                        OutlinedButton(onClick={model.change{g.act(PokerAction.RAISE,cap)}},modifier=Modifier.weight(1f)){Text("ALL-IN")}
                    }
                }
                Text("You can visit the lobby; this hand pauses until you return.",fontSize=11.sp,color=Color.LightGray)
            }else{
                Button(onClick={model.change{g.startHand()}},enabled=p.stack>0,modifier=Modifier.fillMaxWidth().testTag("poker-deal")){Text(if(g.hand==0)"DEAL FIRST HAND" else "NEXT HAND")}
                OutlinedButton(onClick={model.change{model.game.bankroll+=g.cashOut()}},modifier=Modifier.fillMaxWidth()){Text("CASH OUT • ${p.stack} CHIPS")}
            }
            if(g.log.isNotEmpty())Text(g.log.takeLast(5).joinToString("\n"),color=Color.LightGray,fontSize=11.sp)
        }
        TextButton(onClick={rules=true}){Text("RULES & READING YOUR RIVALS")}
    }
    if(rules)AlertDialog(onDismissRequest={rules=false},title={Text("Read the player. Play the cards.")},text={Column(Modifier.verticalScroll(rememberScrollState())){Text("Best five cards from your two cards and the five community cards win. Blinds rotate, minimum raises follow the last full raise, and all-ins create side pots. Ties split the pot; odd chips go clockwise from the button. No rake.\n")
        PokerPersonality.entries.forEach{Text("${it.label}: ${when(it){PokerPersonality.BULLY->"Pressures the table with raises.";PokerPersonality.ROCK->"Patient and selective.";PokerPersonality.SHOWBOAT->"Loves a dramatic bluff.";PokerPersonality.GAMBLER->"Chases draws and takes chances.";PokerPersonality.VETERAN->"Mixes tactics and notices your aggression."}}")}
        Text("\nWatch expressions and gestures across several hands. They are clues, not guarantees. Rivals cannot see your cards or the actual future deck. Busted opponents re-buy 500 virtual chips between hands.")}},confirmButton={TextButton(onClick={rules=false}){Text("LET’S PLAY")}})
}

@Composable
private fun PokerPortrait(p:PokerSeat,index:Int,acting:Boolean,modifier:Modifier){
    val room=RoomStyle.entries[(LocalRoomStyle.current.ordinal+index)%RoomStyle.entries.size]
    val context=LocalContext.current
    val sheet by produceState<ImageBitmap?>(null,room){value=withContext(Dispatchers.Default){decodeDealer(context,room)}}
    var frame by remember { mutableIntStateOf(0) }
    LaunchedEffect(p.expression,acting){
        frame=when(p.expression){1->6;2->0;3->8;4->9;5->1;else->0}
        if(acting&&p.expression==3){repeat(4){frame=if(it%2==0)10 else 11;delay(240)};frame=8}
    }
    Canvas(modifier.clipToBounds().semantics{contentDescription=room.host}){
        // Use the very same keyed animation atlas as the blackjack dealer.
        sheet?.let{drawDealerPose(it,room,frame,-size.width*.16f,size.width*1.32f)}
        drawLine(room.accent,Offset(0f,size.height-2f),Offset(size.width,size.height-2f),4f)
    }
}

@Composable
private fun BanditLever(spinning:Boolean,enabled:Boolean,onPull:()->Unit,modifier:Modifier){
    var dragged by remember{mutableFloatStateOf(0f)}
    val pull by animateFloatAsState(if(spinning)1f else dragged,tween(180),label="lever")
    Canvas(modifier.semantics{contentDescription="Pull slot machine lever"}.clickable(enabled=enabled,onClick=onPull)
        .pointerInput(enabled){if(enabled)detectVerticalDragGestures(
            onDragEnd={if(dragged>.3f)onPull();dragged=0f},
            onDragCancel={dragged=0f},
            onVerticalDrag={change,amount->change.consume();dragged=(dragged+amount/size.height*.9f).coerceIn(0f,1f)})}){
        val pivot=Offset(size.width*.24f,size.height*.72f)
        val knob=Offset(size.width*.68f,size.height*(.12f+pull*.58f))
        drawCircle(Color(0xFF68636B),size.width*.23f,pivot)
        drawLine(Color(0xFF69666B),pivot,knob,size.width*.22f)
        drawLine(Color(0xFFDDD8CE),pivot-Offset(2f,0f),knob-Offset(2f,0f),size.width*.08f)
        drawCircle(Color(0xFF851E2B),size.width*.29f,knob)
        drawCircle(Color(0xFFE66562),size.width*.09f,knob-Offset(size.width*.08f,size.width*.09f))
    }
}
