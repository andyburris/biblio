package com.andb.apps.biblio.ui.common

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.offset

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

    if(startPx > 0) drawLine(color, Offset(0f, 0f), Offset(0f, size.height), strokeWidth = startPx)
    if(topPx > 0) drawLine(color, Offset(0f, 0f), Offset(size.width, 0f), strokeWidth = topPx)
    if(endPx > 0) drawLine(color, Offset(size.width, 0f), Offset(size.width, size.height), strokeWidth = endPx)
    if(bottomPx > 0) drawLine(color, Offset(0f, size.height), Offset(size.width, size.height), strokeWidth = bottomPx)
}

fun Modifier.skew(xDeg: Double = 0.0, yDeg: Double = 0.0): Modifier = drawWithContent {
    drawContext.canvas.skew(
        Math.toRadians(xDeg).toFloat(),
        Math.toRadians(yDeg).toFloat()
    )
    drawContent()
}

fun Modifier.negativePadding(
    horizontal: Dp = 0.dp,
    vertical: Dp = 0.dp,
) = this.layout { measurable, constraints ->
    val placeable =
        // Step 1
        measurable.measure(constraints.offset(
            horizontal = (-horizontal * 2).roundToPx(),
            vertical = (-vertical * 2).roundToPx()
        ))
    layout(
        width = placeable.width + (horizontal * 2).roundToPx(),
        height = placeable.height + (vertical * 2).roundToPx(),
    ) {
        placeable.place(
            x = 0 + horizontal.roundToPx(),
            y = 0 + vertical.roundToPx(),
        )
    }
}