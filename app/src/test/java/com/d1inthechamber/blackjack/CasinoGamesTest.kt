package com.d1inthechamber.blackjack

import org.junit.Assert.*
import org.junit.Test
import java.util.Random
import java.io.*

class CasinoGamesTest {
    private fun cards(s:String)=s.split(" ").map{Card(it.dropLast(1),it.takeLast(1))}
    @Test fun slotsExhaustiveOddsAndPayouts(){
        var total=0
        for(a in SlotsGame.strip)for(b in SlotsGame.strip)for(c in SlotsGame.strip)total+=SlotsGame.multiplier(listOf(a,b,c))
        assertEquals(.91925,total/8000.0,.0000001)
        assertEquals(60,SlotsGame.multiplier(listOf(4,4,4)))
        assertEquals(2,SlotsGame.multiplier(listOf(0,1,0)))
        assertEquals(0,SlotsGame.multiplier(listOf(1,1,0)))
    }
    @Test fun solitaireDealHasEveryCardAndDrawThreeRecyclesInOrder(){
        val g=SolitaireGame(3,Random(1));assertEquals(24,g.stock.size);assertEquals(52,(g.stock+g.columns.flatten()).distinct().size)
        assertEquals((1..7).toList(),g.columns.map{it.size});assertEquals((0..6).toList(),g.hidden)
        val before=g.snapshot();g.draw();assertEquals(3,g.waste.size);assertEquals(before.stock.takeLast(3).reversed(),g.waste)
        assertTrue(g.undo());assertEquals(before,g.snapshot())
        repeat(8){g.draw()};val waste=g.waste.toList();g.draw();assertEquals(waste.reversed(),g.stock)
    }
    @Test fun solitaireRejectsIllegalMovesFlipsAndUndoes(){
        val g=SolitaireGame();g.columns=MutableList(7){mutableListOf()};g.hidden=MutableList(7){0};g.waste=cards("5♥").toMutableList()
        g.columns[0].add(Card("6","♠"));g.columns[1].add(Card("6","♦"))
        assertFalse(g.move(SolitairePick(-1,0),1));assertFalse(g.move(SolitairePick(-1,0),2));assertTrue(g.move(SolitairePick(-1,0),0));assertTrue(g.undo())
        g.columns[2]=cards("Q♠ K♥").toMutableList();g.hidden[2]=1
        assertTrue(g.move(SolitairePick(2,1),3));assertEquals(0,g.hidden[2]);assertTrue(g.undo());assertEquals(1,g.hidden[2])
        g.waste=cards("A♠").toMutableList();assertTrue(g.move(SolitairePick(-1,0),7));g.waste=cards("2♥").toMutableList();assertFalse(g.move(SolitairePick(-1,0),7))
    }
    @Test fun solitaireDetectsDeadDealsButNotReachableStockMoves(){
        val g=SolitaireGame()
        g.stock.clear();g.waste.clear();g.foundations.forEach{it.clear()}
        g.columns=MutableList(7){mutableListOf(Card("2","♠"))};g.hidden=MutableList(7){0}
        assertTrue(g.stuck);assertFalse(g.hasAvailableMove())

        g.columns.forEach{it.clear()};g.waste.add(Card("K","♠"))
        assertFalse(g.stuck);assertTrue(g.hasAvailableMove())
    }
    @Test fun evaluatorOrdersCategoriesAndHandlesWheelAndBoardTies(){
        val examples=listOf("A♠ J♦ 9♥ 7♣ 2♠","A♠ A♦ 9♥ 7♣ 2♠","A♠ A♦ 9♥ 9♣ 2♠","A♠ A♦ A♥ 7♣ 2♠","A♠ 2♦ 3♥ 4♣ 5♠","A♠ J♠ 9♠ 7♠ 2♠","A♠ A♦ A♥ 7♣ 7♠","A♠ A♦ A♥ A♣ 2♠","9♠ 10♠ J♠ Q♠ K♠")
        val ranks=examples.map{pokerRank(cards(it))};assertTrue(ranks.zipWithNext().all{it.first<it.second})
        assertTrue(pokerRank(cards("2♠ 3♦ 4♥ 5♣ 6♠"))>ranks[4])
        val board=cards("10♠ J♠ Q♠ K♠ A♠")
        assertEquals(pokerRank(board+cards("2♥ 3♦")),pokerRank(board+cards("9♥ 9♦")))
        assertEquals("Full house",pokerRank(cards("A♠ A♦ A♥ K♣ K♠ K♥ 2♦")).title)
    }
    @Test fun sidePotsTiesFoldedMoneyAndUnmatchedExcess(){
        val ranks=listOf(PokerRank(3,"x"),PokerRank(2,"x"),PokerRank(1,"x"),PokerRank(0,"x"))
        assertEquals(listOf(200,300,200,0),pokerPayouts(listOf(50,150,300,200),listOf(false,false,false,true),ranks,0))
        assertEquals(listOf(1,2,0),pokerPayouts(listOf(1,1,1),listOf(false,false,true),List(3){PokerRank(1,"tie")},0))
        assertEquals(listOf(20,90),pokerPayouts(listOf(10,100),listOf(false,false),ranks.take(2),0))
    }
    @Test fun shortAllInDoesNotReopenBetting(){
        val g=PokerGame(Random(3));g.sit(500,0);g.startHand()
        g.actor=0;g.currentBet=100;g.minRaise=80
        g.seats.forEach{it.stack=400;it.streetBet=100;it.total=100;it.acted=true;it.actedAt=100}
        g.seats[0].stack=30;g.seats[0].acted=false
        assertTrue(g.act(PokerAction.RAISE,130));assertEquals(1,g.actor);assertFalse(g.canRaise(1));assertFalse(g.act(PokerAction.RAISE,210));assertTrue(g.act(PokerAction.CALL))
    }
    @Test fun pokerAllCheckCallHandsConserveChipsAndEnd(){
        val g=PokerGame(Random(4));g.sit(500,0)
        repeat(12){g.startHand();val total=g.seats.sumOf{it.stack}+g.pot;var turns=0
            while(g.active){assertTrue(g.act(PokerAction.CALL));assertEquals(total,g.seats.sumOf{it.stack}+g.pot);assertTrue(++turns<25)}
            assertEquals(5,g.board.size);assertEquals(0,g.pot)
        }
    }
    @Test fun botCannotUseHiddenCardsAndPersonalityChangesBehaviour(){
        val g=PokerGame(Random(5));g.sit(500,0);g.startHand();val a=g.observation(1)
        g.seats[0].hole=cards("A♠ A♥");g.deck.reverse();assertEquals(a,g.observation(1))
        assertEquals(decidePoker(a,PokerPersonality.BULLY,Random(7)),decidePoker(g.observation(1),PokerPersonality.BULLY,Random(7)))
        assertTrue(PokerPersonality.BULLY.bluff>PokerPersonality.ROCK.bluff)
    }
    @Test fun simulatedBotTablesConserveChipsEvenWithAllIns(){
        repeat(8){seed->val g=PokerGame(Random(seed.toLong()));g.sit(500,seed%6);g.startHand();val total=g.seats.sumOf{it.stack}+g.pot;var actions=0
            while(g.active){if(g.actor==0){if(g.canRaise() && actions%3==0)g.act(PokerAction.RAISE,g.seats[0].streetBet+g.seats[0].stack)else g.act(PokerAction.CALL)}else g.botStep()
                assertEquals(total,g.seats.sumOf{it.stack}+g.pot);assertTrue(++actions<100)}
        }
    }
    @Test fun completeCasinoStateSerializesWithoutLosingDeckOrWallet(){
        val c=CasinoState();c.poker.sit(500,5);c.poker.startHand();c.solitaire.draw();c.slots.spin();c.lastGame="POKER"
        val b=BlackjackState();b.bankroll=500.5
        val archive=CasinoArchive(blackjack=b.savedGame(),casino=c)
        val bytes=ByteArrayOutputStream().also{ObjectOutputStream(it).use{it.writeObject(archive)}}.toByteArray()
        val restored=ObjectInputStream(ByteArrayInputStream(bytes)).use{it.readObject() as CasinoArchive}
        assertEquals(500.5,restored.blackjack.bankroll,0.0);assertEquals(c.poker.deck,restored.casino.poker.deck);assertEquals(c.solitaire.snapshot(),restored.casino.solitaire.snapshot());assertEquals(c.slots.reels,restored.casino.slots.reels)
    }
}
