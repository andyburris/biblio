package com.andb.apps.biblio.ui.home

import android.content.Context
import android.graphics.Bitmap
import android.renderscript.Allocation
import android.renderscript.Element
import android.renderscript.RenderScript
import android.renderscript.ScriptIntrinsicBlur
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.paint
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.setFrom
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastJoinToString
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.publication.epub.pageList
import org.readium.r2.shared.publication.services.cover
import org.readium.r2.shared.publication.services.locateProgression
import kotlin.math.roundToInt


val BookItemWidth = (256.dp)
val DefaultBookItemHeight = 384.dp

data class BookCoverImage(val cover: Bitmap, val blurred: Bitmap, val spineBlurred: Bitmap)
@Composable
fun BookItem(
    publication: Publication,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val density = LocalDensity.current

    val pages = publication.metadata.numberOfPages
        ?: if(publication.pageList.size > 5) publication.pageList.size else 300
    val spineWidth = ((pages / 16.0) + 2.0).dp
    fun height(cover: Bitmap?) = when (cover) {
        null -> DefaultBookItemHeight
        else -> (BookItemWidth * (cover.height / cover.width.toFloat()))
    }

    val potentialCovers = remember { mutableStateOf<BookCoverImage?>(null) }
    LaunchedEffect(publication) {
        val cover = publication.cover()
        if (cover != null) {
            val blurred = (0 until 4).fold(cover) { acc, _ ->
                acc.blur(context, 25f)
            }
            val spineResized = Bitmap.createScaledBitmap(
                blurred,
                with(density) { spineWidth.toPx() }.toInt(),
                with(density) { height(cover).toPx() }.toInt(),
                false
            ).flipHorizontally()
            val spineBlurred = (0 until 4).fold(spineResized) { acc, _ ->
                acc.blur(context, 25f)
            }
            potentialCovers.value = BookCoverImage(cover, blurred, spineBlurred)
        }
    }


    Row(
        modifier
            .height(IntrinsicSize.Max)
            .skew(yDeg = -3.0)
    ) {
        val bg = MaterialTheme.colorScheme.secondaryContainer
        Box(
            Modifier
                .padding(bottom = spineWidth)
                .skew(yDeg = 55.0)
                .width(spineWidth)
                .fillMaxHeight()
                .defaultMinSize(minHeight = 24.dp)
                .then(when(val cover = potentialCovers.value) {
                    null -> Modifier.background(MaterialTheme.colorScheme.secondaryContainer)
                    else -> Modifier.drawBehind {
                        drawImage(cover.spineBlurred.asImageBitmap())
                    }
                })

        )
        Column() {
            val bg = MaterialTheme.colorScheme.background
            val bgSecondary = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
            val pageColors = (0 until (pages / 25)).flatMap { listOf(bg, bgSecondary) }
            val pagesGradient = Brush.verticalGradient(pageColors)
            Box(
                Modifier
                    .offset(x = -spineWidth)
                    .skew(xDeg = 55.0)
                    .height(spineWidth)
                    .width(BookItemWidth)
                    .background(pagesGradient)
            )
            BookCover(potentialCovers.value?.cover, publication)
        }
    }
}

@Composable
fun BookCover(potentialCover: Bitmap?, publication: Publication) {
    when(potentialCover) {
        null -> Column(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.secondaryContainer)
                .size(width = BookItemWidth, height = DefaultBookItemHeight)
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp, Alignment.Bottom),
        ) {
            Text(text = publication.metadata.title ?: "No title found", style = MaterialTheme.typography.titleMedium)
            Text(text = publication.metadata.authors.fastJoinToString(", ") { it.name })
        }
        else -> Image(
            bitmap = potentialCover.asImageBitmap(),
            contentDescription = "Cover of ${publication.metadata.title}",
            modifier = Modifier
                .width(BookItemWidth)
                .aspectRatio(potentialCover.width / potentialCover.height.toFloat())
        )
    }
}

fun Modifier.skew(xDeg: Double = 0.0, yDeg: Double = 0.0): Modifier = drawWithContent {

    drawContext.canvas.skew(
        Math.toRadians(xDeg).toFloat(),
        Math.toRadians(yDeg).toFloat()
    )
    drawContent()
}

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