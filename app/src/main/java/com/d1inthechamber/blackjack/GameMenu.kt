package com.d1inthechamber.blackjack

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
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
    if (atTable && foreground) {
        BlackjackApp(model.game, onMenu = { model.save(); atTable = false },
            onBuyIn = { model.startNew(); atTable = true })
    } else {
        MaterialTheme(colorScheme = darkColorScheme(primary = Color(0xFFD0B477))) {
            Box(Modifier.fillMaxSize()) {
                Image(painterResource(R.drawable.casino_dingy_1970s),null,Modifier.fillMaxSize(),contentScale=ContentScale.Crop)
                Column(Modifier.fillMaxSize().background(Color.Black.copy(alpha=.5f)).safeDrawingPadding().padding(32.dp),
                    horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.Center) {
                    Text("ROYAL FELT",fontSize=34.sp,color=Color(0xFFD0B477))
                    Text("A BAD NIGHT IN VEGAS",fontSize=12.sp,color=Color(0xFFC0B49A))
                    Spacer(Modifier.height(36.dp))
                    Button(enabled=model.hasSaved,onClick={ atTable=true },modifier=Modifier.fillMaxWidth()) { Text("CONTINUE") }
                    Button(onClick={ if(model.hasSaved) confirmNew=true else { model.startNew();atTable=true } },modifier=Modifier.fillMaxWidth()) { Text("START NEW GAME") }
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
