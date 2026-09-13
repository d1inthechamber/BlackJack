package com.d1inthechamber.blackjack

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner

@Composable
fun GameRoot(model: BlackjackViewModel) {
    CompositionLocalProvider(LocalRoomStyle provides model.room) {
    var chooseRoom by rememberSaveable { mutableStateOf(false) }
    var atTable by rememberSaveable { mutableStateOf(false) }
    var confirmNew by remember { mutableStateOf(false) }
    var foreground by remember { mutableStateOf(true) }
    val owner = LocalLifecycleOwner.current
    DisposableEffect(owner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) foreground = false
            if (event == Lifecycle.Event.ON_START) foreground = true
        }
        owner.lifecycle.addObserver(observer)
        onDispose { owner.lifecycle.removeObserver(observer) }
    }
    BackHandler(atTable) { model.save(); atTable = false }
    if (chooseRoom) {
        RoomPicker(model.room,onSelect={model.selectRoom(it);chooseRoom=false},onBack={chooseRoom=false})
    } else if (atTable && foreground) {
        BlackjackApp(model.game, onMenu = { model.save(); atTable = false },
            onBuyIn = { model.startNew(); atTable = true })
    } else {
        MaterialTheme(colorScheme = darkColorScheme(primary = Color(0xFFD0B477))) {
            Box(Modifier.fillMaxSize()) {
                Image(painterResource(model.room.background),null,Modifier.fillMaxSize(),contentScale=ContentScale.Crop)
                Column(Modifier.fillMaxSize().background(Color.Black.copy(alpha=.5f)).safeDrawingPadding().padding(32.dp),
                    horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.Center) {
                    Text("ROYAL FELT",fontSize=34.sp,color=Color(0xFFD0B477))
                    Text(model.room.title.uppercase(),fontSize=12.sp,color=Color(0xFFC0B49A))
                    Spacer(Modifier.height(36.dp))
                    Button(enabled=model.hasSaved,onClick={ atTable=true },modifier=Modifier.fillMaxWidth()) { Text("CONTINUE") }
                    Button(onClick={ if(model.hasSaved) confirmNew=true else { model.startNew();atTable=true } },modifier=Modifier.fillMaxWidth()) { Text("START NEW GAME") }
                    OutlinedButton(onClick={chooseRoom=true},modifier=Modifier.fillMaxWidth()) { Text("CHOOSE ROOM") }
                    Text("1,000 free chips • No real money",color=Color(0xFFC0B49A),fontSize=12.sp)
                }
            }
            if(confirmNew) AlertDialog(onDismissRequest={confirmNew=false},title={Text("Start a fresh game?")},
                text={Text("This replaces your saved game with 1,000 free chips.")},
                confirmButton={TextButton(onClick={model.startNew();confirmNew=false;atTable=true}) { Text("START FRESH") }},
                dismissButton={TextButton(onClick={confirmNew=false}){Text("CANCEL")}})
        }
    }
}

}

@Composable
private fun RoomPicker(selected:RoomStyle,onSelect:(RoomStyle)->Unit,onBack:()->Unit) {
    BackHandler { onBack() }
    MaterialTheme(colorScheme=darkColorScheme(primary=selected.accent)) {
        Column(Modifier.fillMaxSize().background(Color(0xFF10100F)).safeDrawingPadding().padding(16.dp)) {
            Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
                Text("CHOOSE YOUR ROOM",Modifier.weight(1f),fontSize=20.sp,color=selected.accent)
                TextButton(onClick=onBack) { Text("BACK") }
            }
            Text("Room, dealer and deck • Your game stays with you",color=Color.LightGray,fontSize=12.sp)
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(12.dp)) {
                RoomStyle.entries.forEach { room ->
                    Card(onClick={onSelect(room)},modifier=Modifier.fillMaxWidth()) {
                        Box(Modifier.fillMaxWidth().height(155.dp)) {
                            Image(painterResource(room.background),null,Modifier.fillMaxSize(),contentScale=ContentScale.Crop)
                            Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha=.35f)))
                            Column(Modifier.align(Alignment.BottomStart).padding(16.dp)) {
                                Text(room.title + if(room==selected) " ✓" else "",fontSize=22.sp,color=room.accent)
                                Text(room.host,fontSize=14.sp,color=Color.White)
                            }
                            room.cardBack?.let { Image(painterResource(it),null,Modifier.align(Alignment.CenterEnd).padding(16.dp).size(62.dp,93.dp)) }
                        }
                    }
                }
            }
        }
    }
}
