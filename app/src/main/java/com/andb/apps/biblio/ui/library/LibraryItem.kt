package com.andb.apps.biblio.ui.library

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.andb.apps.biblio.data.Book
import com.andb.apps.biblio.data.BookProgress
import com.andb.apps.biblio.ui.common.ExactText
import com.andb.apps.biblio.ui.home.BookItem
import com.andb.apps.biblio.ui.home.BookItemSize
import com.andb.apps.biblio.ui.home.joinAuthorsToString
import com.andb.apps.biblio.ui.theme.BiblioTheme
import kotlin.math.roundToInt

const val NO_TITLE = "No Title"

@Composable
fun LibraryItem(
    publication: Book,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BookItem(publication = publication, size = BookItemSize.Small)
        Column {
            ExactText(
                text = publication.title ?: NO_TITLE,
                style = BiblioTheme.typography.title,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            ExactText(
                text = when(publication.progress) {
                    is BookProgress.Progress -> "${(publication.progress.percent * 100).roundToInt()}% • ${publication.authors.joinAuthorsToString()}"
                    is BookProgress.Basic -> publication.authors.joinAuthorsToString()
                },
                style = BiblioTheme.typography.body,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}