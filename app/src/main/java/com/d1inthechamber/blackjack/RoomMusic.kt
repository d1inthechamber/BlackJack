package com.d1inthechamber.blackjack

import android.content.Context
import android.media.*
import android.os.Build
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner

internal fun roomMusicResource(room:RoomStyle)=when(room) {
    RoomStyle.VEGAS->R.raw.music_vegas;RoomStyle.CARNIVAL->R.raw.music_carnival
    RoomStyle.EGYPT->R.raw.music_egypt;RoomStyle.IRON->R.raw.music_iron
    RoomStyle.WEST->R.raw.music_west;RoomStyle.PUNK->R.raw.music_punk;RoomStyle.GREEN->R.raw.music_green
}
@Composable
internal fun RoomMusic(room:RoomStyle,enabled:Boolean) {
    val context=LocalContext.current;val owner=LocalLifecycleOwner.current
    DisposableEffect(room,enabled,owner) {
        val audio=context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        var player:MediaPlayer?=null
        var focused=false
        var closed=false
        val listener=AudioManager.OnAudioFocusChangeListener { change ->
            if(!closed) {
                focused=change==AudioManager.AUDIOFOCUS_GAIN
                if(focused&&enabled&&owner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) player?.start()
                else player?.pause()
            }
        }
        val attrs=AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_GAME).setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build()
        val request=if(Build.VERSION.SDK_INT>=26) AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN).setAudioAttributes(attrs).setOnAudioFocusChangeListener(listener).build() else null
        fun requestFocus() {
            if(!enabled||closed)return
            val result=if(Build.VERSION.SDK_INT>=26) audio.requestAudioFocus(request!!) else {
                @Suppress("DEPRECATION") audio.requestAudioFocus(listener,AudioManager.STREAM_MUSIC,AudioManager.AUDIOFOCUS_GAIN)
            }
            focused=result==AudioManager.AUDIOFOCUS_REQUEST_GRANTED
            if(focused)player?.start()
        }
        if(enabled) {
            player=MediaPlayer.create(context,roomMusicResource(room))?.apply { isLooping=true;setVolume(.30f,.30f) }
            if(owner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED))requestFocus()
        }
        val observer=LifecycleEventObserver { _,event ->
            if(event==Lifecycle.Event.ON_RESUME)requestFocus()
            if(event==Lifecycle.Event.ON_PAUSE)player?.pause()
        }
        owner.lifecycle.addObserver(observer)
        onDispose {
            closed=true;owner.lifecycle.removeObserver(observer)
            player?.release();player=null
            if(enabled) { if(Build.VERSION.SDK_INT>=26)audio.abandonAudioFocusRequest(request!!) else {
                @Suppress("DEPRECATION") audio.abandonAudioFocus(listener)
            } }
        }
    }
}
