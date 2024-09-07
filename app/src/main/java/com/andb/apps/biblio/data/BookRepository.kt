package com.andb.apps.biblio.data

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.os.Environment
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.core.content.FileProvider
import app.cash.sqldelight.coroutines.asFlow
import com.andb.apps.biblio.BuildConfig
import com.andb.apps.biblio.SavedBook
import com.andb.apps.biblio.ui.home.ReadiumUtils
import com.andb.apps.biblio.ui.home.StoragePermissionState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.take
import org.readium.r2.shared.publication.epub.pageList
import org.readium.r2.shared.publication.services.cover
import org.readium.r2.shared.util.getOrElse
import java.io.File
import java.time.LocalDateTime
import java.util.UUID
import kotlin.time.Duration.Companion.seconds
import kotlin.time.measureTimedValue


val ACCEPTED_EXTENSIONS = listOf("epub")
val ROOT_DIR: String = Environment.getExternalStorageDirectory().absolutePath
val ANDROID_DIR = File("$ROOT_DIR/Android")
val DATA_DIR = File("$ROOT_DIR/data")

class BookRepository(
    private val context: Context,
    private val coroutineScope: CoroutineScope,
) {
    private val readium = ReadiumUtils(context)
    private val database = createDatabase(context)
    private val coverStorage = CoverStorage(context)

    @OptIn(FlowPreview::class)
    suspend fun getBooks(): Flow<List<Book>> {
        val flow = database.savedBookQueries
            .selectAll()
            .asFlow()
            .map { query -> query.executeAsList() }
            .map { books ->
                books.map { book ->
                    Book(
                        id = book.identifier,
                        title = book.title,
                        authors = book.authors,
                        cover = when(val cover = coverStorage.getCover(book.identifier)) {
                            null -> BookCover.Unavailable
                            else -> BookCover.Available(cover)
                        },
                        progress = book.progress,
                        length = book.length?.toInt(),
                        filePath = book.filePath,
                    )
                }
            }

        val debouncedFirst = merge(
            flow.take(1),
            flow.drop(1).debounce(10.seconds)
        )
        return debouncedFirst.onEach { books ->
            Log.d("BookRepository", "refreshing books")
            refreshPublicationsFromStorage(books)
        }
    }

    private suspend fun getPublicationsFromStorage(): List<File> {
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
        return files
    }

    private suspend fun refreshPublicationsFromStorage(existingBooks: List<Book>) {
        val files = getPublicationsFromStorage()

        val existingPaths = existingBooks.map { it.filePath }
        val updatedPaths = files.map { it.path }
        val newFiles = files.filter { it.path !in existingPaths }
        val deletedBooks = existingBooks.filter { it.filePath !in updatedPaths }

        if (newFiles.isEmpty() && deletedBooks.isEmpty()) return

        val loaded = newFiles
            .map { file ->
                coroutineScope.async {
                    val asset = readium.assetRetriever.retrieve(file)
                        .getOrElse { return@async null }

                    val publication =
                        readium.publicationOpener.open(asset, allowUserInteraction = false)
                            .getOrElse {
                                asset.close()
                                return@async null
                            }
                    publication to file
                }
            }
            .awaitAll()
            .filterNotNull()
            .distinctBy { (pub, _) ->
                listOf(
                    pub.metadata.title,
                    pub.metadata.authors.distinctBy { it.identifier ?: it.name }
                )
            }

        val covers = loaded.map {
            coroutineScope.async {
                it.first.cover()
            }
        }.awaitAll()
        val zipped = loaded.zip(covers)
        val books: List<SavedBook> = zipped.map { (pair, coverInfo) ->
            val (pub, file) = pair
            SavedBook(
                key = 0L,
                identifier = pub.metadata.identifier ?: UUID.randomUUID().toString(),
                title = pub.metadata.title,
                authors = pub.metadata.authors.distinctBy { it.identifier ?: it.name },
                progress = BookProgress.Basic(lastOpened = LocalDateTime.now(), timesOpened = 0L),
                length = (pub.metadata.numberOfPages ?: if(pub.pageList.size > 5) pub.pageList.size else 300).toLong(),
                filePath = file.path,
            )
        }
        books.zip(covers).forEach { (book, cover) ->
            if (cover != null) coverStorage.saveCover(book.identifier, cover)
        }
        database.savedBookQueries.transaction {
            books.map { book ->
                database.savedBookQueries.insertFullBookObject(book)
            }
        }
    }

    fun openBook(book: Book) {
        val file = File(book.filePath)
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
    data class Loaded(val books: List<Book>) : BooksState()
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
                getBooks().collect {
                    allBooks.value = BooksState.Loaded(it)
                }
            }
            false -> allBooks.value = BooksState.NoPermission
        }
    }

    return allBooks
}