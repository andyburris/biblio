package com.andb.apps.biblio.ui.common

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

data class BorderSides(
    val top: Boolean = false,
    val right: Boolean = false,
    val bottom: Boolean = false,
    val left: Boolean = false,
)

fun Modifier.border(
    color: Color,
    start: Dp = 0.dp,
    top: Dp = 0.dp,
    end: Dp = 0.dp,
    bottom: Dp = 0.dp,
) = this.drawBehind {
    val startPx = start.toPx()
    val topPx = top.toPx()
    val endPx = end.toPx()
    val bottomPx = bottom.toPx()
    drawLine(color, start = Offset(startPx, topPx), end = Offset(size.width - endPx, topPx))
    drawLine(color, start = Offset(size.width - endPx, topPx), end = Offset(size.width - endPx, size.height - bottomPx))
    drawLine(color, start = Offset(size.width - endPx, size.height - bottomPx), end = Offset(startPx, size.height - bottomPx))
    drawLine(color, start = Offset(startPx, size.height - bottomPx), end = Offset(startPx, topPx))
}