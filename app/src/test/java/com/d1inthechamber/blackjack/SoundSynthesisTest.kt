package com.d1inthechamber.blackjack

import java.nio.ByteBuffer
import java.nio.ByteOrder
import org.junit.Assert.*
import org.junit.Test
import kotlin.math.abs

class SoundSynthesisTest {
    @Test fun allCuesProduceAudibleUnclippedMonoPcmAtTheDeclaredDuration() {
        CasinoSound.entries.forEach { sound ->
            val wave=casinoSoundWave(sound);val b=ByteBuffer.wrap(wave).order(ByteOrder.LITTLE_ENDIAN)
            assertEquals("RIFF",String(wave,0,4));assertEquals("WAVE",String(wave,8,4))
            assertEquals(wave.size-8,b.getInt(4));assertEquals(1,b.getShort(20).toInt())
            assertEquals(1,b.getShort(22).toInt());assertEquals(22050,b.getInt(24))
            assertEquals(16,b.getShort(34).toInt());assertEquals(wave.size-44,b.getInt(40))
            val pcm=(44 until wave.size step 2).map { b.getShort(it).toInt() }
            assertEquals((sound.seconds*22050).toInt(),pcm.size)
            assertTrue("$sound must be audible",pcm.map { abs(it) }.average()>40)
            assertTrue("$sound must not clip",pcm.all { abs(it)<27000 })
            assertTrue("$sound fades out",abs(pcm.last())<100)
        }
        assertFalse(casinoSoundWave(CasinoSound.WIN).contentEquals(casinoSoundWave(CasinoSound.LOSE)))
    }
}
