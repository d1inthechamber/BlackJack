package com.d1inthechamber.blackjack

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.math.sin

@Composable
fun TableEventOverlay(game: BlackjackState) {
    var visible by remember(game) { mutableStateOf(false) }
    var headline by remember(game) { mutableStateOf("") }
    var net by remember(game) { mutableStateOf("") }
    LaunchedEffect(game, game.roundNumber, game.finished, game.shuffling) {
        visible = false
        if (game.finished && !game.shuffling) {
            headline = game.message.uppercase()
            net = game.lastRound?.let { "ROUND NET  ${if (it.net > 0) "+" else ""}${formatChips(it.net)} CHIPS" } ?: ""
            visible = true
            delay(2300)
            visible = false
        }
    }
    Box(Modifier.fillMaxSize(),contentAlignment=Alignment.Center) {
        AnimatedVisibility(visible,
            enter=slideInHorizontally(tween(480),initialOffsetX={-it})+fadeIn(tween(200)),
            exit=slideOutHorizontally(tween(400),targetOffsetX={it})+fadeOut(tween(300))) {
            Column(Modifier.fillMaxWidth().background(Color(0xF0201810))
                .border(1.dp,Color(0xFFA98C51)).padding(vertical=22.dp,horizontal=16.dp),
                horizontalAlignment=Alignment.CenterHorizontally) {
                Text(headline,color=Color(0xFFE1CB91),fontSize=25.sp,fontWeight=FontWeight.Black,textAlign=TextAlign.Center)
                Text(net,color=Color(0xFFBEB29A),fontSize=12.sp)
            }
        }
        if(game.shuffling) {
            val phase by rememberInfiniteTransition(label="shuffle").animateFloat(0f,6.2832f,
                infiniteRepeatable(tween(650),repeatMode=RepeatMode.Restart),label="riffle")
            Column(Modifier.fillMaxWidth().background(Color(0xEB17130E)).padding(22.dp),
                horizontalAlignment=Alignment.CenterHorizontally) {
                Box(Modifier.size(240.dp,135.dp),contentAlignment=Alignment.Center) {
                    repeat(8) { i ->
                        val side=if(i%2==0) -1 else 1
                        val wave=sin(phase+i*.4f)
                        Box(Modifier.offset(x=(side*(35+wave*25)+i*2).dp,y=(i*2-wave*8).dp)
                            .rotate(side*(8+wave*12))) { CardView("?",64.dp,90.dp) }
                    }
                }
                Text("SHUFFLING THE SHOE",color=Color(0xFFE1CB91),fontWeight=FontWeight.Bold)
                Text("Six decks. A fresh start.",color=Color(0xFFBEB29A),fontSize=12.sp)
            }
        }
    }
}
