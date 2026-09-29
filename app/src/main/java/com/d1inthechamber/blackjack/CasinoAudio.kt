package com.d1inthechamber.blackjack

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import androidx.compose.runtime.staticCompositionLocalOf
import kotlinx.coroutines.*
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue

internal val LocalCasinoAudio=staticCompositionLocalOf<CasinoAudio?> { null }

internal class CasinoAudio(context: Context) {
    private val pool=SoundPool.Builder().setMaxStreams(8).setAudioAttributes(
        AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build()).build()
    private val scope=CoroutineScope(SupervisorJob()+Dispatchers.IO)
    private val ids=ConcurrentHashMap<CasinoSound,Int>()
    private val ready=ConcurrentHashMap.newKeySet<Int>()
    private val streams=ConcurrentLinkedQueue<Int>()
    @Volatile private var closed=false
    @Volatile var enabled=true
        set(value) { field=value; if(!value)stopAll() }
    init {
        pool.setOnLoadCompleteListener { _,id,status -> if(status==0&&!closed)ready.add(id) }
        val directory=File(context.cacheDir,"casino-sounds-36").apply { mkdirs() }
        scope.launch {
            CasinoSound.entries.forEach { sound ->
                ensureActive()
                val file=File(directory,"${sound.name.lowercase()}.wav")
                if(!file.exists()||file.length()<44)file.writeBytes(casinoSoundWave(sound))
                synchronized(this@CasinoAudio) { if(!closed)ids[sound]=pool.load(file.absolutePath,1) }
            }
        }
    }
    fun isReady(sound:CasinoSound)=ids[sound]?.let { it in ready }==true
    @Synchronized fun play(sound: CasinoSound, volume:Float=.65f, rate:Float=1f):Int {
        if(closed||!enabled)return 0
        val id=ids[sound] ?: return 0
        if(id !in ready)return 0
        val stream=pool.play(id,volume.coerceIn(0f,1f),volume.coerceIn(0f,1f),1,0,rate.coerceIn(.5f,2f))
        if(stream!=0)streams.add(stream)
        while(streams.size>32)streams.poll()
        return stream
    }
    @Synchronized fun stopAll() { if(!closed)while(true)pool.stop(streams.poll() ?: break) }
    @Synchronized fun close() { if(!closed){stopAll();closed=true;scope.cancel();pool.release()} }
}
