package com.andb.apps.biblio.ui.library

import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.andb.apps.biblio.data.LocalSettings
import com.andb.apps.biblio.ui.settings.SettingsPopup
import com.andb.apps.biblio.ui.settings.SettingsPopupHeader
import com.andb.apps.biblio.ui.settings.SettingsPopupItem

@Composable
fun LibrarySettingsPopup(
    modifier: Modifier = Modifier,
    onOpenSettingsScreen: () -> Unit,
) {
    val settings = LocalSettings.current
    SettingsPopup(modifier) {
        item(span = { GridItemSpan(maxCurrentLineSpan) }) {
            SettingsPopupHeader("Biblio Settings • Library", onMore = onOpenSettingsScreen)
        }
        item { SettingsPopupItem(settings.library.view) }
    }
}