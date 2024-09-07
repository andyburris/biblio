package com.andb.apps.biblio.ui.library

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.andb.apps.biblio.data.Book
import com.andb.apps.biblio.data.BooksState
import com.andb.apps.biblio.ui.common.BiblioBottomBar
import com.andb.apps.biblio.ui.common.BiblioScaffold

@Composable
fun LibraryPage(
    booksState: BooksState,
    modifier: Modifier = Modifier,
    onNavigateBack: () -> Unit,
    onOpenBook: (Book) -> Unit,
) {
    BiblioScaffold(
        modifier = modifier,
        bottomBar = {
            BiblioBottomBar(pageTitle = "Library", onNavigateBack = onNavigateBack)
        }
    ) {
        when(booksState) {
            BooksState.Loading -> {

            }

            BooksState.NoPermission -> {

            }

            is BooksState.Loaded -> {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(200.dp),
                    modifier = modifier
                        .fillMaxSize()
                        .padding(16.dp),
                ) {
                    items(booksState.books) {
                        LibraryItem(
                            publication = it,
                            modifier = Modifier
                                .clickable { onOpenBook(it) }
                                .padding(8.dp),
                        )
                    }
                }
            }
        }
    }
}