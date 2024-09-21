package com.andb.apps.biblio.ui.common

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.offset
import com.andb.apps.biblio.ui.theme.OverlayIndication
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

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

@Composable
fun Modifier.clickableOverlay(onClick: () -> Unit) = this.clickable(
    interactionSource = remember { MutableInteractionSource() },
    indication = OverlayIndication,
    onClick = onClick,
)

fun Modifier.rotateWithBounds(degrees: Float) = this.layout { measurable, constraints ->
    val angle = Math.toRadians(degrees.toDouble())
    val rotatedConstraints = constraints.copy(
        minWidth = (constraints.minWidth * abs(cos(angle)) + constraints.minHeight * abs(sin(angle))).toInt(),
        minHeight = (constraints.minWidth * abs(sin(angle)) + constraints.minHeight * abs(cos(angle))).toInt(),
        maxWidth = (constraints.maxWidth * abs(cos(angle)) + constraints.maxHeight * abs(sin(angle))).toInt(),
        maxHeight = (constraints.maxWidth * abs(sin(angle)) + constraints.maxHeight * abs(cos(angle))).toInt()
    )
    val placeable = measurable.measure(rotatedConstraints)
    val rotatedHeight = (placeable.width * Math.abs(Math.sin(angle)) + placeable.height * Math.abs(Math.cos(angle))).toFloat()
    val rotatedWidth = (placeable.width * Math.abs(Math.cos(angle)) + placeable.height * Math.abs(Math.sin(angle))).toFloat()
    layout(width = rotatedWidth.toInt(), height = rotatedHeight.toInt()) {
        placeable.placeRelative(
            x = ((rotatedWidth - placeable.width) / 2).toInt(),
            y = ((rotatedHeight - placeable.height) / 2).toInt()
        )
    }
}.rotate(degrees)