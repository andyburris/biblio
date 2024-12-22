package com.andb.apps.biblio.ui.library

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.adamglin.PhosphorIcons
import com.adamglin.phosphoricons.Regular
import com.adamglin.phosphoricons.regular.Arrowsleftright
import com.adamglin.phosphoricons.regular.Pencilsimple
import com.adamglin.phosphoricons.regular.X
import com.andb.apps.biblio.data.Book
import com.andb.apps.biblio.ui.common.BiblioBottomBar
import com.andb.apps.biblio.ui.common.BiblioButton
import com.andb.apps.biblio.ui.common.ButtonStyle
import com.andb.apps.biblio.ui.common.ExactText
import com.andb.apps.biblio.ui.common.border
import com.andb.apps.biblio.ui.common.pager.BiblioPageSwitcher
import com.andb.apps.biblio.ui.common.pager.BiblioPager
import com.andb.apps.biblio.ui.common.pager.BiblioPagerItem
import com.andb.apps.biblio.ui.common.pager.BiblioPagerState
import com.andb.apps.biblio.ui.common.pager.BiblioPagerWidth
import com.andb.apps.biblio.ui.home.DefaultPageLength
import com.andb.apps.biblio.ui.theme.BiblioTheme

fun Book.spineWidthDp(multiplier: Double = 1.0): Dp {
    return (((0.1 * multiplier).dp) * (length ?: DefaultPageLength)).coerceIn(24.dp..128.dp) + 4.dp
}

@Composable
fun ShelfPage(
    shelf: LibraryShelf,
    books: List<Book>,
    modifier: Modifier = Modifier,
    onNavigateBack: () -> Unit,
    onOpenBook: (Book) -> Unit,
    onMoveBooks: (List<Book>, LibraryShelf) -> Unit,
) {
    val isEditing = remember { mutableStateOf(false) }
    val selectedBooks = remember { mutableStateOf(emptyList<Book>()) }
    val items = books.map { book ->
        BiblioPagerItem(
            width = BiblioPagerWidth.Fixed(book.spineWidthDp(0.8)),
            content = {
                val isSelected = book in selectedBooks.value
                LibrarySpine(
                    book = book,
                    modifier = Modifier
                        .clickable {
                            when {
                                !isEditing.value -> onOpenBook(book)
                                isSelected -> selectedBooks.value -= book
                                else -> selectedBooks.value += book
                            }
                        }
                        .fillMaxSize()
                        .padding(horizontal = 1.dp),
                    isSelected = isSelected,
                )
            }
        )
    }
    BiblioPager(
        items = items,
        minRowHeight = 160.dp,
        modifier = modifier,
        header = { ShelfHeader(title = shelf.title, pagerState = it) },
        bottomBar =  when (isEditing.value) {
            false -> {{ BiblioBottomBar(
                pageTitle = "Library",
                onNavigateBack = onNavigateBack,
                pageSwitcher = { BiblioPageSwitcher(pagerState = it) },
            ) {
                BiblioButton(
                    style = ButtonStyle.Outline,
                    onClick = { isEditing.value = true },
                    icon = PhosphorIcons.Regular.Pencilsimple,
                    text = "Edit",
                )
            }}}
            true -> {{ BiblioBottomBar(
                pageTitle = "${selectedBooks.value.size} selected",
                onNavigateBack = { isEditing.value = false; selectedBooks.value = emptyList() },
                pageSwitcher = { BiblioPageSwitcher(pagerState = it) },
                backIcon = PhosphorIcons.Regular.X,
            ) {
                LibraryShelf.entries.filter { it !== shelf }.map {
                    BiblioButton(
                        style = ButtonStyle.Outline,
                        enabled = selectedBooks.value.isNotEmpty(),
                        onClick = { onMoveBooks(selectedBooks.value, it); isEditing.value = false },
                        icon = PhosphorIcons.Regular.Arrowsleftright,
                        text = "${it.shortTitle}",
                    )
                }
            }}}
        },
        row = { row, mod, content ->
            Row(
                modifier = mod
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
            .padding(start = 24.dp, top = 24.dp, end = 24.dp, bottom = 8.dp),
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
                    when(pagerState.currentItemRange.value.last + 1) {
                        0 -> append("0")
                        else -> append("${pagerState.currentItemRange.value.first + 1}-${pagerState.currentItemRange.value.last + 1}")
                    }
                }
                withStyle(BiblioTheme.typography.body.copy(color = BiblioTheme.colors.onBackgroundTertiary).toSpanStyle()) {
                    append("/${pagerState.totalItems.value}")
                }
            }.toAnnotatedString()
        )
    }
}
