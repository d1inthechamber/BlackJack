package com.d1inthechamber.blackjack

import android.media.AudioAttributes
import android.media.SoundPool
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable

internal enum class HumanReaction { CHEER, FRUSTRATED, TENSE }

/** Plays one recorded human reaction per completed event, never a looping voice. */
@Composable
internal fun HumanReactionVoice(eventKey:Int,reaction:HumanReaction?,enabled:Boolean){
    val pool=remember{
        SoundPool.Builder().setMaxStreams(2).setAudioAttributes(
            AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_GAME).setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build()
        ).build()
    }
    val context=androidx.compose.ui.platform.LocalContext.current
    val samples=remember(pool){mutableStateMapOf<HumanReaction,Int>()}
    var ready by remember{mutableIntStateOf(0)}
    var lastPlayed by rememberSaveable{mutableIntStateOf(eventKey)}
    DisposableEffect(pool){
        pool.setOnLoadCompleteListener{_,_,status->if(status==0)ready++}
        samples[HumanReaction.CHEER]=pool.load(context,R.raw.dealer_laugh,1)
        samples[HumanReaction.FRUSTRATED]=pool.load(context,R.raw.dealer_groan,1)
        samples[HumanReaction.TENSE]=pool.load(context,R.raw.dealer_grunt,1)
        onDispose{pool.release()}
    }
    LaunchedEffect(eventKey,reaction,enabled,ready){
        if(eventKey>lastPlayed&&reaction!=null&&ready>=HumanReaction.entries.size){
            if(enabled)samples[reaction]?.let{pool.play(it,.72f,.72f,1,0,1f)}
            lastPlayed=eventKey
        }
    }
}
