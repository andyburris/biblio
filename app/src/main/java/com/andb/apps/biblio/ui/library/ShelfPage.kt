package com.andb.apps.biblio.ui.library

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.coerceAtLeast
import androidx.compose.ui.unit.dp
import com.andb.apps.biblio.data.Book
import com.andb.apps.biblio.ui.common.BiblioBottomBar
import com.andb.apps.biblio.ui.common.BiblioPageSwitcher
import com.andb.apps.biblio.ui.common.BiblioPager
import com.andb.apps.biblio.ui.common.BiblioPagerItem
import com.andb.apps.biblio.ui.common.BiblioPagerState
import com.andb.apps.biblio.ui.common.BiblioPagerWidth
import com.andb.apps.biblio.ui.common.ExactText
import com.andb.apps.biblio.ui.common.border
import com.andb.apps.biblio.ui.home.DefaultPageLength
import com.andb.apps.biblio.ui.theme.BiblioTheme

@Composable
fun ShelfPage(
    shelf: LibraryShelf,
    books: List<Book>,
    modifier: Modifier = Modifier,
    onNavigateBack: () -> Unit,
    onOpenBook: (Book) -> Unit,
) {
    val items = (books).map { book ->
        BiblioPagerItem(
            width = BiblioPagerWidth.Fixed(((0.15.dp) * (book.length ?: DefaultPageLength)).coerceAtLeast(24.dp) + 4.dp),
            content = {
                LibrarySpine(
                    book = book,
                    modifier = Modifier
                        .clickable { onOpenBook(book) }
                        .fillMaxSize()
                        .padding(horizontal = 1.dp)
                )
            }
        )
    }
    BiblioPager(
        items = items,
        minRowHeight = 160.dp,
        modifier = modifier,
        header = { ShelfHeader(title = shelf.title, pagerState = it); 50.dp },
        bottomBar = {
            BiblioBottomBar(
                pageTitle = "Library",
                onNavigateBack = onNavigateBack,
                pageSwitcher = { BiblioPageSwitcher(pagerState = it) },
            )
        },
        row = { row, modifier, content ->
            Row(
                modifier = modifier
                    .padding(top = 12.dp)
                    .border(BiblioTheme.colors.surface, bottom = 4.dp),
                horizontalArrangement = when(row.fillsWidth) {
                    true -> Arrangement.SpaceBetween
                    false -> Arrangement.Start
                },
                content = content,
            )
        }
    )
}

@Composable
private fun ShelfHeader(
    title: String,
    pagerState: BiblioPagerState,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        ExactText(
            text = title,
            style = BiblioTheme.typography.title,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        ExactText(
            text = AnnotatedString.Builder().apply {
                withStyle(BiblioTheme.typography.body.copy(color = BiblioTheme.colors.onBackgroundSecondary).toSpanStyle()) {
                    append("${pagerState.currentItemRange.value.first + 1}-${pagerState.currentItemRange.value.last + 1}")
                }
                withStyle(BiblioTheme.typography.body.copy(color = BiblioTheme.colors.onBackgroundTertiary).toSpanStyle()) {
                    append("/${pagerState.totalItems.value}")
                }
            }.toAnnotatedString()
        )
    }
}
