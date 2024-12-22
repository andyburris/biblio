package com.andb.apps.biblio.ui.theme

import androidx.compose.foundation.IndicationNodeFactory
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.node.DelegatableNode
import androidx.compose.ui.node.DrawModifierNode
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

private class DarkenIndicationNode(
    private val interactionSource: InteractionSource,
    private val darkenPercent: Float,
) : Modifier.Node(), DrawModifierNode {
    var currentPressPosition: Offset = Offset.Zero
    val pressedState = mutableStateOf(false)

    private suspend fun animateToPressed(pressPosition: Offset) {
        currentPressPosition = pressPosition
        pressedState.value = true
    }

    private suspend fun animateToResting() {
        pressedState.value = false
    }

    override fun onAttach() {
        coroutineScope.launch {
            interactionSource.interactions.collectLatest { interaction ->
                when (interaction) {
                    is PressInteraction.Press -> animateToPressed(interaction.pressPosition)
                    is PressInteraction.Release -> animateToResting()
                    is PressInteraction.Cancel -> animateToResting()
                }
            }
        }
    }

    override fun ContentDrawScope.draw() {
        val darkenMatrix = ColorMatrix().apply {
            setToScale(1f - darkenPercent, 1f - darkenPercent, 1f - darkenPercent, 1f)
//            setToSaturation(0f)
        }
        val darkenFilter = ColorFilter.colorMatrix(darkenMatrix)
        val paint = Paint().apply { colorFilter = darkenFilter }

        val canvasBounds = Rect(Offset(-50f, -50f), Size(size.width + 100f,  size.height + 100f))
        drawIntoCanvas {
            if(pressedState.value) {
                it.saveLayer(canvasBounds, paint)
                this@draw.drawContent()
                it.restore()
            } else {
                this.drawContent()
            }
        }
    }
}

class DarkenIndicationNodeFactory(private val darkenPercent: Float) : IndicationNodeFactory {
    override fun create(interactionSource: InteractionSource): DelegatableNode {
        return DarkenIndicationNode(interactionSource, darkenPercent)
    }

    override fun hashCode(): Int = -1

    override fun equals(other: Any?) = other === this
}

val BlackoutIndication = DarkenIndicationNodeFactory(1f)
val OverlayIndication = DarkenIndicationNodeFactory(0.2f)