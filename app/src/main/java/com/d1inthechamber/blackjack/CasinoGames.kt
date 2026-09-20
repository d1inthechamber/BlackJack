package com.d1inthechamber.blackjack

import java.io.Serializable
import java.util.Random

internal fun singlePack() = listOf("♠","♥","♦","♣").flatMap { s -> (1..13).map { n -> Card(when(n){1->"A";11->"J";12->"Q";13->"K";else->"$n"},s) } }
internal val Card.number: Int get() = when(rank){"A"->1;"J"->11;"Q"->12;"K"->13;else->rank.toInt()}
internal val Card.red: Boolean get() = suit=="♥" || suit=="♦"
internal fun <T> shuffled(items:List<T>,rng:Random):MutableList<T> = items.toMutableList().also { java.util.Collections.shuffle(it,rng) }

/** A fixed, independent 20-stop strip on each reel. No adaptive odds or near-miss rewriting. */
internal class SlotsGame(val rng:Random=Random()):Serializable {
    var bet=10
    var reels=listOf(0,1,2)
    var spins=0
    var returned=0
    var message="Three reels. One payline. Make some chaos."
    fun spin():Int {
        reels=List(3){strip[rng.nextInt(strip.size)]}; spins++
        returned=bet*multiplier(reels)
        message=if(returned>0) "${returned} chips returned • ${if(returned>bet) "+${returned-bet}" else "${returned-bet}"} net" else "The house takes this one."
        return returned
    }
    companion object {
        val strip=listOf(0,0,0,0,0,0,1,1,1,1,1,2,2,2,2,3,3,3,4,4)
        val triples=listOf(5,8,15,30,60)
        fun multiplier(r:List<Int>):Int = when {
            r.distinct().size==1 -> triples[r[0]]
            r.count{it==0}==2 -> 2
            else -> 0
        }
    }
}
internal data class SolitaireSnapshot(val stock:List<Card>,val waste:List<Card>,val columns:List<List<Card>>,val hidden:List<Int>,val foundations:List<List<Card>>,val moves:Int):Serializable
internal data class SolitairePick(val pile:Int,val index:Int):Serializable // -1 waste, 0..6 tableau, 7..10 foundations
internal class SolitaireGame(val drawCount:Int=1, rng:Random=Random()):Serializable {
    var stock=shuffled(singlePack(),rng)
    var waste= mutableListOf<Card>()
    var columns=MutableList(7){mutableListOf<Card>()}
    var hidden=MutableList(7){it}
    var foundations=MutableList(4){mutableListOf<Card>()}
    var moves=0
    var rewarded=false
    private val history=mutableListOf<SolitaireSnapshot>()
    init { require(drawCount==1||drawCount==3); for(i in 0..6) repeat(i+1){columns[i].add(stock.removeAt(stock.lastIndex))} }
    val won get()=foundations.sumOf{it.size}==52
    val canUndo get()=history.isNotEmpty() && !won
    fun snapshot()=SolitaireSnapshot(stock.toList(),waste.toList(),columns.map{it.toList()},hidden.toList(),foundations.map{it.toList()},moves)
    private fun checkpoint(){history.add(snapshot());if(history.size>150)history.removeAt(0)}
    fun undo():Boolean {
        if(!canUndo)return false
        val s=history.removeAt(history.lastIndex);stock=s.stock.toMutableList();waste=s.waste.toMutableList();columns=s.columns.map{it.toMutableList()}.toMutableList();hidden=s.hidden.toMutableList();foundations=s.foundations.map{it.toMutableList()}.toMutableList();moves=s.moves
        return true
    }
    fun draw():Boolean {
        if(won || stock.isEmpty()&&waste.isEmpty())return false
        checkpoint()
        if(stock.isEmpty()){stock=waste.reversed().toMutableList();waste.clear()} else repeat(minOf(drawCount,stock.size)){waste.add(stock.removeAt(stock.lastIndex))}
        moves++;return true
    }
    fun cards(p:SolitairePick):List<Card> = when {
        p.pile==-1 && p.index==waste.lastIndex -> waste.takeLast(1)
        p.pile in 0..6 && p.index>=hidden[p.pile] && p.index in columns[p.pile].indices -> columns[p.pile].drop(p.index)
        p.pile in 7..10 && p.index==foundations[p.pile-7].lastIndex -> foundations[p.pile-7].takeLast(1)
        else -> emptyList()
    }
    fun legal(p:SolitairePick,dest:Int):Boolean {
        val cs=cards(p);if(cs.isEmpty() || p.pile==dest || won)return false
        if(cs.zipWithNext().any{(a,b)->a.red==b.red || a.number!=b.number+1})return false
        return when(dest){
            in 0..6 -> columns[dest].lastOrNull()?.let{it.red!=cs[0].red && it.number==cs[0].number+1} ?: (cs[0].number==13)
            in 7..10 -> cs.size==1 && (foundations[dest-7].lastOrNull()?.let{it.suit==cs[0].suit && cs[0].number==it.number+1} ?: (cs[0].number==1))
            else -> false
        }
    }
    fun move(p:SolitairePick,dest:Int):Boolean {
        if(!legal(p,dest))return false
        checkpoint();val cs=cards(p)
        when(p.pile){-1->waste.removeAt(waste.lastIndex);in 0..6->{repeat(cs.size){columns[p.pile].removeAt(columns[p.pile].lastIndex)};hidden[p.pile]=minOf(hidden[p.pile],maxOf(0,columns[p.pile].size-1))};else->foundations[p.pile-7].removeAt(foundations[p.pile-7].lastIndex)}
        if(dest<7)columns[dest].addAll(cs) else foundations[dest-7].addAll(cs)
        moves++;return true
    }
    fun hint():Pair<SolitairePick,Int>? {
        val picks=mutableListOf<SolitairePick>()
        if(waste.isNotEmpty())picks.add(SolitairePick(-1,waste.lastIndex))
        for(c in 0..6)for(i in hidden[c] until columns[c].size)picks.add(SolitairePick(c,i))
        for(d in (7..10)+(0..6))for(p in picks)if(legal(p,d) && !(d<7&&columns[d].isEmpty()&&p.pile in 0..6&&p.index==0))return p to d
        return null
    }
}

// Extensions deliberately keep the serialized SolitaireGame class shape identical to
// v3.2 while adding no-moves detection for this release.
internal val SolitaireGame.stuck: Boolean get() = !won && !hasAvailableMove()

internal fun SolitaireGame.hasAvailableMove():Boolean {
    if(won)return false
    val picks=mutableListOf<SolitairePick>()
    if(waste.isNotEmpty())picks.add(SolitairePick(-1,waste.lastIndex))
    for(c in 0..6)for(i in hidden[c] until columns[c].size)picks.add(SolitairePick(c,i))
    for(f in 7..10)if(foundations[f-7].isNotEmpty())picks.add(SolitairePick(f,foundations[f-7].lastIndex))
    if(picks.any{p->(0..10).any{d->legal(p,d)}})return true
    // With unlimited recycling, every remaining stock/waste card is reachable on a
    // later pass, so do not report a dead deal while drawing can still expose a move.
    return (stock+waste).distinct().any{card->
        (0..6).any{d->columns[d].lastOrNull()?.let{it.red!=card.red&&it.number==card.number+1}?: (card.number==13)} ||
            (7..10).any{d->foundations[d-7].lastOrNull()?.let{it.suit==card.suit&&card.number==it.number+1}?: (card.number==1)}
    }
}
