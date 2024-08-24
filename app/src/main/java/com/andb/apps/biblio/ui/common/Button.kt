package com.andb.apps.biblio.ui.common

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ButtonElevation
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

enum class ButtonStyle {
    Ghost, Outline,
}
@Composable
fun BiblioButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    style: ButtonStyle = ButtonStyle.Ghost,
    contentPadding: PaddingValues = PaddingValues(8.dp),
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    content: @Composable RowScope.() -> Unit

) {
    Button(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        shape = when(style){
            ButtonStyle.Ghost -> RectangleShape
            ButtonStyle.Outline -> CircleShape
        },
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.Transparent,
            contentColor = MaterialTheme.colorScheme.secondary,
            disabledContentColor = MaterialTheme.colorScheme.tertiary,
            disabledContainerColor = Color.Transparent,
        ),
        elevation = ButtonDefaults.buttonElevation(0.dp, 0.dp, 0.dp, 0.dp, 0.dp),
        border = when(style){
            ButtonStyle.Ghost -> null
            ButtonStyle.Outline -> BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
        },
        contentPadding = contentPadding,
        interactionSource = interactionSource,
        content = content
    )
}