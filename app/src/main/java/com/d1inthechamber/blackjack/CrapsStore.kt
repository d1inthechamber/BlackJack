package com.d1inthechamber.blackjack

import android.content.Context
import android.util.AtomicFile
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.io.Serializable

internal data class SavedCraps(val version: Int = 1, val game: CrapsGame) : Serializable {
    companion object { private const val serialVersionUID = 1L }
}

/** Craps is kept beside the v1 casino archive so existing v3.2 saves remain readable. */
internal class CrapsStore(context: Context) {
    private val file = AtomicFile(File(context.filesDir, "casino-chaos-craps-v1.bin"))

    fun load(): CrapsGame? = try {
        file.openRead().use { stream ->
            val saved = ObjectInputStream(stream).use { it.readObject() as SavedCraps }
            require(saved.version == 1)
            saved.game
        }
    } catch (_: Exception) { null }

    fun save(game: CrapsGame) {
        val bytes = ByteArrayOutputStream().also { buffer ->
            ObjectOutputStream(buffer).use { it.writeObject(SavedCraps(game = game)) }
        }.toByteArray()
        val output = file.startWrite()
        try {
            output.write(bytes)
            file.finishWrite(output)
        } catch (error: Exception) {
            file.failWrite(output)
            throw error
        }
    }
}
