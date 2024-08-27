package com.andb.apps.biblio.ui.common

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.adamglin.PhosphorIcons
import com.adamglin.phosphoricons.Regular
import com.adamglin.phosphoricons.regular.Arrowleft
import com.andb.apps.biblio.ui.theme.BiblioTheme

@Composable
fun BiblioScaffold(
    modifier: Modifier = Modifier,
    bottomBar: (@Composable () -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    Column(modifier) {
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
        ) {
            content()
        }
        bottomBar?.invoke()
    }
}

@Composable
fun BiblioBottomBar(
    pageTitle: String,
    modifier: Modifier = Modifier,
    onNavigateBack: () -> Unit,
    content: @Composable () -> Unit = {},
) {
    Row(
        modifier = modifier
            .border(BiblioTheme.colors.divider, top = 1.dp)
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        BiblioButton(
            onClick = onNavigateBack,
            style = ButtonStyle.Ghost,
            text = pageTitle,
            icon = PhosphorIcons.Regular.Arrowleft
        )
        content()
    }
}