package com.andb.apps.biblio.data

import android.Manifest
import android.content.Context
import android.os.Environment
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.PermissionChecker.PERMISSION_GRANTED
import com.andb.apps.biblio.ui.home.ReadiumUtils
import kotlinx.coroutines.launch
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.util.getOrElse
import org.readium.r2.shared.util.toAbsoluteUrl
import org.readium.r2.shared.util.toUrl
import java.io.File

val ACCEPTED_EXTENSIONS = listOf("epub")
val ROOT_DIR: String = Environment.getExternalStorageDirectory().absolutePath
val ANDROID_DIR = File("$ROOT_DIR/Android")
val DATA_DIR = File("$ROOT_DIR/data")

class BookRepository(private val readium: ReadiumUtils) {
    suspend fun getPublications(): List<Publication> {
        val files = File(ROOT_DIR).walk()
            // before entering this dir check if
            .onEnter {
                !it.isHidden // it is not hidden
                    && it != ANDROID_DIR // it is not Android directory
                    && it != DATA_DIR // it is not data directory
                    && !File(it, ".nomedia").exists() // there is no .nomedia file inside
            }.filter { ACCEPTED_EXTENSIONS.contains(it.extension) } // it is of accepted type
            .toList()

        return files.map { file ->
            val asset = readium.assetRetriever.retrieve(file)
                .getOrElse {
                    return@map null
                }

            val publication = readium.publicationOpener.open(asset, allowUserInteraction = false)
                .getOrElse {
                    asset.close()
                    return@map null
                }

            publication
        }.filterNotNull()
    }
}