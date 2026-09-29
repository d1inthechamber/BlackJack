package com.d1inthechamber.blackjack

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
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
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.zIndex
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.semantics
import kotlin.math.abs
import kotlin.math.floor

@Composable
fun GameRoot(model:BlackjackViewModel) {
    val revision=model.revision
    var screen by rememberSaveable { mutableStateOf("LOBBY") }
    var confirmNew by remember {mutableStateOf(false)}
    var beforeUtility by rememberSaveable {mutableStateOf("LOBBY")}
    var foreground by remember {mutableStateOf(true)}
    val owner=LocalLifecycleOwner.current
    DisposableEffect(owner){val observer=LifecycleEventObserver{_,event->if(event==Lifecycle.Event.ON_STOP)foreground=false else if(event==Lifecycle.Event.ON_START)foreground=true};owner.lifecycle.addObserver(observer);onDispose{owner.lifecycle.removeObserver(observer)}}
    CompositionLocalProvider(LocalRoomStyle provides model.room, LocalCasinoSettings provides model.settings) {
        MaterialTheme(colorScheme=darkColorScheme(primary=model.room.accent,secondary=model.room.button)) {
            val lobby={model.save();screen="LOBBY"}
            val enter:(String)->Unit={model.enter(it);screen=it}
            BackHandler(screen!="LOBBY"){if(screen=="SETTINGS"||screen=="ROOMS")screen=beforeUtility else lobby()}
            Box(Modifier.fillMaxSize().background(Color(0xFF100D14)).safeDrawingPadding()) {
            Box(Modifier.fillMaxSize().padding(top=58.dp)) {
            if(!foreground)Box(Modifier.fillMaxSize().background(Color.Black)) else AnimatedContent(
                targetState=screen,
                transitionSpec={fadeIn(tween(220)) togetherWith fadeOut(tween(160))},
                label="casino-screen-transition"
            ){destination->when(destination){
                "SETTINGS"->SettingsPage(model){screen=beforeUtility}
                "ROOMS"->RoomsPage(model){screen=beforeUtility}
                "BLACKJACK"->BlackjackApp(model.game,lobby,{model.refill()})
                "SLOTS"->SlotsScreen(model,lobby)
                "SOLITAIRE"->SolitaireScreen(model,lobby)
                "POKER"->PokerScreen(model,lobby)
                "CRAPS"->CrapsScreen(model,lobby)
                else->CasinoFrame(model,"THE CASINO FLOOR",null){
                    Text("CASINO CHAOS",fontSize=32.sp,fontWeight=FontWeight.Black,color=model.room.accent,letterSpacing=2.sp,textAlign=TextAlign.Center,modifier=Modifier.fillMaxWidth().testTag("casino-title"))

                    Spacer(Modifier.height(12.dp))
                    Button(enabled=model.hasSaved,onClick={enter(model.casino.lastGame)},modifier=Modifier.fillMaxWidth()){Text("CONTINUE")}
                    val locked=model.game.inRound||model.game.shuffling
                    if(locked)Text("Your blackjack hand is waiting. Finish it before entering another game.",color=model.room.accent)
                    val entries=listOf(Triple("BLACKJACK","♠","Your original table • animated dealers"),Triple("SLOTS","7","Themed reels • fixed odds • instant chaos"),Triple("SOLITAIRE","♣","Classic solitaire • drag full stacks"),Triple("POKER","♦","Texas Hold’em • three original rivals"),Triple("CRAPS","⚄","Street dice • real bills • character showdown"))
                    for((name,symbol,detail) in entries) {
                        Card(onClick={enter(name)},enabled=!locked||name=="BLACKJACK",colors=CardDefaults.cardColors(containerColor=Color(0xE516141C)),border=BorderStroke(1.dp,model.room.accent.copy(alpha=.5f)),modifier=Modifier.fillMaxWidth().padding(vertical=4.dp)){
                            Row(Modifier.padding(16.dp),verticalAlignment=Alignment.CenterVertically){Text(symbol,color=model.room.accent,fontSize=34.sp,modifier=Modifier.width(50.dp));Column(Modifier.weight(1f)){Text(name,fontWeight=FontWeight.Black,color=Color.White,fontSize=20.sp)}}
                        }
                    }
                    if(model.casino.poker.seated)Text("Poker table: ${model.casino.poker.seats[0].stack} chips held • return to poker to cash out",color=model.room.accent,fontSize=12.sp)
                    TextButton(onClick={if(model.hasSaved)confirmNew=true else {model.startNew();enter("BLACKJACK")}},modifier=Modifier.fillMaxWidth()){Text("START NEW GAME")}
                    Text("Virtual chips only • Free refills • No purchases",color=Color.LightGray,fontSize=12.sp)
                }
            }}
            }
            RoomBorder(model.room)
            CasinoNavigation(
                screen=screen,
                onGames={model.save();screen="LOBBY"},
                onRooms={if(screen!="ROOMS"){beforeUtility=if(screen=="SETTINGS")"LOBBY" else screen;model.save();screen="ROOMS"}},
                onSettings={if(screen!="SETTINGS"){beforeUtility=if(screen=="ROOMS")"LOBBY" else screen;model.save();screen="SETTINGS"}},
                modifier=Modifier.align(Alignment.TopCenter).padding(horizontal=12.dp)
            )
            }
            if(confirmNew)AlertDialog(onDismissRequest={confirmNew=false},title={Text("Start a fresh casino?")},text={Text("This replaces all saved games and table chips with a new 1,000-chip bankroll.")},confirmButton={TextButton(onClick={model.startNew();confirmNew=false;enter("BLACKJACK")}){Text("START FRESH")}},dismissButton={TextButton(onClick={confirmNew=false}){Text("CANCEL")}})
        }
    }
}

@Composable
private fun CasinoNavigation(screen:String,onGames:()->Unit,onRooms:()->Unit,onSettings:()->Unit,modifier:Modifier=Modifier){
    Surface(modifier.fillMaxWidth().height(50.dp),color=Color(0xF019141D),shape=RoundedCornerShape(15.dp),border=BorderStroke(1.dp,LocalRoomStyle.current.accent.copy(alpha=.65f))){
        Row(Modifier.fillMaxSize().padding(4.dp),horizontalArrangement=Arrangement.spacedBy(4.dp)){
            fun active(name:String)=when(name){"GAMES"->screen=="LOBBY";else->screen==name}
            listOf(Triple("GAMES","nav-games",onGames),Triple("ROOMS","nav-rooms",onRooms),Triple("SETTINGS","settings-button",onSettings)).forEach{(label,tag,action)->
                TextButton(onClick=action,modifier=Modifier.weight(1f).fillMaxHeight().testTag(tag),colors=ButtonDefaults.textButtonColors(containerColor=if(active(label))LocalRoomStyle.current.accent.copy(alpha=.22f) else Color.Transparent,contentColor=if(active(label))Color.White else LocalRoomStyle.current.accent)){Text(label,fontSize=11.sp,fontWeight=FontWeight.Black)}
            }
        }
    }
}

@Composable
internal fun CasinoFrame(model:BlackjackViewModel,title:String,onBack:(()->Unit)?,content:@Composable ColumnScope.(Int)->Unit){
    RoomMusic(model.room,model.settings.music)
    Box(Modifier.fillMaxSize()){
        Image(painterResource(model.room.background),null,Modifier.fillMaxSize(),contentScale=ContentScale.Crop)
        Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha=.65f)))
        Column(Modifier.align(Alignment.TopCenter).widthIn(max=860.dp).fillMaxSize().padding(horizontal=16.dp).verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(8.dp)){
            Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){
                if(onBack!=null)TextButton(onClick=onBack){Text(if(title=="SETTINGS")"BACK" else "LOBBY")}
                Text("${formatChips(model.game.bankroll)} CHIPS",Modifier.weight(1f),fontSize=15.sp,color=model.room.accent,fontWeight=FontWeight.Bold)

            }

            if(onBack!=null)Text(title,color=Color.White,fontSize=27.sp,fontWeight=FontWeight.Black)
            content(model.revision)
            if(model.game.bankroll<10&&!model.game.inRound&&!model.game.shuffling)Button(onClick={model.refill()},modifier=Modifier.fillMaxWidth()){Text("REFILL • 1,000 FREE CHIPS")}
            Spacer(Modifier.height(20.dp))
        }
    }
}
internal fun slotSymbols(room:RoomStyle):List<String> = when(room){
    RoomStyle.VEGAS->listOf("CHERRY","CHIPS","BELL","STAR","7")
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
    val reelPositions=remember(game){List(3){Animatable(game.reels[it].toFloat())}}
    var help by remember{mutableStateOf(false)}
    val symbols=slotSymbols(model.room)
    LaunchedEffect(spinning){
        if(spinning){
            coroutineScope {
                reelPositions.forEachIndexed { i, position ->
                    launch {
                        val start=position.value.toInt()
                        val target=start+20+i*5+(game.reels[i]-start%5+5)%5
                        position.animateTo(target.toFloat(),tween(1700+i*300,easing=CubicBezierEasing(.10f,.55f,.18f,1f)))
                        position.snapTo(game.reels[i].toFloat())
                    }
                }
            }
            spinning=false
        }
    }
    val pull:()->Unit={if(!spinning&&model.game.bankroll>=game.bet){model.change{model.game.bankroll-=game.bet;model.game.bankroll+=game.spin()};spinning=true}}
    CasinoFrame(model,"CHAOS SLOTS",onBack){

        Row(Modifier.fillMaxWidth().padding(vertical=4.dp),verticalAlignment=Alignment.CenterVertically){
        Surface(color=Color(0xFF302019),shape=RoundedCornerShape(topStart=36.dp,topEnd=36.dp,bottomStart=12.dp,bottomEnd=12.dp),border=BorderStroke(4.dp,model.room.accent),modifier=Modifier.weight(1f)){
            Column(Modifier.background(Brush.verticalGradient(listOf(Color(0xFF463329),Color(0xFF100F15),Color(0xFF35251F)))).padding(10.dp),horizontalAlignment=Alignment.CenterHorizontally){
                Text("CASINO CHAOS",color=model.room.accent,fontSize=20.sp,fontWeight=FontWeight.Black,letterSpacing=1.sp)
                Text("ONE ARM BANDIT",color=Color(0xFFF0D7A0),fontSize=10.sp,letterSpacing=2.sp)
                Spacer(Modifier.height(10.dp))
                Row(Modifier.fillMaxWidth().border(4.dp,Color(0xFFB4AAA0),RoundedCornerShape(8.dp)).padding(6.dp),horizontalArrangement=Arrangement.spacedBy(4.dp)){
                    repeat(3){i->
                        val position=reelPositions[i].value
                        val n=floor(position).toInt()
                        Box(Modifier.weight(1f).height(204.dp).testTag("slot-reel-$i").clipToBounds().background(Color(0xFF211B19))
                            .semantics{stateDescription=if(spinning)"Spinning" else symbols[game.reels[i]]}){
                            repeat(4){cell->
                                Box(Modifier.fillMaxWidth().offset(y=(68f*(cell-(position-floor(position)))).dp).height(68.dp),contentAlignment=Alignment.Center){SlotEmblem((n+cell+4)%5,model.room,Modifier.size(64.dp))}
                            }
                            Box(Modifier.fillMaxWidth().offset(y=68.dp).height(68.dp).border(1.dp,model.room.accent.copy(alpha=.8f)))
                            Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Black.copy(alpha=.80f),Color.Transparent,Color.Transparent,Color.Black.copy(alpha=.80f)))))
                        }
                    }
                }
                Text("◀  WIN LINE  ▶",color=model.room.accent,fontSize=12.sp)
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth().background(Color(0xFF090909),RoundedCornerShape(5.dp)).padding(8.dp),horizontalArrangement=Arrangement.SpaceBetween){
                    Text("BET ${game.bet}",color=Color(0xFFEDB958),fontSize=12.sp)
                    Text(if(spinning)"WIN —" else "WIN ${game.returned}",color=Color(0xFFEDB958),fontSize=12.sp)
                }
                Spacer(Modifier.height(8.dp))
                Box(Modifier.fillMaxWidth().height(28.dp).border(3.dp,Color(0xFF797575),RoundedCornerShape(9.dp)).background(Color(0xFF09090B)),contentAlignment=Alignment.Center){Text("COIN RETURN",color=Color.Gray,fontSize=9.sp)}
            }
        }
        BanditLever(spinning,!spinning&&model.game.bankroll>=game.bet,pull,Modifier.width(46.dp).height(290.dp))
        }
        Text(if(spinning)"Let them roll…" else game.message,color=if(game.returned>0)model.room.accent else Color.White,fontSize=18.sp,modifier=Modifier.fillMaxWidth(),textAlign=TextAlign.Center)
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)){listOf(10,25,50).forEach{n->OutlinedButton(onClick={model.change{game.bet=n}},enabled=!spinning,modifier=Modifier.weight(1f)){Text(if(n==game.bet)"● $n" else "$n")}}}
        Button(onClick=pull,enabled=!spinning&&model.game.bankroll>=game.bet,modifier=Modifier.fillMaxWidth().height(60.dp).testTag("slot-spin")){Text(if(spinning)"SPINNING…" else "SPIN • ${game.bet} CHIPS",fontSize=18.sp,fontWeight=FontWeight.Black)}
        TextButton(onClick={help=true}){Text("PAY TABLE & RULES")}

    }
    if(help)AlertDialog(onDismissRequest={help=false},title={Text("One line. Fixed odds.")},text={Column{symbols.forEachIndexed{i,s->Text("3 × $s = ${SlotsGame.triples[i]}× stake")};Text("Exactly two ${symbols[0]} = 2× stake. Everything else = 0.\nIndependent 20-stop reels: symbol weights 6 / 5 / 4 / 3 / 2. Theoretical return: 91.925%. Every spin is independent; no guaranteed wins.")}},confirmButton={TextButton(onClick={help=false}){Text("GOT IT")}})
}
@Composable
internal fun PokerPortrait(p:PokerSeat,index:Int,acting:Boolean,modifier:Modifier){
    val room=RoomStyle.entries[(LocalRoomStyle.current.ordinal+index)%RoomStyle.entries.size]
    val context=LocalContext.current
    var sheet by remember(room){mutableStateOf<ImageBitmap?>(null)}
    LaunchedEffect(room){sheet=withContext(Dispatchers.Default){decodeDealer(context,room)}}
    var frame by remember { mutableIntStateOf(0) }
    val motion=remember{Animatable(0f)}
    LaunchedEffect(p.expression,acting,p.lastAction){
        frame=when(p.expression){1->6;2->0;3->8;4->9;5->1;else->0}
        if(p.expression==3){
            repeat(2){motion.animateTo(1f,tween(85));motion.animateTo(-1f,tween(85))}
            motion.animateTo(0f,tween(110))
            if(acting){repeat(4){frame=if(it%2==0)10 else 11;delay(180)};frame=8}
        }else if(acting){
            motion.animateTo(.72f,tween(130));motion.animateTo(0f,spring(dampingRatio=Spring.DampingRatioMediumBouncy))
        }
    }
    Box(modifier.graphicsLayer{rotationZ=motion.value*3.5f;scaleX=1f+abs(motion.value)*.035f;scaleY=scaleX;translationY=-abs(motion.value)*4.dp.toPx()}.testTag("poker-portrait-$index").semantics{contentDescription="Opponent ${index+1}";stateDescription=if(sheet==null)"Loading" else "Ready"},contentAlignment=Alignment.Center){
    Canvas(Modifier.fillMaxSize().clipToBounds()){
        if(sheet==null)drawCircle(room.accent.copy(alpha=.4f),size.minDimension*.12f,center,style=Stroke(2.dp.toPx()))
        // Use the very same keyed animation atlas as the blackjack dealer.
        sheet?.let{drawDealerPose(it,room,frame,(size.width-minOf(size.width,size.height))/2f,minOf(size.width,size.height))}
    }
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
