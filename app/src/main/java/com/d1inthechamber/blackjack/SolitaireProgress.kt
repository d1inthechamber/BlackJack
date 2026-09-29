package com.d1inthechamber.blackjack

internal enum class SolitaireProgress { AVAILABLE, BLOCKED, UNKNOWN }

/** Search exposed moves, including useful foundation reversals, without peeking at
 * hidden faces. Cycles alone are not progress. A bounded search never declares a
 * loss when it cannot prove one. */
internal fun solitaireProgress(initial: SolitaireSnapshot, drawCount:Int, limit:Int=2500):SolitaireProgress {
    if(initial.foundations.sumOf { it.size }==52)return SolitaireProgress.AVAILABLE
    val baseline=initial.foundations.mapNotNull { it.lastOrNull() }.associate { it.suit to it.number }
    val reachable=mutableSetOf<Card>()
    var stock=initial.stock.toMutableList();val waste=initial.waste.toMutableList()
    val draws=mutableSetOf<Pair<List<Card>,List<Card>>>()
    while(draws.add(stock.toList() to waste.toList())) {
        waste.lastOrNull()?.let { reachable.add(it) }
        if(stock.isEmpty()) { if(waste.isEmpty())break;stock=waste.reversed().toMutableList();waste.clear() }
        else repeat(minOf(drawCount,stock.size)){waste.add(stock.removeAt(stock.lastIndex))}
    }
    data class Board(val columns:List<List<Card>>,val foundations:List<List<Card>>) {
        fun key()=columns.mapIndexed { i,c -> initial.hidden[i].toString()+":"+c.drop(initial.hidden[i]).joinToString() }.sorted().joinToString("|")+"/"+
            foundations.map { it.lastOrNull()?.toString() ?: "" }.sorted().joinToString("|")
    }
    fun tableau(card:Card,pile:List<Card>)=pile.lastOrNull()?.let { it.red!=card.red&&it.number==card.number+1 } ?: (card.number==13)
    fun foundation(card:Card,pile:List<Card>)=pile.lastOrNull()?.let { it.suit==card.suit&&it.number+1==card.number } ?: (card.number==1)
    val firstBoard=Board(initial.columns,initial.foundations)
    val queue=java.util.ArrayDeque<Board>();queue.add(firstBoard)
    val seen=mutableSetOf(firstBoard.key())
    var truncated=false
    while(queue.isNotEmpty()) {
        val b=queue.removeFirst()
        if(reachable.any { card -> b.columns.any { tableau(card,it) }||b.foundations.any { foundation(card,it) } })return SolitaireProgress.AVAILABLE
        fun add(source:Int,index:Int,dest:Int) {
            val columns=b.columns.map { it.toMutableList() };val foundations=b.foundations.map { it.toMutableList() }
            val pile=if(source<7)columns[source] else foundations[source-7]
            val cards=pile.drop(index);repeat(cards.size){pile.removeAt(pile.lastIndex)}
            (if(dest<7)columns[dest] else foundations[dest-7]).addAll(cards)
            val next=Board(columns,foundations);val key=next.key()
            if(key !in seen) {
                if(seen.size>=limit)truncated=true
                else {seen.add(key);queue.add(next)}
            }
        }
        for(source in 0..10) {
            val pile=if(source<7)b.columns[source] else b.foundations[source-7]
            val first=if(source<7)initial.hidden[source] else pile.lastIndex
            if(first<0)continue
            for(index in first until pile.size) {
                val cards=pile.drop(index)
                if(cards.zipWithNext().any { (a,c)->a.red==c.red||a.number!=c.number+1 })continue
                for(dest in 0..10) {
                    if(dest==source)continue
                    val legal=if(dest<7)tableau(cards[0],b.columns[dest]) else cards.size==1&&foundation(cards[0],b.foundations[dest-7])
                    if(!legal)continue
                    if(source<7&&initial.hidden[source]>0&&index==initial.hidden[source])return SolitaireProgress.AVAILABLE
                    if(dest>=7&&cards[0].number>(baseline[cards[0].suit] ?: 0))return SolitaireProgress.AVAILABLE
                    if(dest<7&&b.columns[dest].isEmpty()&&source<7&&index==0)continue
                    // Moving cards between foundation slots is a symmetric relabeling.
                    if(source>=7&&dest>=7)continue
                    add(source,index,dest)
                }
            }
        }
    }
    return if(truncated)SolitaireProgress.UNKNOWN else SolitaireProgress.BLOCKED
}
