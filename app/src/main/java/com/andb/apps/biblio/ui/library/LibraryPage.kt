package com.andb.apps.biblio.ui.library

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.adamglin.PhosphorIcons
import com.adamglin.phosphoricons.Regular
import com.adamglin.phosphoricons.regular.Caretright
import com.andb.apps.biblio.data.Book
import com.andb.apps.biblio.data.BooksState
import com.andb.apps.biblio.ui.common.BiblioBottomBar
import com.andb.apps.biblio.ui.common.BiblioButton
import com.andb.apps.biblio.ui.common.BiblioPager
import com.andb.apps.biblio.ui.common.BiblioPagerItem
import com.andb.apps.biblio.ui.common.BiblioPagerWidth
import com.andb.apps.biblio.ui.common.BiblioScaffold
import com.andb.apps.biblio.ui.common.ButtonStyle
import com.andb.apps.biblio.ui.common.ExactText
import com.andb.apps.biblio.ui.common.border
import com.andb.apps.biblio.ui.theme.BiblioTheme

enum class LibraryShelf(val title: String) {
    CurrentlyReading("Currently Reading"),
    UpNext("Up Next"),
    DoneOrBackburner("Already Read & Backburner"),
}

@Composable
fun LibraryPage(
    booksState: BooksState.Loaded,
    modifier: Modifier = Modifier,
    onNavigateBack: () -> Unit,
    onOpenShelf: (LibraryShelf) -> Unit,
    onOpenBook: (Book) -> Unit,
) {
    BiblioScaffold(
        modifier = modifier,
        bottomBar = {
            BiblioBottomBar(pageTitle = "Library", onNavigateBack = onNavigateBack)
        }
    ) {
        TempLibraryGrid(
            booksState = booksState,
            onOpenBook = onOpenBook,
            onOpenShelf = onOpenShelf,
        )
    }
//            val headers = listOf(
//                BiblioPagerItem(BiblioPagerWidth.Fill) {
//                    LibrarySectionHeader(
//                        "Currently Reading",
//                        booksState.currentlyReading,
//                        onExpandSection = { onOpenShelf("currentlyReading") },
//                    )
//                },
//                BiblioPagerItem(BiblioPagerWidth.Fill) {
//                    LibrarySectionHeader(
//                        "Up Next",
//                        booksState.unread,
//                        onExpandSection = { onOpenShelf("unread") },
//                    )
//                },
//                BiblioPagerItem(BiblioPagerWidth.Fill) {
//                    LibrarySectionHeader(
//                        "Already Read & Backburner",
//                        booksState.doneOrBackburner,
//                        onExpandSection = { onOpenShelf("doneOrBackburner") },
//                    )
//                },
//            )
//            val items =
//                headers.slice(0..0) +
//                booksState.currentlyReading.map { BiblioPagerItem(BiblioPagerWidth.Fixed()) { LibrarySpine(it) } } +
//                headers.slice(1..1) +
//                booksState.unread.map { BiblioPagerItem(BiblioPagerWidth.Fill) { LibrarySpine(it) } } +
//
//            BiblioPager(
//                items,
//                modifier = modifier,
//                bottomBar = { pagerState ->
//                    BiblioBottomBar(pageTitle = "Library", onNavigateBack = onNavigateBack)
//                }
//            ) {
//                TempLibraryGrid(
//                    booksState = booksState,
//                    onOpenBook = onOpenBook,
//                )
//            }
}

@Composable
private fun TempLibraryGrid(
    booksState: BooksState.Loaded,
    modifier: Modifier = Modifier,
    onOpenShelf: (LibraryShelf) -> Unit,
    onOpenBook: (Book) -> Unit,
) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(200.dp),
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 12.dp)
    ) {
        item(span = { GridItemSpan(this.maxLineSpan) }) {
            LibrarySectionHeader(
                title = "Currently Reading",
                books = booksState.currentlyReading,
                modifier = Modifier.padding(horizontal = 12.dp).padding(top = 16.dp),
                onExpandSection = { onOpenShelf(LibraryShelf.CurrentlyReading) },
            )
        }
        items(booksState.currentlyReading) {
            LibraryItem(
                publication = it,
                modifier = Modifier
                    .clickable { onOpenBook(it) }
                    .padding(horizontal = 12.dp, vertical = 8.dp),
            )
        }

        item(span = { GridItemSpan(this.maxLineSpan) }) {
            LibrarySectionHeader(
                title = "Up Next",
                books = booksState.unread,
                modifier = Modifier.padding(horizontal = 12.dp).padding(top = 16.dp),
                onExpandSection = { onOpenShelf(LibraryShelf.UpNext) },
            )
        }
        items(booksState.unread) {
            LibraryItem(
                publication = it,
                modifier = Modifier
                    .clickable { onOpenBook(it) }
                    .padding(horizontal = 12.dp, vertical = 8.dp),
            )
        }

        item(span = { GridItemSpan(this.maxLineSpan) }) {
            LibrarySectionHeader(
                title = "Already Read & Backburner",
                books = booksState.doneOrBackburner,
                modifier = Modifier.padding(horizontal = 12.dp).padding(top = 16.dp),
                onExpandSection = { onOpenShelf(LibraryShelf.DoneOrBackburner) },
            )
        }
        items(booksState.doneOrBackburner) {
            LibraryItem(
                publication = it,
                modifier = Modifier
                    .clickable { onOpenBook(it) }
                    .padding(horizontal = 12.dp, vertical = 8.dp),
            )
        }
    }
}
@Composable
private fun LibrarySectionHeader(
    title: String,
    books: List<Book>,
    modifier: Modifier = Modifier,
    onExpandSection: () -> Unit,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
    ) {
        ExactText(
            text = title,
            style = BiblioTheme.typography.title,
            color = BiblioTheme.colors.onBackgroundSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        ExactText(
            text = books.size.toString(),
            color = BiblioTheme.colors.onBackgroundSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.weight(1f))
        BiblioButton(
            onClick = onExpandSection,
            style = ButtonStyle.Ghost,
        ) {
            ExactText(text = "More", color = BiblioTheme.colors.onBackgroundSecondary)
            Icon(
                imageVector = PhosphorIcons.Regular.Caretright,
                contentDescription = null,
                tint = BiblioTheme.colors.onBackgroundSecondary,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
private fun LibrarySection(
    title: String,
    books: List<Book>,
    modifier: Modifier = Modifier,
    onOpenBook: (Book) -> Unit,
    onExpandSection: () -> Unit,
) {
    Column(
        modifier =  modifier
            .border(bottom = 1.dp, color = BiblioTheme.colors.divider)
            .padding(bottom = 8.dp)
    ) {
        LibrarySectionHeader(title, books, Modifier.padding(horizontal = 24.dp).padding(top = 16.dp), onExpandSection)
        if(books.isNotEmpty()) {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(200.dp),
                modifier = Modifier
                    .padding(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 16.dp)
            ) {
                items(books) {
                    LibraryItem(
                        publication = it,
                        modifier = Modifier
                            .clickable { onOpenBook(it) }
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                    )
                }
            }
        } else {
            Spacer(Modifier.height(4.dp))
        }
    }
}