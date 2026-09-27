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
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import kotlin.math.roundToInt

// The supplied illustration has uneven gutters, not a uniform square grid.
// Keep each original symbol inside its measured region, in RoomStyle order.
private val slotAtlasRegions = listOf(
    listOf(IntRect(0,0,216,199), IntRect(216,0,415,201), IntRect(415,0,633,204), IntRect(633,0,844,199), IntRect(844,0,1060,197)),
    listOf(IntRect(0,199,212,398), IntRect(212,201,423,402), IntRect(423,204,617,396), IntRect(617,199,844,401), IntRect(844,197,1060,402)),
    listOf(IntRect(0,398,200,608), IntRect(200,402,425,604), IntRect(425,396,631,602), IntRect(631,401,830,600), IntRect(830,402,1060,607)),
    listOf(IntRect(0,608,200,810), IntRect(200,604,417,808), IntRect(417,602,621,811), IntRect(621,600,848,808), IntRect(848,619,1060,815)),
    listOf(IntRect(0,810,214,995), IntRect(214,808,408,999), IntRect(408,811,635,998), IntRect(635,808,840,993), IntRect(840,815,1060,1007)),
    listOf(IntRect(0,995,200,1187), IntRect(200,999,433,1201), IntRect(433,998,617,1199), IntRect(617,993,834,1203), IntRect(834,1007,1060,1225)),
    listOf(IntRect(0,1187,216,1435), IntRect(216,1201,410,1435), IntRect(410,1199,617,1435), IntRect(617,1203,834,1435), IntRect(834,1225,1060,1435))
)

@Composable
internal fun SlotEmblem(n: Int, room: RoomStyle, modifier: Modifier) {
    val atlas = ImageBitmap.imageResource(R.drawable.slot_symbols)
    val symbol = slotSymbols(room)[n]
    Canvas(modifier.semantics { contentDescription = "${room.title}: $symbol" }) {
        val region = slotAtlasRegions[room.ordinal][n]
        val scale = minOf(size.width / region.width, size.height / region.height)
        val width = (region.width * scale).roundToInt()
        val height = (region.height * scale).roundToInt()
        drawImage(atlas,
            srcOffset = IntOffset(region.left, region.top),
            srcSize = IntSize(region.width, region.height),
            dstOffset = IntOffset(((size.width - width) / 2).roundToInt(), ((size.height - height) / 2).roundToInt()),
            dstSize = IntSize(width, height), filterQuality = FilterQuality.Medium)
    }
}
