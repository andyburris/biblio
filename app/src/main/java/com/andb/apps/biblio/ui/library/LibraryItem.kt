package com.andb.apps.biblio.ui.library

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.blue
import androidx.core.graphics.green
import androidx.core.graphics.red
import com.andb.apps.biblio.data.Book
import com.andb.apps.biblio.data.BookCover
import com.andb.apps.biblio.data.BookProgress
import com.andb.apps.biblio.data.averageLuminosity
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
) {
    val isImageDark = remember(book.cover) {
        book.cover is BookCover.Available && book.cover.image.averageLuminosity() < 0.5
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
                            color = when(isImageDark) {
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
                lineHeight = 18.sp,
                overflow = TextOverflow.Ellipsis,
            )
            if (book.progress is BookProgress.Progress) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Box(
                        modifier = Modifier
                            .background(
                                color = when(isImageDark) {
                                    true -> BiblioTheme.colors.onPrimarySecondary
                                    false -> BiblioTheme.colors.onBackgroundSecondary
                                },
                                shape = RoundedCornerShape(topEnd = 4.dp)
                            )
                            .fillMaxWidth(book.progress.percent.toFloat().coerceAtLeast(0.01f))
                            .height(4.dp),
                    )
                }
            }
        }
    }
}