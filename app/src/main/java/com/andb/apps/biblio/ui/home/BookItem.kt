package com.andb.apps.biblio.ui.home

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.andb.apps.biblio.data.Book
import com.andb.apps.biblio.data.BookCover
import com.andb.apps.biblio.data.BookProgress
import com.andb.apps.biblio.data.LocalSettings
import com.andb.apps.biblio.ui.common.ExactText
import com.andb.apps.biblio.ui.common.skew
import com.andb.apps.biblio.ui.library.NO_TITLE
import com.andb.apps.biblio.ui.theme.BiblioTheme
import kotlin.math.roundToInt


const val DefaultBookAspectRatio = (256 / 384.0f)
const val DefaultPageLength = 300L

enum class BookItemSize {
    Small, Medium, Large
}

private sealed class BookItemInfo {
    data class Pub(val publication: Book) : BookItemInfo()
    data class Custom(val icon: ImageVector?, val title: String, val badge: String?) : BookItemInfo()
}

@Composable
fun BookItem(
    publication: Book,
    size: BookItemSize,
    modifier: Modifier = Modifier,
) = BookItem(BookItemInfo.Pub(publication), size, modifier)

@Composable
fun BookItem(
    title: String,
    size: BookItemSize,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    badge: String? = null,
) = BookItem(BookItemInfo.Custom(icon, title, badge), size, modifier)

@Composable
private fun BookItem(
    itemInfo: BookItemInfo,
    size: BookItemSize,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val density = LocalDensity.current

    val height = when(size) {
        BookItemSize.Small -> 44.dp
        BookItemSize.Medium -> 128.dp
        BookItemSize.Large -> 356.dp
    }

    val spineWidthMultiplier = when(size) {
        BookItemSize.Small -> 1 / 256.0
        BookItemSize.Medium -> 1 / 96.0
        BookItemSize.Large -> 1 / 20.0
    }

    val spineInsetMargin = when(size) {
        BookItemSize.Small -> 0.dp
        BookItemSize.Medium -> 2.dp
        BookItemSize.Large -> 4.dp
    }

    val pages = when(itemInfo) {
        is BookItemInfo.Custom -> DefaultPageLength
        is BookItemInfo.Pub -> itemInfo.publication.length ?: DefaultPageLength
    }

    val spineWidth = ((pages * spineWidthMultiplier) + 2.0).dp + spineInsetMargin
    val cover = when {
        itemInfo is BookItemInfo.Pub && itemInfo.publication.cover is BookCover.Available -> itemInfo.publication.cover
        else -> BookCover.Unavailable
    }

    Row(
        modifier
            .height(IntrinsicSize.Max)
            .skew(yDeg = -2.5)
    ) {
        val spineModifier = Modifier
            .padding(bottom = spineWidth)
            .skew(yDeg = 55.0)
            .width(spineWidth)
            .heightIn(max = height)
            .fillMaxHeight()
            .clip(RoundedCornerShape(bottomStart = 12.dp, topStart = spineInsetMargin))

        when(cover) {
            BookCover.Unavailable -> Box(
                spineModifier
                    .background(BiblioTheme.colors.onBackgroundTertiary)
            )
            is BookCover.Available -> Image(
                bitmap = cover.blurredSpine.asImageBitmap(),
                contentDescription = "Cover image",
                modifier = spineModifier,
                contentScale = ContentScale.FillBounds,
            )
        }

        Column(
            Modifier.width(IntrinsicSize.Max)
        ) {
            val bg = BiblioTheme.colors.background
            val bgSecondary = BiblioTheme.colors.onBackgroundTertiary
            val pagesMultiplier = when(size) {
                BookItemSize.Small -> 1 / 256.0
                BookItemSize.Medium -> 1 / 96.0
                BookItemSize.Large -> 1 / 60.0
            }
            val pageColors = (0 until (pages * pagesMultiplier)
                .toInt()
                .coerceAtLeast(1))
                .flatMap { listOf(bg, bgSecondary) }
                .let { listOf(bgSecondary) + it }
            val pagesGradient = Brush.verticalGradient(pageColors)
            Box(
                Modifier.heightIn(max = spineWidth)
            ) {
                Box(
                    Modifier
                        .offset(x = -spineWidth)
                        .skew(xDeg = 55.0)
                        .height(spineWidth)
                        .fillMaxWidth()
                        .then(Modifier.padding(top = spineInsetMargin / 2, end = spineInsetMargin))
                        .background(pagesGradient)
                )
                if(
                    itemInfo is BookItemInfo.Pub
                    && itemInfo.publication.progress is BookProgress.Progress
                    && size != BookItemSize.Small
                ) {
                    val percent = itemInfo.publication.progress.percent
                    val bookmarkOffset = when(size) {
                        BookItemSize.Large -> 16.dp
                        else -> 8.dp
                    }
                    Row(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .wrapContentHeight(align = Alignment.Bottom, unbounded = true)
                            .offset(
                                x = -(bookmarkOffset + (spineWidth * percent.toFloat()) * 0.8f),
                                y = -(spineWidth * percent.toFloat() * 0.9f),
                            )
                            .background(
                                color = BiblioTheme.colors.onBackground,
                                shape = when(size){
                                    BookItemSize.Large -> RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp)
                                    else -> RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp)
                                },
                            )
                            .padding(
                                horizontal = when(size){
                                    BookItemSize.Large -> 8.dp
                                    else -> 4.dp
                                },
                                vertical = when(size){
                                    BookItemSize.Large -> 1.dp
                                    else -> 0.dp
                                }
                            ),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        when (LocalSettings.current.settings.common.showNumbers) {
                            true -> ExactText(
                                text = "${(percent * 100).roundToInt()}%",
                                color = BiblioTheme.colors.background,
                                style = when(size){
                                    BookItemSize.Large -> BiblioTheme.typography.body
                                    else -> BiblioTheme.typography.caption
                                }
                            )
                            false -> Spacer(when(size) {
                                BookItemSize.Large -> Modifier.size(width = 8.dp, height = 12.dp)
                                else -> Modifier.size(width = 4.dp, height = 8.dp)
                            })
                        }
                    }
                }
            }

            val coverModifier = Modifier.clip(RoundedCornerShape(
                topEnd = spineInsetMargin * 2,
                bottomEnd = spineInsetMargin * 2,
            ))
            when(cover) {
                BookCover.Unavailable -> TextCover(
                    itemInfo = itemInfo,
                    size = size,
                    height = height,
                    modifier = coverModifier,
                )
                is BookCover.Available -> ImageCover(
                    cover = cover,
                    publication = (itemInfo as BookItemInfo.Pub).publication,
                    height = height,
                    modifier = coverModifier,
                )
            }
        }
    }
}

@Composable
private fun TextCover(
    itemInfo: BookItemInfo,
    size: BookItemSize,
    height: Dp,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .background(BiblioTheme.colors.surface)
            .height(height = height)
            .aspectRatio(DefaultBookAspectRatio)
            .padding(
                when (size) {
                    BookItemSize.Large -> 12.dp
                    else -> 8.dp
                }
            ),
        verticalArrangement = Arrangement.spacedBy(0.dp, Alignment.Bottom),
    ) {
        if(size == BookItemSize.Small) return
        if(itemInfo is BookItemInfo.Custom && itemInfo.badge != null) {
            ExactText(
                text = itemInfo.badge,
                style = BiblioTheme.typography.caption,
                color = BiblioTheme.colors.onBackgroundSecondary,
                textAlign = TextAlign.End,
                modifier = Modifier
                    .fillMaxWidth()
            )
            Spacer(modifier = Modifier.weight(1f))
        }
        if(itemInfo is BookItemInfo.Custom && itemInfo.icon != null) {
            Image(
                imageVector = itemInfo.icon,
                contentDescription = null,
                modifier = Modifier.size(when(size) {
                    BookItemSize.Large -> 32.dp
                    else -> 20.dp
                })
            )
        }
        ExactText(
            text = when(itemInfo) {
                is BookItemInfo.Pub -> itemInfo.publication.title ?: NO_TITLE
                is BookItemInfo.Custom -> itemInfo.title
            },
            style = when(size) {
                BookItemSize.Large -> BiblioTheme.typography.title
                else -> BiblioTheme.typography.caption
            },
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        if(itemInfo is BookItemInfo.Pub) {
            ExactText(
                text = itemInfo.publication.authors.joinAuthorsToString(),
                style = when(size) {
                    BookItemSize.Large -> BiblioTheme.typography.body
                    else -> BiblioTheme.typography.caption
                },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = BiblioTheme.colors.onBackgroundSecondary,
            )
        }
    }
}

@Composable
fun ImageCover(
    cover: BookCover.Available,
    publication: Book,
    height: Dp,
    modifier: Modifier = Modifier,
) {
    Image(
        bitmap = when {
            cover.isDark && LocalSettings.current.common.eInkColors.value -> cover.brightenedImage.asImageBitmap()
            else -> cover.image.asImageBitmap()
        },
        contentDescription = "Cover of ${publication.title}",
        modifier = modifier
            .height(height)
            .aspectRatio(cover.image.width / cover.image.height.toFloat())
    )
}

fun Bitmap.flipHorizontally(): Bitmap {
    val matrix = android.graphics.Matrix().apply { postScale(-1f, 1f, width / 2f, height / 2f) }
    return Bitmap.createBitmap(this, 0, 0, width, height, matrix, true)
}