package com.d1inthechamber.blackjack

import android.media.AudioAttributes
import android.media.SoundPool
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.delay

internal fun dealerVoiceResource(mood:DealerMood)=when(mood){DealerMood.ANGRY->R.raw.dealer_groan;DealerMood.SMUG->R.raw.dealer_grunt;DealerMood.LAUGH->R.raw.dealer_laugh;DealerMood.IDLE->0}

@Composable
internal fun DealerVoice(game:BlackjackState,flights:CardFlights,enabled:Boolean){
    val context=LocalContext.current;val room=LocalRoomStyle.current
    val pool=remember{SoundPool.Builder().setMaxStreams(1).setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_GAME).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build()).build()}
    val sounds=remember{mutableMapOf<Int,Int>()}
    var loaded by remember{mutableIntStateOf(0)}
    var stream by remember{mutableIntStateOf(0)}
    var lastReacted by remember(game){mutableIntStateOf(if(game.finished)game.roundNumber else -1)}
    DisposableEffect(pool){pool.setOnLoadCompleteListener{_,_,status->if(status==0)loaded++};listOf(R.raw.dealer_grunt,R.raw.dealer_groan,R.raw.dealer_laugh).forEach{sounds[it]=pool.load(context,it,1)};onDispose{pool.release()}}
    LaunchedEffect(enabled){if(!enabled&&stream!=0)pool.stop(stream)}
    LaunchedEffect(game.finished,game.roundNumber,flights.pending,enabled,loaded){
        if(game.finished&&!flights.pending&&game.roundNumber!=lastReacted){
            delay(100)
            if(flights.pending)return@LaunchedEffect
            if(!enabled){lastReacted=game.roundNumber;return@LaunchedEffect}
            if(loaded<3)return@LaunchedEffect
            lastReacted=game.roundNumber
            val sound=sounds[dealerVoiceResource(dealerMood(game.lastRound?.net))]?:return@LaunchedEffect
            val pitch=when(room){RoomStyle.VEGAS->.92f;RoomStyle.CARNIVAL->1.10f;RoomStyle.EGYPT->.84f;RoomStyle.IRON->.78f;RoomStyle.WEST->.95f;RoomStyle.PUNK->1.06f;RoomStyle.GREEN->.88f}
            stream=pool.play(sound,.55f,.55f,1,0,pitch)
        }
    }
}
