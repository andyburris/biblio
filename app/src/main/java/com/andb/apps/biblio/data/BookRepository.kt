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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.withContext
import org.readium.r2.shared.publication.epub.pageList
import org.readium.r2.shared.publication.services.cover
import org.readium.r2.shared.util.getOrElse
import java.io.File
import java.time.LocalDateTime
import java.util.Date
import java.util.UUID
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.seconds
import kotlin.time.measureTimedValue
import kotlin.time.toJavaDuration


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

    fun getBooks(): Flow<List<Book>> {
        val flow = database.savedBookQueries
            .selectAll()
            .asFlow()
            .map { query -> query.executeAsList() }
            .map { books ->
                books.map { book ->
                    Book(
                        id = book.id,
                        title = book.title,
                        authors = book.authors,
                        cover = when(val cover = coverStorage.getCover(book.id)) {
                            null -> BookCover.Unavailable
                            else -> BookCover.Available(cover)
                        },
                        progress = book.progress,
                        length = book.length?.toInt(),
                        filePath = book.filePath,
                    )
                }
            }

        return flow
    }

    private fun getPublicationsFromStorage(): List<File> {
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

    suspend fun refreshPublicationsFromStorage(existingBooks: List<Book>) {
        val files = getPublicationsFromStorage()

        val existingPaths = existingBooks.map { it.filePath }
        val updatedPaths = files.map { it.path }
        val newFiles = files.filter { it.path !in existingPaths }
        val deletedBooks = existingBooks.filter { it.filePath !in updatedPaths }

        if (newFiles.isEmpty() && deletedBooks.isEmpty()) return

        val loadedBooks = newFiles
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

        val savedBooks: List<SavedBook> = loadedBooks.map { (pub, file) ->
            SavedBook(
                id = pub.metadata.identifier ?: UUID.randomUUID().toString(),
                title = pub.metadata.title,
                authors = pub.metadata.authors.distinctBy { it.identifier ?: it.name },
                progress = BookProgress.Basic(addedAt = LocalDateTime.now(), timesOpened = 0L),
                length = (pub.metadata.numberOfPages ?: if(pub.pageList.size > 5) pub.pageList.size else 300).toLong(),
                filePath = file.path,
            )
        }
        loadedBooks.zip(savedBooks).map { (pub, book) ->
            coroutineScope.async {
                val cover = pub.first.cover()
                if (cover != null) coverStorage.saveCover(book.id, cover)
            }
        }.awaitAll()

        val deletedIds = deletedBooks.map { it.id }
        val movedBooks = savedBooks.filter { book -> book.id in deletedIds }
        val movedBookIds = movedBooks.map { it.id }

        database.savedBookQueries.transaction {
            savedBooks.filter { it.id !in movedBookIds }.map { book ->
                database.savedBookQueries.insertFullBookObject(book)
            }
            deletedBooks.filter { it.id !in movedBookIds }.forEach { book ->
                database.savedBookQueries.delete(book.id)
            }
            movedBooks.forEach {
                database.savedBookQueries.updateFilePath(it.filePath, it.id)
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


        database.savedBookQueries.updateProgress(book.progress.increaseOpened(), book.id)

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
    data class Loaded(
        val currentlyReading: List<Book>,
        val unread: List<Book>,
        val doneOrBackburner: List<Book>,
        val allBooks: List<Book>,
    ) : BooksState()
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
                getBooks().collect { books ->
                    allBooks.value = books.categorize()
                }
            }
            false -> allBooks.value = BooksState.NoPermission
        }
    }

    LaunchedEffect(permissionState) {
        withContext(Dispatchers.IO) {
            while(true){
                refreshPublicationsFromStorage(when(val state = allBooks.value) {
                    is BooksState.Loaded -> state.allBooks
                    else -> emptyList()
                })
                delay(10000)
            }
        }
    }

    return allBooks
}

private val backburnerTime = 90.days
private fun List<Book>.categorize(): BooksState.Loaded {
    val sorted = this
        .sortedBy { it.title }
        .sortedByDescending { it.progress.lastOpened }
    val (currentlyReading, unread, doneOrBackburner) = sorted
        .fold(Triple(emptyList<Book>(), emptyList<Book>(), emptyList<Book>())) { (currentlyReading, unread, doneOrBackburner), book ->
            val beforeBackburnerTime = book.progress.lastOpened?.isBefore(LocalDateTime.now().minus(backburnerTime.toJavaDuration())) ?: false
            when(book.progress) {
                is BookProgress.Progress -> {
                    val progress = book.progress.percent
                    when {
                        progress in 0.0..0.95 && book.progress.lastOpened?.isAfter(LocalDateTime.now().minus(backburnerTime.toJavaDuration())) ?: false ->
                            Triple((currentlyReading + book), unread, doneOrBackburner)
                        progress == 0.0 || book.progress.lastOpened == null ->
                            Triple(currentlyReading, (unread + book), doneOrBackburner)
                        else ->
                            Triple(currentlyReading, unread, (doneOrBackburner + book))
                    }
                }
                is BookProgress.Basic -> {
                    when {
                        book.progress.lastOpened?.isAfter(LocalDateTime.now().minus(backburnerTime.toJavaDuration())) ?: false ->
                            Triple((currentlyReading + book), unread, doneOrBackburner)
                        book.progress.lastOpened == null ->
                            Triple(currentlyReading, (unread + book), doneOrBackburner)
                        else ->
                            Triple(currentlyReading, unread, (doneOrBackburner + book))
                    }
                }
            }
        }
    return BooksState.Loaded(currentlyReading, unread, doneOrBackburner, sorted)
}