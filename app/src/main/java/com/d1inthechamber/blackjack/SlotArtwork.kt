package com.d1inthechamber.blackjack

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import kotlin.math.roundToInt

/** The supplied atlas contains five symbols per room, in RoomStyle order. */
@Composable
internal fun SlotEmblem(n: Int, room: RoomStyle, modifier: Modifier) {
    val atlas = ImageBitmap.imageResource(R.drawable.slot_symbols)
    val symbol = slotSymbols(room)[n]
    Canvas(modifier.semantics { contentDescription = "${room.title}: $symbol" }) {
        val cell = atlas.width / 5
        val side = size.minDimension.roundToInt()
        drawImage(atlas,
            srcOffset = IntOffset(n * cell, room.ordinal * cell),
            srcSize = IntSize(cell, cell),
            dstOffset = IntOffset(((size.width - side) / 2).roundToInt(), ((size.height - side) / 2).roundToInt()),
            dstSize = IntSize(side, side), filterQuality = FilterQuality.Medium)
    }
}
