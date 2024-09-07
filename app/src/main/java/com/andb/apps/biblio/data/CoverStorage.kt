package com.andb.apps.biblio.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class CoverStorage(
    val context: Context,
) {
    private val coverDir = context.getExternalFilesDir("covers") ?: throw Error("Could not get external files dir")

    private fun fileName(bookId: String) = "$bookId.png"

    fun getCover(bookId: String): Bitmap? {
        val file = File(coverDir, fileName(bookId))
        return when {
            file.exists() -> BitmapFactory.decodeFile(file.absolutePath)
            else -> null
        }
    }

    suspend fun saveCover(bookId: String, cover: Bitmap): File {
        return withContext(Dispatchers.IO) {
            val coverImageFile = File(coverDir, fileName(bookId))
            val fos = FileOutputStream(coverImageFile)
            cover.compress(Bitmap.CompressFormat.PNG, 80, fos)
            fos.flush()
            fos.close()
            coverImageFile
        }
    }
}