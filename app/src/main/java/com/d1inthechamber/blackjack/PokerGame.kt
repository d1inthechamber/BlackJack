package com.d1inthechamber.blackjack

import java.io.Serializable
import java.util.Random

internal data class PokerRank(val value:Long,val title:String):Comparable<PokerRank>,Serializable { override fun compareTo(other:PokerRank)=value.compareTo(other.value) }
internal fun pokerRank(cards:List<Card>):PokerRank {
    require(cards.size in 5..7 && cards.distinct().size==cards.size)
    fun five(c:List<Card>):PokerRank {
        val ns=c.map{if(it.number==1)14 else it.number}.sortedDescending()
        val groups=ns.groupingBy{it}.eachCount().entries.sortedWith(compareByDescending<Map.Entry<Int,Int>>{it.value}.thenByDescending{it.key})
        val flush=c.map{it.suit}.distinct().size==1
        val distinct=ns.distinct()
        val straight=if(distinct.size==5 && distinct[0]-distinct[4]==4)distinct[0] else if(distinct==listOf(14,5,4,3,2))5 else 0
        val cat:Int;val keys:List<Int>
        when {
            flush&&straight>0->{cat=8;keys=listOf(straight)}
            groups[0].value==4->{cat=7;keys=groups.map{it.key}}
            groups[0].value==3&&groups[1].value==2->{cat=6;keys=groups.map{it.key}}
            flush->{cat=5;keys=ns}
            straight>0->{cat=4;keys=listOf(straight)}
            groups[0].value==3->{cat=3;keys=groups.map{it.key}}
            groups[0].value==2&&groups[1].value==2->{cat=2;keys=groups.map{it.key}}
            groups[0].value==2->{cat=1;keys=groups.map{it.key}}
            else->{cat=0;keys=ns}
        }
        var v=cat.toLong();repeat(5){v=v*15+(keys.getOrNull(it)?:0)}
        return PokerRank(v,listOf("High card","One pair","Two pair","Three of a kind","Straight","Flush","Full house","Four of a kind","Straight flush")[cat])
    }
    var best=PokerRank(-1,"")
    for(a in 0 until cards.size-4)for(b in a+1 until cards.size-3)for(c in b+1 until cards.size-2)for(d in c+1 until cards.size-1)for(e in d+1 until cards.size){val rank=five(listOf(cards[a],cards[b],cards[c],cards[d],cards[e]));if(rank>best)best=rank}
    return best
}
internal enum class PokerPersonality(val label:String,val aggression:Double,val looseness:Double,val bluff:Double,val habit:String):Serializable {
    BULLY("The Bully",.78,.12,.25,"drums their fingers"),ROCK("The Rock",.35,-.12,.04,"goes perfectly still"),SHOWBOAT("The Showboat",.67,.06,.22,"flashes a wide grin"),GAMBLER("The Gambler",.55,.22,.14,"glances at the pot"),VETERAN("The Veteran",.58,0.0,.13,"adjusts their collar")
}
internal data class PokerSeat(val name:String,val personality:PokerPersonality,var stack:Int=500,var hole:List<Card> = emptyList(),var folded:Boolean=false,var streetBet:Int=0,var total:Int=0,var acted:Boolean=false,var actedAt:Int=0,var tell:String="Watches the table",var lastAction:String="",var expression:Int=0):Serializable
internal enum class PokerAction { FOLD,CALL,RAISE }
/** This is the entire input to the bot. Opponents' holes and the actual deck are deliberately absent. */
internal data class PokerObservation(val hole:List<Card>,val board:List<Card>,val opponents:Int,val pot:Int,val toCall:Int,val stack:Int,val streetBet:Int,val currentBet:Int,val minRaise:Int,val canRaise:Boolean,val humanRaises:Int)
internal data class PokerDecision(val action:PokerAction,val target:Int=0,val tell:String,val expression:Int)
internal fun decidePoker(o:PokerObservation,p:PokerPersonality,rng:Random):PokerDecision {
    val unseen=singlePack().filter{it !in o.hole && it !in o.board}
    var equity=0.0
    repeat(36){
        val sample=shuffled(unseen,rng);val board=o.board+List(5-o.board.size){sample.removeAt(sample.lastIndex)}
        val own=pokerRank(o.hole+board);var best=own;var ties=1
        repeat(o.opponents){val rival=pokerRank(List(2){sample.removeAt(sample.lastIndex)}+board);if(rival>best){best=rival;ties=1}else if(rival==best)ties++}
        if(own==best)equity+=1.0/ties
    }
    equity/=36
    val bluff=rng.nextDouble()<p.bluff && equity<.48
    val price=o.toCall.toDouble()/maxOf(1,o.pot+o.toCall)
    val looseness=p.looseness+if(p==PokerPersonality.VETERAN&&o.humanRaises>3).08 else 0.0
    val raise=o.canRaise&&(bluff||equity>.58-looseness/2)&&rng.nextDouble()<p.aggression
    val action=when{raise->PokerAction.RAISE;o.toCall==0->PokerAction.CALL;equity+looseness+(.10*rng.nextDouble())>=price+.09->PokerAction.CALL;else->PokerAction.FOLD}
    val max=o.streetBet+o.stack
    val target= minOf(max,maxOf(o.currentBet+o.minRaise,o.currentBet+(o.pot*(if(bluff).7 else .45+p.aggression*.45)).toInt()))
    val signal=when(p){PokerPersonality.BULLY, PokerPersonality.SHOWBOAT->bluff;PokerPersonality.ROCK->equity>.65;PokerPersonality.GAMBLER->equity in .25.. .55;PokerPersonality.VETERAN->rng.nextBoolean()}
    val shows=rng.nextDouble()<(if(signal).67 else .22)
    val tell=if(shows)p.habit else listOf("Studies the felt","Watches the cards","Counts their chips")[rng.nextInt(3)]
    return PokerDecision(action,target,tell,if(shows)p.ordinal+1 else 0)
}
/** Distributes layered pots; folded money counts, folded seats never win; unmatched excess is returned. */
internal fun pokerPayouts(contributions:List<Int>,folded:List<Boolean>,ranks:List<PokerRank>,button:Int):List<Int> {
    val out=MutableList(contributions.size){0};var previous=0
    for(level in contributions.filter{it>0}.distinct().sorted()){
        val members=contributions.indices.filter{contributions[it]>=level};val amount=(level-previous)*members.size;previous=level
        val eligible=members.filter{!folded[it]}
        if(members.size==1){out[members[0]]+=amount;continue}
        require(eligible.isNotEmpty()){"Pot has no eligible player"}
        val best=eligible.maxOf{ranks[it]};val winners=eligible.filter{ranks[it]==best}.sortedBy{(it-button-1+out.size)%out.size}
        winners.forEachIndexed{i,s->out[s]+=amount/winners.size+if(i<amount%winners.size)1 else 0}
    }
    return out
}
internal class PokerGame(val rng:Random=Random()):Serializable {
    var seats=mutableListOf<PokerSeat>()
    var board= mutableListOf<Card>()
    var deck= mutableListOf<Card>()
    var button=0
    var actor=-1
    var street=0
    var currentBet=0
    var minRaise=10
    var hand=0
    var active=false
    var showdown=false
    var humanRaises=0
    var message="Take a seat • 5 / 10 blinds"
    var log=mutableListOf<String>()
    val pot get()=seats.sumOf{it.total}
    val seated get()=seats.isNotEmpty()
    fun sit(amount:Int,room:Int){
        require(!seated && amount>=100)
        val casts=listOf(listOf("Knuckles","Pearl","Vince"),listOf("Rattle","Velvet","Grin"),listOf("Khepri","Nefra","Seth"),listOf("Rivet","Copper","Baron"),listOf("Tone","Diamond","Ace"),listOf("Riot","Static","Patch"))
        val profiles=listOf(PokerPersonality.BULLY,PokerPersonality.ROCK,PokerPersonality.SHOWBOAT,PokerPersonality.GAMBLER,PokerPersonality.VETERAN)
        seats=mutableListOf(PokerSeat("You",PokerPersonality.VETERAN,amount))
        repeat(3){seats.add(PokerSeat(casts[room][it],profiles[(room+it)%5],500))}
    }
    fun cashOut():Int {require(!active);val amount=seats.firstOrNull()?.stack?:0;seats.clear();board.clear();return amount}
    private fun next(i:Int,predicate:(Int)->Boolean):Int=(1..seats.size).map{(i+it)%seats.size}.firstOrNull(predicate)?:-1
    private fun put(i:Int,amount:Int){val p=seats[i];val n=minOf(amount,p.stack);require(n>=0);p.stack-=n;p.streetBet+=n;p.total+=n}
    fun startHand(){
        if(active || !seated || seats[0].stack<=0)return
        seats.drop(1).filter{it.stack==0}.forEach{it.stack=500} // new virtual buy-in between hands only
        deck=shuffled(singlePack(),rng);board.clear();street=0;currentBet=10;minRaise=10;active=true;showdown=false;hand++;log.clear()
        seats.forEach{it.hole=List(2){deck.removeAt(deck.lastIndex)};it.folded=false;it.streetBet=0;it.total=0;it.acted=false;it.actedAt=0;it.lastAction="";it.tell="Watches the table";it.expression=0}
        button=(button+1)%seats.size
        val sb=next(button){true};val bb=next(sb){true};put(sb,5);put(bb,10);actor=next(bb){seats[it].stack>0};message="Pre-flop • ${seats[actor].name} to act"
    }
    fun toCall(i:Int=actor)=if(i in seats.indices)maxOf(0,currentBet-seats[i].streetBet) else 0
    fun canRaise(i:Int=actor):Boolean {
        if(!active || i !in seats.indices)return false
        val p=seats[i]
        return !p.folded && p.stack>toCall(i) && seats.indices.any{it!=i&&!seats[it].folded&&seats[it].stack>0} && (!p.acted || currentBet-p.actedAt>=minRaise)
    }
    fun act(action:PokerAction,target:Int=0):Boolean {
        if(!active || actor !in seats.indices)return false
        val who=actor;val p=seats[who];val call=toCall()
        if(p.folded||p.stack==0)return false
        when(action){
            PokerAction.FOLD->{p.folded=true;p.lastAction="Fold"}
            PokerAction.CALL->{put(who,call);p.lastAction=if(call==0)"Check" else if(p.stack==0)"All-in call" else "Call $call"}
            PokerAction.RAISE->{
                if(!canRaise())return false
                val cap=p.stack+p.streetBet
                if(target<=currentBet||target>cap||target<currentBet+minRaise&&target!=cap)return false
                val raise=target-currentBet;put(who,target-p.streetBet);if(raise>=minRaise)minRaise=raise;currentBet=target
                p.lastAction=if(p.stack==0)"All-in $target" else "Raise to $target";if(who==0)humanRaises++
            }
        }
        p.acted=true;p.actedAt=currentBet;log.add("${p.name}: ${p.lastAction}");if(log.size>16)log.removeAt(0)
        advance(who);return true
    }
    private fun advance(who:Int){
        val alive=seats.indices.filter{!seats[it].folded}
        if(alive.size==1){val winner=alive[0];val amount=pot;seats[winner].stack+=amount;seats.forEach{it.total=0};active=false;actor=-1;message="${seats[winner].name} wins $amount • everyone else folded";return}
        val actionable=alive.filter{seats[it].stack>0}
        val pending=actionable.filter{seats[it].streetBet<currentBet || !seats[it].acted}
        if(pending.isNotEmpty() && !(actionable.size==1 && toCall(actionable[0])==0)){
            actor=next(who){it in pending};message="${listOf("Pre-flop","Flop","Turn","River")[street]} • ${seats[actor].name} to act";return
        }
        if(street==3){finish();return}
        street++;deck.removeAt(deck.lastIndex);repeat(if(street==1)3 else 1){board.add(deck.removeAt(deck.lastIndex))}
        seats.forEach{it.streetBet=0;it.acted=false;it.actedAt=0};currentBet=0;minRaise=10
        if(actionable.size<=1){advance(button);return}
        actor=next(button){it in actionable};message="${listOf("Pre-flop","Flop","Turn","River")[street]} • ${seats[actor].name} to act"
    }
    private fun finish(){
        val ranks=seats.map{if(it.folded)PokerRank(-1,"Folded") else pokerRank(it.hole+board)}
        val payouts=pokerPayouts(seats.map{it.total},seats.map{it.folded},ranks,button)
        seats.forEachIndexed{i,p->p.stack+=payouts[i];p.total=0;p.lastAction=if(p.folded)"Folded" else ranks[i].title;p.expression=if(payouts[i]>0)3 else 1}
        message=payouts.indices.filter{payouts[it]>0}.joinToString(" • "){"${seats[it].name} +${payouts[it]} (${ranks[it].title})"}
        active=false;showdown=true;actor=-1
    }
    fun observation(i:Int)=PokerObservation(seats[i].hole.toList(),board.toList(),seats.count{!it.folded}-1,pot,toCall(i),seats[i].stack,seats[i].streetBet,currentBet,minRaise,canRaise(i),humanRaises)
    fun botStep(){if(!active||actor<=0)return;val p=seats[actor];val d=decidePoker(observation(actor),p.personality,rng);p.tell=d.tell;p.expression=d.expression;if(!act(d.action,d.target))act(PokerAction.CALL)}
}
