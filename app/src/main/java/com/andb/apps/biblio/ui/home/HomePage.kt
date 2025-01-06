package com.andb.apps.biblio.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.adamglin.PhosphorIcons
import com.adamglin.phosphoricons.Regular
import com.adamglin.phosphoricons.regular.Books
import com.adamglin.phosphoricons.regular.Slidershorizontal
import com.adamglin.phosphoricons.regular.Squaresfour
import com.andb.apps.biblio.data.Book
import com.andb.apps.biblio.data.BooksState
import com.andb.apps.biblio.data.LocalSettings
import com.andb.apps.biblio.ui.common.BiblioButton
import com.andb.apps.biblio.ui.common.BiblioScaffold
import com.andb.apps.biblio.ui.common.ButtonStyle
import com.andb.apps.biblio.ui.common.ExactText
import com.andb.apps.biblio.ui.common.clickableOverlay
import com.andb.apps.biblio.ui.theme.BiblioTheme
import java.text.SimpleDateFormat

@Composable
fun HomePage(
    booksState: BooksState,
    modifier: Modifier = Modifier,
    onNavigateToApps: (editMode: Boolean) -> Unit,
    onNavigateToLibrary: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onRequestStoragePermission: () -> Unit,
    onOpenBook: (Book) -> Unit,
) {
    BiblioScaffold(
        modifier = modifier,
        bottomBar = { HomeBottomBar(onNavigateToApps = onNavigateToApps, onNavigateToSettings = onNavigateToSettings) },
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            when(booksState) {
                BooksState.Loading -> Text(
                    modifier = Modifier.padding(64.dp),
                    text = "Loading...",
                )
                BooksState.NoPermission -> Column(
                    modifier = Modifier.padding(64.dp)
                ) {
                    Text(text = "Biblio needs storage permissions to access your books")
                    BiblioButton(
                        onClick = onRequestStoragePermission,
                        style = ButtonStyle.Outline,
                        text = "Allow storage permissions",
                    )
                }
                is BooksState.Loaded -> when (booksState.allBooks.isNotEmpty()) {
                    true -> Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp)
                            .padding(top = 24.dp, bottom = 0.dp)
                            .weight(1f)
                    ) {
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            val book = booksState.currentlyReading.firstOrNull() ?: booksState.unread.first()
                            BookItem(
                                publication = book,
                                size = BookItemSize.Large,
                                modifier = Modifier
                                    .clickableOverlay { onOpenBook(book) }
                                    .padding(16.dp),
                            )
                        }
                        BoxWithConstraints(
                            Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                val numBooks = (this@BoxWithConstraints.maxWidth / 128.dp).toInt()
                                val slate = booksState.currentlyReading + booksState.unread //TODO: always show one next up?
                                slate.drop(1).take(numBooks).forEach {
                                    Box(
                                        Modifier
                                            .clickableOverlay { onOpenBook(it) }
                                            .weight(1f)
                                            .padding(vertical = 8.dp),
                                    ) {
                                        BookItem(
                                            publication = it,
                                            size = BookItemSize.Medium,
                                            modifier = Modifier.align(Alignment.Center),
                                        )
                                    }
                                }
                                Box(
                                    Modifier
                                        .clickableOverlay { onNavigateToLibrary() }
                                        .weight(1f)
                                        .padding(vertical = 8.dp),
                                ) {
                                    BookItem(
                                        title = "Library",
                                        icon = PhosphorIcons.Regular.Books,
                                        badge = when(LocalSettings.current.common.showNumbers.value) {
                                            true -> "+${booksState.allBooks.size - numBooks - 1}"
                                            false -> null
                                        },
                                        size = BookItemSize.Medium,
                                        modifier = Modifier
                                            .align(Alignment.Center)
                                            .widthIn(min = 64.dp),
                                    )
                                }
                            }
                        }
                    }
                    false -> Text(text = "No books found")
                }
            }
        }
    }
}

@Composable
private fun HomeBottomBar(
    modifier: Modifier = Modifier,
    onNavigateToApps: (editMode: Boolean) -> Unit,
    onNavigateToSettings: () -> Unit,
) {
    val isSettingsPopupOpen = remember { mutableStateOf(false) }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp, horizontal = 32.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val time = currentTimeAsState()
        val formatter = SimpleDateFormat("h:mm a", java.util.Locale.ROOT)
        ExactText(
            text = formatter.format(time.value),
        )

        val batteryState = currentBatteryAsState()
        val wifiState = wifiSignalAsState()
        BiblioButton(
            onClick = { isSettingsPopupOpen.value = true },
            style = ButtonStyle.Outline,
        ) {
            Icon(
                imageVector = PhosphorIcons.Regular.Slidershorizontal,
                contentDescription = "Settings icon",
                modifier = Modifier.size(16.dp)
            )
            Icon(
                imageVector = wifiState.value.toIcon(),
                contentDescription = wifiState.value.strengthDescription(),
                modifier = Modifier.size(16.dp),
            )
            ExactText(
                text = batteryState.value.toPercentString(),
                style = BiblioTheme.typography.caption
            )
            Icon(
                imageVector = batteryState.value.toIcon(),
                contentDescription = null,
                modifier = Modifier.size(16.dp),
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.End,
            modifier = Modifier.weight(1f),
        ) {
            PinnedAppsRow(modifier = Modifier.weight(1f))
            BiblioButton(
                onClick = { onNavigateToApps(false) },
                style = ButtonStyle.Outline,
                text = "Apps",
                icon = PhosphorIcons.Regular.Squaresfour
            )
        }
    }

    if (isSettingsPopupOpen.value) {
        Popup(
            alignment = Alignment.BottomCenter,
            offset = IntOffset(0, with(LocalDensity.current) { -48.dp.roundToPx() }),
            onDismissRequest = { isSettingsPopupOpen.value = false },
            properties = PopupProperties()
        ) {
            HomeSettingsPopup(
                modifier = Modifier.padding(16.dp),
                onOpenSettingsScreen = onNavigateToSettings,
                onOpenPinnedEdit = { onNavigateToApps(true) },
            )
        }
    }
}