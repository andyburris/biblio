package com.andb.apps.biblio.ui.library

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.adamglin.PhosphorIcons
import com.adamglin.phosphoricons.Regular
import com.adamglin.phosphoricons.regular.Check
import com.andb.apps.biblio.data.Book
import com.andb.apps.biblio.data.BookCover
import com.andb.apps.biblio.data.BookProgress
import com.andb.apps.biblio.ui.common.ExactText
import com.andb.apps.biblio.ui.common.rotateWithBounds
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

@Composable
fun LibrarySpine(
    book: Book,
    modifier: Modifier = Modifier,
    isSelected: Boolean = false,
) {
    val isImageDark = remember(book.cover) {
        book.cover is BookCover.Available && book.cover.isDark
    }
    Box(
        modifier = modifier
            .border(1.dp, BiblioTheme.colors.divider, shape = RoundedCornerShape(6.dp))
            .background(BiblioTheme.colors.surface, shape = RoundedCornerShape(6.dp))
            .clip(RoundedCornerShape(6.dp)),
    ) {
        if (book.cover is BookCover.Available) {
            Image(
                bitmap = book.cover.blurredSpine.asImageBitmap(),
                contentScale = ContentScale.FillBounds,
                contentDescription = null,
                modifier = Modifier
                    .drawWithContent {
                        drawContent()
                        drawRect(
                            color = when (isImageDark) {
                                true -> Color.Black.copy(alpha = 0.5f)
                                false -> Color.White.copy(alpha = 0.5f)
                            },
                            topLeft = Offset(0f, 0f),
                            size = size,
                        )
                    }
                    .fillMaxSize(),
            )
        }
        Column(
            modifier = Modifier
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            ExactText(
                text = AnnotatedString.Builder().apply {
                    withStyle(
                        BiblioTheme.typography.caption
                            .copy(fontWeight = FontWeight.Bold, color = when(isImageDark) {
                                true -> BiblioTheme.colors.onPrimary
                                false -> BiblioTheme.colors.onBackground
                            })
                            .toSpanStyle()
                    ) {
//                        append("${book.length}pgs")
//                        append(" • ")
                        append(book.title ?: NO_TITLE)
                    }
                    withStyle(
                        BiblioTheme.typography.caption
                            .copy(color = when(isImageDark) {
                                true -> BiblioTheme.colors.onPrimarySecondary
                                false -> BiblioTheme.colors.onBackgroundSecondary
                            })
                            .toSpanStyle()
                    ) {
                        append(" • ")
                        append(book.authors.joinAuthorsToString())
                    }
                }.toAnnotatedString(),
                modifier = Modifier
                    .padding(8.dp)
                    .weight(1f)
                    .rotateWithBounds(90f),
                overflow = TextOverflow.Ellipsis,
            )
            if (isSelected) {
                Icon(
                    imageVector = PhosphorIcons.Regular.Check,
                    contentDescription = "Selected",
                    tint = when (isImageDark) {
                        true -> BiblioTheme.colors.onPrimary
                        false -> BiblioTheme.colors.onBackground
                    },
                    modifier = Modifier
                        .padding(4.dp)
                        .size(16.dp),
                )
            }
            if (book.progress is BookProgress.Progress) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Box(
                        modifier = Modifier
                            .background(
                                color = when (isImageDark) {
                                    true -> BiblioTheme.colors.onPrimary
                                    false -> BiblioTheme.colors.onBackgroundSecondary
                                },
                                shape = RoundedCornerShape(topEnd = 4.dp)
                            )
                            .fillMaxWidth(
                                book.progress.percent
                                    .toFloat()
                                    .coerceAtLeast(0.01f)
                            )
                            .height(6.dp),
                    )
                }
            }
        }
    }
}