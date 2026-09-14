package com.d1inthechamber.blackjack

import android.content.Context
import android.util.AtomicFile
import java.io.*

internal class CasinoState:Serializable {
    var slots=SlotsGame()
    var solitaire=SolitaireGame()
    var poker=PokerGame()
    var lastGame="BLACKJACK"
}
internal data class CasinoArchive(val version:Int=1,val blackjack:SavedGame,val casino:CasinoState):Serializable
internal class CasinoStore(context:Context) {
    private val file=AtomicFile(File(context.filesDir,"casino-chaos-v1.bin"))
    fun load():CasinoArchive?=try { file.openRead().use { ObjectInputStream(it).use{it.readObject() as CasinoArchive} }.also{require(it.version==1);it.blackjack.restore()} } catch(_:Exception){null}
    fun save(game:BlackjackState,casino:CasinoState){
        val bytes=ByteArrayOutputStream().also{ObjectOutputStream(it).use{out->out.writeObject(CasinoArchive(blackjack=game.savedGame(),casino=casino))}}.toByteArray()
        val out=file.startWrite();try{out.write(bytes);file.finishWrite(out)}catch(e:Exception){file.failWrite(out);throw e}
    }
}
