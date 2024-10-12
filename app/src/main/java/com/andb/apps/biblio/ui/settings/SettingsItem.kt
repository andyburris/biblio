package com.andb.apps.biblio.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.adamglin.PhosphorIcons
import com.adamglin.phosphoricons.Regular
import com.adamglin.phosphoricons.regular.Caretright
import com.andb.apps.biblio.data.SettingState
import com.andb.apps.biblio.data.ToggleableSettingState
import com.andb.apps.biblio.ui.common.BiblioButton
import com.andb.apps.biblio.ui.common.ExactText
import com.andb.apps.biblio.ui.common.border
import com.andb.apps.biblio.ui.theme.BiblioTheme

@Composable
fun <T> SettingsPopupItem(
    settingState: SettingState<T>,
    modifier: Modifier = Modifier,
    onMore: ((SettingState<T>) -> Unit)? = null,
) {
    SettingsPopupItem(
        title = settingState.name,
        icon = settingState.icon,
        isActivated = settingState.isActivated,
        modifier = modifier,
        state = settingState.stateDescription,
        onMore = onMore?.let { { it(settingState) } },
        onToggle = (settingState as? ToggleableSettingState)?.let { { it.toggle() } },
    )
}

@Composable
fun SettingsPopupItem(
    title: String,
    icon: ImageVector,
    isActivated: Boolean?,
    modifier: Modifier = Modifier,
    state: String? = null,
    onMore: (() -> Unit)? = null,
    onToggle: (() -> Unit)? = onMore,
) {
    Row(
        modifier = modifier
            .border(BiblioTheme.colors.divider, bottom = 1.dp, end = 1.dp)
            .clickable(onClick = onToggle ?: onMore ?: {})
            .padding(start = 16.dp, top = 12.dp, bottom = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = BiblioTheme.colors.onBackgroundSecondary,
            modifier = Modifier
                .then(
                    when (isActivated) {
                        true -> Modifier.background(BiblioTheme.colors.surface, shape = CircleShape)
                        false -> Modifier.border(
                            1.dp,
                            color = BiblioTheme.colors.divider,
                            shape = CircleShape
                        )

                        null -> Modifier
                    }
                )
                .padding(6.dp)
                .size(20.dp)
        )
        Column(
            modifier = Modifier.weight(1f),
        ) {
            ExactText(
                text = title,
                maxLines = 1,
            )
            if(state != null) {
                ExactText(
                    text = state,
                    color = BiblioTheme.colors.onBackgroundSecondary,
                    maxLines = 1,
                )
            }
        }
        if(onMore != null) {
            BiblioButton(
                icon = PhosphorIcons.Regular.Caretright,
                onClick = onMore,
            )
        }
    }
}

data class SettingsSegment(
    val key: String,
    val title: String,
    val icon: ImageVector,
)
@Composable
fun SettingsSegmentItem(
    title: String,
    segments: List<SettingsSegment>,
    selectedSegmentKey: String,
    modifier: Modifier = Modifier,
    onSelect: (SettingsSegment) -> Unit,
    onMore: () -> Unit,
) {
    Row(
        modifier = modifier
            .border(BiblioTheme.colors.divider, bottom = 1.dp),
    ) {
        segments.forEach { segment ->
            val isSelected = segment.key == selectedSegmentKey
            Row(
                modifier = Modifier
                    .border(BiblioTheme.colors.divider, end = 1.dp)
                    .clickable(onClick = { onSelect(segment) })
                    .background(if (isSelected) BiblioTheme.colors.surface else Color.Transparent)
                    .height(48.dp)
                    .weight(1f)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = segment.icon,
                    contentDescription = null,
                    tint = when(isSelected) {
                        true -> BiblioTheme.colors.onBackground
                        false -> BiblioTheme.colors.onBackgroundSecondary
                    },
                    modifier = Modifier.size(20.dp),
                )
            }
        }
        BiblioButton(
            onClick = onMore,
            icon = PhosphorIcons.Regular.Caretright,
            modifier = Modifier,
        )
    }
}

@Composable
fun <T> SettingsPageItem(
    settingState: SettingState<T>,
    modifier: Modifier = Modifier,
    onMore: ((SettingState<T>) -> Unit)? = null,
    onToggle: ((SettingState<T>) -> Unit)? = onMore,
    widget: @Composable () -> Unit = {},
    content: @Composable () -> Unit = {},
) = SettingsPageItem(
    title = settingState.name,
    icon = settingState.icon,
    modifier = modifier,
    isActivated = settingState.isActivated,
    state = settingState.stateDescription,
    onMore = onMore?.let{ { it(settingState) } },
    onToggle = onToggle?.let{ { it(settingState) } },
    widget = widget,
    content = content,
)

@Composable
fun SettingsPageItem(
    title: String,
    icon: ImageVector,
    isActivated: Boolean?,
    modifier: Modifier = Modifier,
    state: String? = null,
    onMore: (() -> Unit)? = null,
    onToggle: (() -> Unit)? = onMore,
    widget: @Composable () -> Unit = {},
    content: @Composable () -> Unit = {},
) {
    Column(
        modifier = modifier
            .border(BiblioTheme.colors.divider, bottom = 1.dp, end = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .clickable(onClick = onToggle ?: onMore ?: {})
                .padding(start = 16.dp, top = 12.dp, bottom = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = BiblioTheme.colors.onBackgroundSecondary,
                modifier = Modifier
                    .then(
                        when (isActivated) {
                            true -> Modifier.background(
                                BiblioTheme.colors.surface,
                                shape = CircleShape
                            )

                            false -> Modifier.border(
                                1.dp,
                                color = BiblioTheme.colors.divider,
                                shape = CircleShape
                            )

                            null -> Modifier
                        }
                    )
                    .padding(6.dp)
                    .size(20.dp)
            )
            Column(
                modifier = Modifier.weight(1f),
            ) {
                ExactText(
                    text = title,
                    maxLines = 1,
                )
                if(state != null) {
                    ExactText(
                        text = state,
                        color = BiblioTheme.colors.onBackgroundSecondary,
                        maxLines = 1,
                    )
                }
            }
            if(onMore != null) {
                BiblioButton(
                    icon = PhosphorIcons.Regular.Caretright,
                    onClick = onMore,
                )
            }
        }
        content()
    }
}