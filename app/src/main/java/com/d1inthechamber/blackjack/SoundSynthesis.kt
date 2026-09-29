package com.d1inthechamber.blackjack

import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.Random
import kotlin.math.*

internal enum class CasinoSound(val seconds: Double) {
    TAP(.07), CARD(.18), SHUFFLE(.65), CHIPS(.38), DICE_SHAKE(.45), DICE_ROLL(.72),
    MONEY(.55), LEVER(.48), TICK(.045), REEL_STOP(.16), WIN(1.8), LOSE(1.9), CLICK(.055), INVALID(.12)
}

/** Original, offline Foley and short musical cues. PCM avoids codec/device variations. */
internal fun casinoSoundWave(sound: CasinoSound): ByteArray {
    val rate = 22050
    val samples = DoubleArray((sound.seconds*rate).toInt())
    val random = Random(7300L+sound.ordinal)
    fun tone(start: Double, length: Double, frequency: Double, gain: Double, decay: Double = 7.0) {
        for (i in max(0,(start*rate).toInt()) until min(samples.size,((start+length)*rate).toInt())) {
            val t=i.toDouble()/rate-start
            val env=min(1.0,t/.004)*exp(-t/length*decay)
            samples[i]+=gain*env*(sin(2*PI*frequency*t)+.20*sin(2*PI*frequency*2.01*t))
        }
    }
    fun noise(start: Double, length: Double, gain: Double, decay: Double = 3.0) {
        var last=0.0
        for (i in max(0,(start*rate).toInt()) until min(samples.size,((start+length)*rate).toInt())) {
            val t=i.toDouble()/rate-start
            val next=random.nextDouble()*2-1
            samples[i]+=(next-last*.60)*gain*min(1.0,t/.002)*exp(-t/length*decay)
            last=next
        }
    }
    when(sound) {
        CasinoSound.TAP -> {noise(0.0,.045,.22);tone(0.0,.07,620.0,.12)}
        CasinoSound.CARD -> {noise(0.0,.16,.19,2.2);tone(.10,.07,230.0,.13)}
        CasinoSound.SHUFFLE -> repeat(8){noise(it*.065,.10,.15)}
        CasinoSound.CHIPS -> repeat(4){tone(it*.065,.18,1400.0+it*190,.18);noise(it*.065,.025,.10)}
        CasinoSound.DICE_SHAKE -> repeat(8){noise(it*.049,.044,.23);tone(it*.049,.06,580.0+it%3*180,.12)}
        CasinoSound.DICE_ROLL -> listOf(0.0,.065,.15,.255,.39,.56).forEachIndexed { i,t ->
            noise(t,.075,.32/(1+i*.14));tone(t,.13,330.0+i*67,.29/(1+i*.12))
        }
        CasinoSound.MONEY -> repeat(5){noise(it*.092,.16,.18,1.6);tone(it*.092,.05,175.0,.07)}
        CasinoSound.LEVER -> {
            noise(0.0,.05,.27);tone(0.0,.15,145.0,.30)
            repeat(9){tone(.065+it*.026,.06,550.0-it*31,.10);noise(.065+it*.026,.025,.08)}
            tone(.33,.15,210.0,.32);noise(.33,.05,.28)
        }
        CasinoSound.TICK -> {noise(0.0,.022,.25);tone(0.0,.04,1250.0,.23)}
        CasinoSound.REEL_STOP -> {noise(0.0,.055,.30);tone(0.0,.16,185.0,.33)}
        CasinoSound.WIN -> {
            listOf(523.25,659.25,783.99,1046.50).forEachIndexed { i,f ->tone(i*.16,.50,f,.23,3.0) }
            listOf(523.25,659.25,783.99,1046.50).forEach { tone(.74,1.06,it,.13,2.9) }
        }
        CasinoSound.LOSE -> {
            listOf(392.0,349.23,311.13,261.63).forEachIndexed { i,f ->tone(i*.27,.70,f,.19,2.5) }
            tone(1.10,.8,130.81,.13,2.4);tone(1.10,.8,155.56,.09,2.4)
        }
        CasinoSound.CLICK -> tone(0.0,.05,880.0,.15)
        CasinoSound.INVALID -> {tone(0.0,.12,185.0,.14);tone(.045,.07,145.0,.11)}
    }
    val wave=ByteBuffer.allocate(44+samples.size*2).order(ByteOrder.LITTLE_ENDIAN)
    wave.put("RIFF".toByteArray());wave.putInt(36+samples.size*2);wave.put("WAVEfmt ".toByteArray())
    wave.putInt(16);wave.putShort(1);wave.putShort(1);wave.putInt(rate);wave.putInt(rate*2)
    wave.putShort(2);wave.putShort(16);wave.put("data".toByteArray());wave.putInt(samples.size*2)
    samples.forEachIndexed { i,s ->
        val tail=min(1.0,(samples.size-i).toDouble()/(rate*.012))
        wave.putShort((tanh(s)*tail*27000).roundToInt().toShort())
    }
    return wave.array()
}
