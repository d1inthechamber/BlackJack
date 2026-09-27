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
internal data class CasinoSession(val archive:CasinoArchive,val craps:CrapsGame):Serializable {
    companion object { private const val serialVersionUID=1L }
}
internal class CasinoStore(context:Context) {
    private val file=AtomicFile(File(context.filesDir,"casino-chaos-v1.bin"))
    private val sessionFile=AtomicFile(File(context.filesDir,"casino-chaos-session-v2.bin"))
    fun loadSession():CasinoSession?=try {
        sessionFile.openRead().use { ObjectInputStream(it).use { it.readObject() as CasinoSession } }
            .also { require(it.archive.version==1);it.archive.blackjack.restore() }
    }catch(_:Exception){null}
    fun load():CasinoArchive?=loadSession()?.archive ?: try { file.openRead().use { ObjectInputStream(it).use{it.readObject() as CasinoArchive} }.also{require(it.version==1);it.blackjack.restore()} } catch(_:Exception){null}
    fun save(game:BlackjackState,casino:CasinoState,craps:CrapsGame?=null){
        val archive=CasinoArchive(blackjack=game.savedGame(),casino=casino)
        val target=if(craps==null)file else sessionFile
        val saved=if(craps==null)archive else CasinoSession(archive,craps)
        val bytes=ByteArrayOutputStream().also{ObjectOutputStream(it).use{out->out.writeObject(saved)}}.toByteArray()
        val out=target.startWrite();try{out.write(bytes);target.finishWrite(out)}catch(e:Exception){target.failWrite(out);throw e}
    }
}
