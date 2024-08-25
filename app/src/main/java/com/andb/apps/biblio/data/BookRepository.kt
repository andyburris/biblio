package com.andb.apps.biblio.data

import android.os.Environment
import com.andb.apps.biblio.ui.home.ReadiumUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.util.getOrElse
import java.io.File

val ACCEPTED_EXTENSIONS = listOf("epub")
val ROOT_DIR: String = Environment.getExternalStorageDirectory().absolutePath
//val ROOT_DIR: String = "/sdcard"
val ANDROID_DIR = File("$ROOT_DIR/Android")
val DATA_DIR = File("$ROOT_DIR/data")

class BookRepository(private val readium: ReadiumUtils) {
    suspend fun getPublications(): List<Publication> {
        val root = File(ROOT_DIR)
        val files = root.walk()
            // before entering this dir check if
            .onEnter {
                !it.isHidden // it is not hidden
                    && it != ANDROID_DIR // it is not Android directory
                    && it != DATA_DIR // it is not data directory
                    && !File(it, ".nomedia").exists() // there is no .nomedia file inside
            }.filter { ACCEPTED_EXTENSIONS.contains(it.extension) } // it is of accepted type
            .toList()

        return files
            .map { file ->
                CoroutineScope(Dispatchers.IO).async {
                    val asset = readium.assetRetriever.retrieve(file)
                        .getOrElse { return@async null }

                    val publication = readium.publicationOpener.open(asset, allowUserInteraction = false)
                        .getOrElse {
                            asset.close()
                            return@async null
                        }
                    publication
                }
            }
            .awaitAll()
            .filterNotNull()
            .distinctBy { listOf(it.metadata.title, it.metadata.authors) }
    }
}