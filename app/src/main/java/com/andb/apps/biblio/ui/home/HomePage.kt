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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.adamglin.PhosphorIcons
import com.adamglin.phosphoricons.Regular
import com.adamglin.phosphoricons.regular.Books
import com.adamglin.phosphoricons.regular.Squaresfour
import com.andb.apps.biblio.data.BooksState
import com.andb.apps.biblio.ui.common.BiblioButton
import com.andb.apps.biblio.ui.common.ButtonStyle
import com.andb.apps.biblio.ui.common.ExactText
import org.readium.r2.shared.publication.Publication
import java.text.SimpleDateFormat

@Composable
fun HomePage(
    booksState: BooksState,
    modifier: Modifier = Modifier,
    onNavigateToApps: () -> Unit,
    onNavigateToLibrary: () -> Unit,
    onRequestStoragePermission: () -> Unit,
    onOpenPublication: (Publication) -> Unit,
) {
    Column(modifier = modifier.fillMaxSize()) {
        Column(
            Modifier
                .fillMaxWidth()
                .weight(1f),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            when(val publications = booksState) {
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
                is BooksState.Loaded -> when(publications.books.isNotEmpty()) {
                    true -> Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(horizontal = 32.dp)
                            .padding(top = 32.dp, bottom = 8.dp)
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
                                publication = publications.books.first(),
                                size = BookItemSize.Large,
                                modifier = Modifier.clickable {
                                    onOpenPublication(publications.books.first())
                              },
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
                                val numBooks = this@BoxWithConstraints.maxWidth / 128.dp
                                publications.books.drop(1).take(numBooks.toInt()).forEach {
                                    BookItem(
                                        publication = it,
                                        size = BookItemSize.Medium,
                                        modifier = Modifier.clickable { onOpenPublication(it) },
                                    )
                                }
                                BookItem(
                                    title = "Library",
                                    icon = PhosphorIcons.Regular.Books,
                                    size = BookItemSize.Medium,
                                    modifier = Modifier.clickable { onNavigateToLibrary() },
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
            BiblioButton(
                onClick = { /*TODO*/ },
                style = ButtonStyle.Outline,
                text = when(val percent = batteryState.value.percent) {
                    null -> "..."
                    else -> "${Math.round(percent * 100)}%"
                },
            )

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