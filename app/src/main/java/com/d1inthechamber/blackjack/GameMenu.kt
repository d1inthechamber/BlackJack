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
internal fun RoomPicker(selected:RoomStyle,onSelect:(RoomStyle)->Unit,onBack:()->Unit) {
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
