package com.andb.apps.biblio.ui.home

import android.content.Context
import android.graphics.Bitmap
import android.renderscript.Allocation
import android.renderscript.Element
import android.renderscript.RenderScript
import android.renderscript.ScriptIntrinsicBlur
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.andb.apps.biblio.data.Book
import com.andb.apps.biblio.data.BookCover
import com.andb.apps.biblio.ui.common.ExactText
import com.andb.apps.biblio.ui.common.skew
import com.andb.apps.biblio.ui.library.NO_TITLE
import com.andb.apps.biblio.ui.theme.BiblioTheme


const val DefaultBookAspectRatio = (256 / 384.0f)

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
    coverInfo: BookItemInfo,
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

    val pages = when(coverInfo) {
        is BookItemInfo.Custom -> 300
        is BookItemInfo.Pub -> coverInfo.publication.length ?: 300
    }

    val spineWidth = ((pages * spineWidthMultiplier) + 2.0).dp
    val cover = when {
        coverInfo is BookItemInfo.Pub && coverInfo.publication.cover is BookCover.Available -> coverInfo.publication.cover
        else -> null
    }

    Row(
        modifier
            .height(IntrinsicSize.Max)
            .skew(yDeg = -3.0)
    ) {
        Box(
            Modifier
                .padding(bottom = spineWidth)
                .skew(yDeg = 55.0)
                .width(spineWidth)
                .fillMaxHeight()
                .clip(RoundedCornerShape(bottomStart = 12.dp))
                .then(when (cover) {
                    null -> Modifier.background(BiblioTheme.colors.onBackgroundTertiary)
                    else -> {
                        val blurred = remember {
                            val blurred = (0 until 4).fold(cover.image) { acc, _ ->
                                acc.blur(context, 25f)
                            }
                            val spineResized = Bitmap.createScaledBitmap(
                                blurred,
                                with(density) { spineWidth.toPx() }.toInt(),
                                with(density) { height.toPx() }.toInt(),
                                false
                            ).flipHorizontally()
                            val spineBlurred = (0 until 4).fold(spineResized) { acc, _ ->
                                acc.blur(context, 25f)
                            }
                            spineBlurred.asImageBitmap()
                        }
                        Modifier.drawBehind {
                            drawImage(blurred)
                        }
                    }
                })

        )
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
            val pageColors = (0 until (pages * pagesMultiplier).toInt().coerceAtLeast(1)).flatMap { listOf(bg, bgSecondary) }
            val pagesGradient = Brush.verticalGradient(pageColors)
            Box(
                Modifier
                    .offset(x = -spineWidth)
                    .skew(xDeg = 55.0)
                    .height(spineWidth)
                    .fillMaxWidth()
                    .background(pagesGradient)
            )
            when(cover) {
                null -> TextCover(coverInfo, size, height)
                else -> ImageCover(cover, (coverInfo as BookItemInfo.Pub).publication, height)
            }
        }
    }
}

@Composable
private fun TextCover(itemInfo: BookItemInfo, size: BookItemSize, height: Dp) {
    Column(
        modifier = Modifier
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
fun ImageCover(cover: BookCover.Available, publication: Book, height: Dp) {
    Image(
        bitmap = cover.image.asImageBitmap(),
        contentDescription = "Cover of ${publication.title}",
        modifier = Modifier
            .height(height)
            .aspectRatio(cover.image.width / cover.image.height.toFloat())
    )
}

@Suppress("DEPRECATION")
fun Bitmap.blur(context: Context, radius: Float = 25f): Bitmap {
    val outBitmap = Bitmap.createBitmap(this.width, this.height, Bitmap.Config.ARGB_8888)
    val rs = RenderScript.create(context)

    // Create allocation objects
    val input: Allocation = Allocation.createFromBitmap(rs, this)
    val output: Allocation = Allocation.createTyped(rs, input.getType())

    // Create and set Script
    val script = ScriptIntrinsicBlur.create(rs, Element.U8_4(rs))
    script.setRadius(radius)
    script.setInput(input)
    script.forEach(output)

    // Copy script result into bitmap
    output.copyTo(outBitmap)

    // Release memory allocations
    input.destroy()
    output.destroy()
    script.destroy()
    rs.destroy()

    return outBitmap
}

fun Bitmap.flipHorizontally(): Bitmap {
    val matrix = android.graphics.Matrix().apply { postScale(-1f, 1f, width / 2f, height / 2f) }
    return Bitmap.createBitmap(this, 0, 0, width, height, matrix, true)
}