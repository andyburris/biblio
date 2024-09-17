package com.andb.apps.biblio.ui.common

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.andb.apps.biblio.ui.theme.BiblioTheme
import com.andb.apps.biblio.ui.theme.BlackoutIndication

enum class ButtonStyle {
    Ghost, Outline,
}

@Composable
fun BiblioButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    text: String? = null,
    icon: ImageVector? = null,
    rightIcon: ImageVector? = null,
    enabled: Boolean = true,
    style: ButtonStyle = ButtonStyle.Ghost,
) {
    BiblioButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        style = style,
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
    ) {
        if(icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(when(style) {
                    ButtonStyle.Ghost -> 20.dp
                    ButtonStyle.Outline -> 16.dp
                })
            )
        }
        if (text != null) {
            ExactText(
                text = text,
                style = when(style) {
                    ButtonStyle.Outline -> BiblioTheme.typography.caption
                    ButtonStyle.Ghost -> BiblioTheme.typography.body
                },
                // modifier = Modifier.border(1.dp, Color.Red),
            )
        }
        if(rightIcon != null) {
            Icon(
                imageVector = rightIcon,
                contentDescription = null,
                modifier = Modifier.size(when(style) {
                    ButtonStyle.Ghost -> 20.dp
                    ButtonStyle.Outline -> 16.dp
                })
            )
        }
    }
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
    val shape = when(style){
        ButtonStyle.Ghost -> RectangleShape
        ButtonStyle.Outline -> CircleShape
    }

    CompositionLocalProvider(LocalContentColor provides BiblioTheme.colors.onBackgroundSecondary) {
        Row(
            modifier = modifier
                .clickable(
                    interactionSource = interactionSource,
                    indication = BlackoutIndication,
                    enabled = enabled,
                    onClick = onClick,
                )
                .minimumInteractiveComponentSize()
                .then(
                    when (style) {
                        ButtonStyle.Ghost -> Modifier
                        ButtonStyle.Outline -> Modifier.border(
                            1.dp,
                            BiblioTheme.colors.divider,
                            shape
                        )
                    }
                )
                .padding(contentPadding),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
            content = content,
        )
    }
}