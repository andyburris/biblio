package com.andb.apps.biblio.ui.library

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.adamglin.PhosphorIcons
import com.adamglin.phosphoricons.Regular
import com.adamglin.phosphoricons.regular.Caretright
import com.adamglin.phosphoricons.regular.Slidershorizontal
import com.andb.apps.biblio.LibraryView
import com.andb.apps.biblio.data.Book
import com.andb.apps.biblio.data.BooksState
import com.andb.apps.biblio.data.LocalSettings
import com.andb.apps.biblio.ui.common.BiblioBottomBar
import com.andb.apps.biblio.ui.common.BiblioButton
import com.andb.apps.biblio.ui.common.BiblioScaffold
import com.andb.apps.biblio.ui.common.ButtonStyle
import com.andb.apps.biblio.ui.common.ExactText
import com.andb.apps.biblio.ui.common.border
import com.andb.apps.biblio.ui.common.pager.BiblioPager
import com.andb.apps.biblio.ui.common.pager.BiblioPagerItem
import com.andb.apps.biblio.ui.common.pager.BiblioPagerWidth
import com.andb.apps.biblio.ui.common.rotateWithBounds
import com.andb.apps.biblio.ui.settings.SettingsPopup
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
    onOpenSettings: () -> Unit,
) {
    val isPopupOpen = remember { mutableStateOf(false) }
    val bottomBar: @Composable () -> Unit = { BiblioBottomBar(pageTitle = "Library", onNavigateBack = onNavigateBack) {
        if (isPopupOpen.value) {
            Popup(
                alignment = Alignment.BottomCenter,
                offset = IntOffset(0, with(LocalDensity.current) { -48.dp.roundToPx() }),
                onDismissRequest = { isPopupOpen.value = false },
                properties = PopupProperties()
            ) {
                LibrarySettingsPopup(Modifier.padding(16.dp), onOpenSettings)
            }
        }
        BiblioButton(
            style = ButtonStyle.Outline,
            icon = PhosphorIcons.Regular.Slidershorizontal,
            onClick = { isPopupOpen.value = true }
        )
    } }
    when(LocalSettings.current.settings.library.view) {
        LibraryView.LIBRARY_VIEW_GRID -> BiblioScaffold(
            modifier = modifier,
            bottomBar = bottomBar,
            content = { TempLibraryGrid(booksState, onOpenBook = onOpenBook, onOpenShelf = onOpenShelf) }
        )
        else -> LibraryShelves(
            booksState = booksState,
            modifier = modifier,
            bottomBar = bottomBar,
            onOpenShelf = onOpenShelf,
            onOpenBook = onOpenBook,
        )
    }
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
                modifier = Modifier
                    .padding(horizontal = 12.dp)
                    .padding(top = 16.dp),
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
                modifier = Modifier
                    .padding(horizontal = 12.dp)
                    .padding(top = 16.dp),
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
                modifier = Modifier
                    .padding(horizontal = 12.dp)
                    .padding(top = 16.dp),
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
private fun LibraryShelves(
    booksState: BooksState.Loaded,
    modifier: Modifier = Modifier,
    bottomBar: @Composable () -> Unit,
    onOpenShelf: (LibraryShelf) -> Unit,
    onOpenBook: (Book) -> Unit,
) {
    BiblioPager(
        modifier = modifier,
        items = listOf(
            BiblioPagerItem(width = BiblioPagerWidth.Fill(), content = {
                LibrarySection(
                    title = "Currently Reading",
                    books = booksState.currentlyReading,
                    width = it.containerSize.width,
                    onOpenBook = onOpenBook,
                    onExpandSection = { onOpenShelf(LibraryShelf.CurrentlyReading) },
                )
            }),
            BiblioPagerItem(width = BiblioPagerWidth.Fill(), content = {
                LibrarySection(
                    title = "Up Next",
                    books = booksState.unread,
                    width = it.containerSize.width,
                    onOpenBook = onOpenBook,
                    onExpandSection = { onOpenShelf(LibraryShelf.UpNext) },
                )
            }),
            BiblioPagerItem(width = BiblioPagerWidth.Fill(), content = {
                LibrarySection(
                    title = "Already Read & Backburner",
                    books = booksState.doneOrBackburner,
                    width = it.containerSize.width,
                    onOpenBook = onOpenBook,
                    onExpandSection = { onOpenShelf(LibraryShelf.DoneOrBackburner) },
                )
            }),
        ),
        minRowHeight = 200.dp,
        bottomBar = { bottomBar() },
    )
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
    width: Dp,
    modifier: Modifier = Modifier,
    onOpenBook: (Book) -> Unit,
    onExpandSection: () -> Unit,
) {
    Column(
        modifier = modifier
            .border(bottom = 1.dp, color = BiblioTheme.colors.divider)
    ) {
        LibrarySectionHeader(title, books,
            Modifier
                .padding(horizontal = 24.dp)
                .padding(top = 8.dp, bottom = 4.dp), onExpandSection)
        Row(
            Modifier.padding(horizontal = 16.dp)
        ) {
            val minMoreWidth = 64.dp
            val (visibleBooks, visibleBooksWidth) = books.fold(emptyList<Book>() to 0.dp) { (acc, currentWidth), book ->
                when {
                    currentWidth + book.spineWidthDp() < width -> (acc + book) to (currentWidth + book.spineWidthDp())
                    else -> acc to currentWidth
                }
            }
            val withMore = when {
                visibleBooks.size == books.size -> visibleBooks
                visibleBooksWidth + minMoreWidth < width -> visibleBooks + null
                else -> visibleBooks.dropLast(1) + null
            }
            withMore.forEach { book ->
                when(book) {
                    null -> Column(
                        modifier = modifier
                            .clickable(onClick = onExpandSection)
                            .weight(1f)
                            .border(1.dp, BiblioTheme.colors.divider, shape = RoundedCornerShape(6.dp))
                            .background(BiblioTheme.colors.surface, shape = RoundedCornerShape(6.dp))
                            .clip(RoundedCornerShape(6.dp)),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        ExactText(
                            text = "+${books.size - visibleBooks.size} more",
                            color = BiblioTheme.colors.onBackgroundSecondary,
                            modifier = Modifier.padding(8.dp)
                                .weight(1f)
                                .rotateWithBounds(90f),
                        )
                    }
                    else -> LibrarySpine(
                        book = book,
                        modifier = Modifier
                            .width(book.spineWidthDp())
                            .clickable { onOpenBook(book) }
                            .padding(horizontal = 1.dp),
                    )
                }
            }
        }
    }
}