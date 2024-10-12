package com.andb.apps.biblio.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.renderscript.Allocation
import android.renderscript.Element
import android.renderscript.RenderScript
import android.renderscript.ScriptIntrinsicBlur
import androidx.core.graphics.blue
import androidx.core.graphics.green
import androidx.core.graphics.red
import com.andb.apps.biblio.SavedBook
import com.andb.apps.biblio.ui.home.DefaultPageLength
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class CoverStorage(
    val context: Context,
) {
    private val coverDir = context.getExternalFilesDir("covers") ?: throw Error("Could not get external files dir")

    private fun fileName(bookId: String) = "$bookId.png"
    private fun blurredFileName(bookId: String) = "$bookId-blurred.png"

    fun getCover(bookId: String): BookCover {
        val file = File(coverDir, fileName(bookId))
        val blurredFile = File(coverDir, blurredFileName(bookId))
        return when {
            file.exists() && blurredFile.exists() -> {
                val cover = BitmapFactory.decodeFile(file.absolutePath)
                val blurred = BitmapFactory.decodeFile(blurredFile.absolutePath)
                BookCover.Available(
                    image = cover,
                    blurredSpine = blurred,
                    isDark = blurred.averageLuminosity() < 0.5
                )
            }
            else -> BookCover.Unavailable
        }
    }

    suspend fun saveCover(book: SavedBook, cover: Bitmap) {
        val bookId = book.id
        val bookLength = book.length ?: DefaultPageLength.toLong()
        return withContext(Dispatchers.IO) {
            val coverImageFile = File(coverDir, fileName(bookId))
            val coverFos = FileOutputStream(coverImageFile)
            cover.compress(Bitmap.CompressFormat.PNG, 80, coverFos)
            coverFos.flush()
            coverFos.close()

            val blurredImageFile = File(coverDir, blurredFileName(bookId))
            val spineFos = FileOutputStream(blurredImageFile)
            val spineWidthRatio = (bookLength / 3000.0)
            val spineWidthPixels = (cover.height * spineWidthRatio).toInt().coerceAtLeast(1)
            // println("resizing image before blur: book: ${book.title} length: $bookLength, spineWidthRatio: $spineWidthRatio, coverHeight: ${cover.height} spineWidthPixels: $spineWidthPixels")
            val resized = Bitmap.createScaledBitmap(cover, spineWidthPixels, cover.height, true)
            val blurred = (0 until 8).fold(resized) { acc, _ ->
                acc.blur(context, 25f)
            }
            blurred.compress(Bitmap.CompressFormat.PNG, 80, spineFos)
            spineFos.flush()
            spineFos.close()
        }
    }
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

private fun Bitmap.averageLuminosity(): Double {
    val smallerWidth = 3
    val aspectRatio = this.width / this.height.toDouble()
    val smallerHeight = (smallerWidth / aspectRatio).toInt()
    val scaled = Bitmap.createScaledBitmap(this, smallerWidth, smallerHeight, true)
    val array = IntArray(smallerWidth * smallerHeight)
    scaled.getPixels(array, 0, smallerWidth, 0, 0, smallerWidth, smallerHeight)
    val luminosity = array
        .map { it.red * 0.299 + it.green * 0.587 + it.blue * 0.114 }
        .average()
    return luminosity / 255
}