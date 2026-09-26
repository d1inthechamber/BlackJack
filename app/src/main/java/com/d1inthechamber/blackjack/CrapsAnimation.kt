package com.d1inthechamber.blackjack

/** Travel from the pot (0) to the winner's edge (1).
 * Bills remain still until the hand has reached and gripped them. */
internal fun collectionHandDistance(progress: Float): Float = when {
    progress < .20f -> 1f
    progress < .48f -> 1f - (progress - .20f) / .28f
    progress < .60f -> 0f
    else -> ((progress - .60f) / .40f).coerceIn(0f, 1f)
}

internal fun collectionBillDistance(progress: Float): Float =
    if (progress < .60f) 0f else collectionHandDistance(progress)

internal fun collectionDirection(winner: CrapsWinner): Float =
    if (winner == CrapsWinner.OPPONENT) -1f else 1f
