package com.andb.apps.biblio.ui.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.adamglin.PhosphorIcons
import com.adamglin.phosphoricons.Regular
import com.adamglin.phosphoricons.regular.Batterycharging
import com.adamglin.phosphoricons.regular.Batteryempty
import com.adamglin.phosphoricons.regular.Batteryfull
import com.adamglin.phosphoricons.regular.Batteryhigh
import com.adamglin.phosphoricons.regular.Batterylow
import com.adamglin.phosphoricons.regular.Batterymedium
import com.adamglin.phosphoricons.regular.Books
import com.adamglin.phosphoricons.regular.Slidershorizontal
import com.adamglin.phosphoricons.regular.Squaresfour
import com.adamglin.phosphoricons.regular.Wifihigh
import com.adamglin.phosphoricons.regular.Wifilow
import com.adamglin.phosphoricons.regular.Wifimedium
import com.adamglin.phosphoricons.regular.Wifinone
import com.andb.apps.biblio.data.Book
import com.andb.apps.biblio.data.BooksState
import com.andb.apps.biblio.ui.common.BiblioButton
import com.andb.apps.biblio.ui.common.ButtonStyle
import com.andb.apps.biblio.ui.common.ExactText
import com.andb.apps.biblio.ui.theme.BiblioTheme
import java.text.SimpleDateFormat

@Composable
fun HomePage(
    booksState: BooksState,
    modifier: Modifier = Modifier,
    onNavigateToApps: () -> Unit,
    onNavigateToLibrary: () -> Unit,
    onRequestStoragePermission: () -> Unit,
    onOpenBook: (Book) -> Unit,
) {
    Column(modifier = modifier.fillMaxSize()) {
        Column(
            Modifier
                .fillMaxWidth()
                .weight(1f),
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
                            BookItem(
                                publication = booksState.currentlyReading.first(),
                                size = BookItemSize.Large,
                                modifier = Modifier
                                    .clickable { onOpenBook(booksState.currentlyReading.first()) }
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
                                    BookItem(
                                        publication = it,
                                        size = BookItemSize.Medium,
                                        modifier = Modifier
                                            .clickable { onOpenBook(it) }
                                            .padding(8.dp),
                                    )
                                }
                                BookItem(
                                    title = "Library",
                                    icon = PhosphorIcons.Regular.Books,
//                                    badge = "+${publications.books.size - numBooks - 1}",
                                    size = BookItemSize.Medium,
                                    modifier = Modifier
                                        .clickable { onNavigateToLibrary() }
                                        .padding(8.dp),
                                )
                            }
                        }
                    }

                    false -> Text(text = "No books found")
                }
            }
        }
        Row(
            modifier = Modifier
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
                onClick = { /*TODO*/ },
                style = ButtonStyle.Outline,
            ) {
                Icon(
                    imageVector = PhosphorIcons.Regular.Slidershorizontal,
                    contentDescription = "Settings icon",
                    modifier = Modifier.size(16.dp)
                )
                Icon(
                    imageVector = when(wifiState.value) {
                        0 -> PhosphorIcons.Regular.Wifinone
                        1 -> PhosphorIcons.Regular.Wifilow
                        2 -> PhosphorIcons.Regular.Wifimedium
                        else -> PhosphorIcons.Regular.Wifihigh
                    },
                    contentDescription = "Wifi strength is ${when(wifiState.value) {
                        0 -> "very low"
                        1 -> "low"
                        2 -> "medium"
                        else -> "strong"
                    }}",
                    modifier = Modifier.size(16.dp),
                )
                ExactText(
                    when(val percent = batteryState.value.percent) {
                        null -> "..."
                        else -> "${Math.round(percent * 100)}%"
                    },
                    style = BiblioTheme.typography.caption
                )
                Icon(
                    imageVector = when(batteryState.value.isCharging) {
                        true -> PhosphorIcons.Regular.Batterycharging
                        false -> when(val percent = batteryState.value.percent) {
                            null -> PhosphorIcons.Regular.Batterycharging
                            in 0.85f..1.0f-> PhosphorIcons.Regular.Batteryfull
                            in 0.65f..0.85f -> PhosphorIcons.Regular.Batteryhigh
                            in 0.4f..0.65f -> PhosphorIcons.Regular.Batterymedium
                            in 0.05f..0.4f -> PhosphorIcons.Regular.Batterylow
                            else -> PhosphorIcons.Regular.Batteryempty
                        }
                    },
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            BiblioButton(
                onClick = onNavigateToApps,
                style = ButtonStyle.Outline,
                text = "Apps",
                icon = PhosphorIcons.Regular.Squaresfour
            )
        }
    }
}