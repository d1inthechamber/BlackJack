package com.d1inthechamber.blackjack

import java.io.Serializable
import java.util.Random

internal enum class CrapsWinner : Serializable { NONE, PLAYER, OPPONENT }

internal data class CrapsOutcome(
    val dieOne: Int,
    val dieTwo: Int,
    val point: Int,
    val winner: CrapsWinner,
    val payout: Int
) : Serializable

/**
 * Pass-line street craps against one character who covers the player's stake.
 * The player's wager and the matching character wager stay in the centre until
 * the shooter makes the point or sevens out.
 */
internal class CrapsGame(private val rng: Random = Random()) : Serializable {
    var bet = 25
    var point = 0
    var dieOne = 1
    var dieTwo = 1
    var handNumber = 0
    var rolls = 0
    var active = false
    var winner = CrapsWinner.NONE
    var centerPot = 0
    var collectionPulse = 0
    var message = "Choose a bill, then tap the hand to shoot."

    fun setBet(amount: Int): Boolean {
        if (active || winner != CrapsWinner.NONE || amount !in listOf(10, 25, 50, 100)) return false
        bet = amount
        return true
    }

    /** Returns the amount to remove from the shared wallet. */
    fun begin(): Int {
        if (active || winner != CrapsWinner.NONE) return 0
        active = true
        point = 0
        centerPot = bet * 2
        handNumber++
        message = "Come-out roll • 7 or 11 wins • 2, 3 or 12 loses"
        return bet
    }

    fun roll(first: Int = rng.nextInt(6) + 1, second: Int = rng.nextInt(6) + 1): CrapsOutcome {
        require(first in 1..6 && second in 1..6)
        if (!active) return CrapsOutcome(dieOne, dieTwo, point, winner, 0)
        dieOne = first
        dieTwo = second
        rolls++
        val total = first + second
        var payout = 0
        if (point == 0) {
            when (total) {
                7, 11 -> {
                    winner = CrapsWinner.PLAYER
                    payout = centerPot
                    message = "$total on the come-out • You win \$${centerPot}"
                }
                2, 3, 12 -> {
                    winner = CrapsWinner.OPPONENT
                    message = "Craps $total • The character wins the pile"
                }
                else -> {
                    point = total
                    message = "Point is $point • Make $point before a 7"
                }
            }
        } else {
            when (total) {
                point -> {
                    winner = CrapsWinner.PLAYER
                    payout = centerPot
                    message = "Point $point made • You win \$${centerPot}"
                }
                7 -> {
                    winner = CrapsWinner.OPPONENT
                    message = "Seven-out • The character wins the pile"
                }
                else -> message = "$total • Point stays $point"
            }
        }
        if (winner != CrapsWinner.NONE) {
            active = false
            collectionPulse++
        }
        return CrapsOutcome(first, second, point, winner, payout)
    }

    fun finishCollection() {
        if (winner == CrapsWinner.NONE) return
        winner = CrapsWinner.NONE
        centerPot = 0
        point = 0
        message = "Choose a bill, then tap the hand to shoot."
    }

    companion object {
        private const val serialVersionUID = 1L
    }
}
