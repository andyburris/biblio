package com.andb.apps.biblio.data

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.os.Environment
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.core.content.FileProvider
import com.andb.apps.biblio.BuildConfig
import com.andb.apps.biblio.ui.home.ReadiumUtils
import com.andb.apps.biblio.ui.home.StoragePermissionState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.util.getOrElse
import java.io.File
import kotlin.time.measureTimedValue


val ACCEPTED_EXTENSIONS = listOf("epub")
val ROOT_DIR: String = Environment.getExternalStorageDirectory().absolutePath
val ANDROID_DIR = File("$ROOT_DIR/Android")
val DATA_DIR = File("$ROOT_DIR/data")

class BookRepository(private val context: Context) {
    private val readium = ReadiumUtils(context)
    private val fileCache = mutableMapOf<Int, File>()

    suspend fun getPublications(): List<Publication> {
        val root = File(ROOT_DIR)
        val (files, duration) = measureTimedValue {
            root.walk()
                // before entering this dir check if
                .onEnter {
                    !it.isHidden // it is not hidden
                            && it != ANDROID_DIR // it is not Android directory
                            && it != DATA_DIR // it is not data directory
                            && !File(it, ".nomedia").exists() // there is no .nomedia file inside
                }.filter { ACCEPTED_EXTENSIONS.contains(it.extension) } // it is of accepted type
                .toList()
        }
        println("took $duration to get files")

        val loaded = files
            .map { file ->
                CoroutineScope(Dispatchers.IO).async {
                    val asset = readium.assetRetriever.retrieve(file)
                        .getOrElse { return@async null }

                    val publication =
                        readium.publicationOpener.open(asset, allowUserInteraction = false)
                            .getOrElse {
                                asset.close()
                                return@async null
                            }
                    file to publication
                }
            }
            .awaitAll()
            .filterNotNull()
            .distinctBy { (_, pub) ->
                listOf(
                    pub.metadata.title,
                    pub.metadata.authors.distinctBy { it.identifier ?: it.name }
                )
            }

        fileCache.putAll(loaded.map { (file, pub) -> pub.hashCode() to file })
        return loaded.map { it.second }
    }

    fun openPublication(publication: Publication) {
        val hash = publication.hashCode()
        val file = fileCache[hash] ?: return
        val uri = FileProvider.getUriForFile(context, "${BuildConfig.APPLICATION_ID}.provider", file)
        val mime = context.contentResolver.getType(uri)
        val openFileIntent = Intent(Intent.ACTION_VIEW)
            .also {
                it.setDataAndType(uri, mime)
                it.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            }
        try {
            context.startActivity(openFileIntent)
        } catch (e: ActivityNotFoundException) {
            // TODO: instruct user to install a reader
            System.err.println("No readers found")
        }
    }
}

sealed class BooksState {
    data object Loading : BooksState()
    data object NoPermission : BooksState()
    data class Loaded(val books: List<Publication>) : BooksState()
}

@Composable
fun BookRepository.booksAsState(
    permissionState: StoragePermissionState,
): State<BooksState> {
    val allBooks = remember { mutableStateOf<BooksState>(BooksState.Loading) }

    LaunchedEffect(permissionState) {
        when(permissionState.isGranted) {
            true -> {
                allBooks.value = BooksState.Loading
                val books = getPublications()
                allBooks.value = BooksState.Loaded(books)
            }
            false -> allBooks.value = BooksState.NoPermission
        }
    }

    return allBooks
}