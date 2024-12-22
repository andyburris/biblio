package com.andb.apps.biblio.data

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.os.Environment
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.core.content.FileProvider
import app.cash.sqldelight.coroutines.asFlow
import com.andb.apps.biblio.BuildConfig
import com.andb.apps.biblio.SavedBook
import com.andb.apps.biblio.ui.home.ReadiumUtils
import com.andb.apps.biblio.ui.home.StoragePermissionState
import com.andb.apps.biblio.ui.library.LibraryShelf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import org.readium.r2.shared.publication.Contributor
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.publication.services.cover
import org.readium.r2.shared.publication.services.locateProgression
import org.readium.r2.shared.util.getOrElse
import java.io.File
import java.time.LocalDateTime
import java.util.UUID
import kotlin.time.Duration.Companion.days
import kotlin.time.measureTimedValue
import kotlin.time.toJavaDuration


val ACCEPTED_EXTENSIONS = listOf("epub")
val ROOT_DIR: String = Environment.getExternalStorageDirectory().absolutePath
val ANDROID_DIR = File("$ROOT_DIR/Android")
val DATA_DIR = File("$ROOT_DIR/data")

class BookRepository(
    private val context: Context,
    private val coroutineScope: CoroutineScope,
    private val syncServer: SyncServer,
) {
    private val readium = ReadiumUtils(context)
    private val database = createDatabase(context)
    private val coverStorage = CoverStorage(context)

    private val progressFileFlow = syncServer.fileFlow.toProgressFileFlow()

    fun getBooks(): Flow<List<Book>> {
        val flow = database.savedBookQueries
            .selectAll()
            .asFlow()
            .map { query -> query.executeAsList() }
            .combine(progressFileFlow) { a, b -> a to b }
            .map { (books, progressFiles) ->
                books.map { book ->
                    Book(
                        id = book.id,
                        identifier = book.identifier,
                        title = book.title,
                        authors = book.authors,
                        cover = coverStorage.getCover(book.id),
                        progress = book.getProgressFor(progressFiles),
                        length = book.length?.toInt(),
                        filePaths = book.filePaths,
                    )
                }
            }

        return flow
    }

    private fun getFilesFromStorage(): List<File> {
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
        println("took $duration to get files, found ${files.size} files")
        return files
    }

    suspend fun refreshPublicationsFromStorage(existingBooks: List<Book>) {
        val files = getFilesFromStorage()

        val existingPaths = existingBooks.flatMap { it.filePaths }
        val updatedPaths = files.map { it.path }
        val newFiles = files.filter { it.path !in existingPaths }
        val deletedBooks = existingBooks.filter { book -> book.filePaths.none { it in updatedPaths } }

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
            .fold(emptyMap<Pair<String?, List<Contributor>>, Pair<Publication, List<File>>>()) { acc, (pub, file) ->
                val key = pub.metadata.title to pub.metadata.authors.distinctBy { it.identifier ?: it.name }
                when(acc.containsKey(key)) {
                    true -> {
                        val current = acc.getValue(key)
                        val newFiles = current.second + file
                        acc + (key to (current.first to newFiles))
                    }
                    false -> {
                        acc + (key to (pub to listOf(file)))
                    }
                }
            }.values
            .toList()

        val savedBooks: List<SavedBook> = loadedBooks.map { (pub, files) ->
            SavedBook(
                id = UUID.randomUUID().toString(),
                identifier = pub.metadata.identifier,
                title = pub.metadata.title,
                authors = pub.metadata.authors.distinctBy { it.identifier ?: it.name },
                progress = BookProgress.Basic(addedAt = LocalDateTime.now(), timesOpened = 0L),
                length = (pub.metadata.numberOfPages ?: pub.locateProgression(1.0)?.locations?.position)?.toLong(),
                filePaths = files.map{ it.path },
            )
        }
        loadedBooks.zip(savedBooks).map { (pub, book) ->
            coroutineScope.async(Dispatchers.IO) {
                val cover = pub.first.cover()
                if (cover != null) coverStorage.saveCover(book, cover)
            }
        }.awaitAll()

        val deletedIds = deletedBooks.map { it.id }
        val movedBooks = savedBooks.filter { book -> book.id in deletedIds }
        val movedBookIds = movedBooks.map { it.id }

        database.savedBookQueries.transaction {
            savedBooks.filter { it.id !in movedBookIds }.forEach { book ->
                database.savedBookQueries.insertFullBookObject(book)
            }
            deletedBooks.filter { it.id !in movedBookIds }.forEach { book ->
                database.savedBookQueries.delete(book.id)
            }
            movedBooks.forEach {
                database.savedBookQueries.updateFilePaths(it.filePaths, it.id)
            }
        }
    }

    fun openBook(book: Book) {
        val file = File(book.filePaths.first())
        val providerAuthority = "${BuildConfig.APPLICATION_ID}.provider"
        val uri = FileProvider.getUriForFile(context, providerAuthority, file)
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

    fun moveBooks(books: List<Book>, shelf: LibraryShelf) {
        database.savedBookQueries.transaction {
            books.forEach { book ->
                when(shelf) {
                    LibraryShelf.CurrentlyReading -> database.savedBookQueries
                        .updateProgress(book.progress.toCurrentlyReading(), book.id)
                    LibraryShelf.UpNext -> database.savedBookQueries
                        .updateProgress(book.progress.toUpNext(), book.id)
                    LibraryShelf.DoneOrBackburner -> database.savedBookQueries
                        .updateProgress(book.progress.toAlreadyRead(), book.id)
                }
            }
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
    val isFirstLoad = remember { mutableStateOf(true) }
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
                when (val existingBooks = allBooks.value) {
                    is BooksState.Loaded -> {
                        refreshPublicationsFromStorage(existingBooks.allBooks)
                        isFirstLoad.value = false
                        delay(10000)
                    }
                    else -> {
                        delay(500)
                    }
                }
            }
        }
    }

    return remember {
        derivedStateOf {
            val isStillLoading = isFirstLoad.value && allBooks.value.let { it is BooksState.Loaded && it.allBooks.isEmpty() }
            when {
                isStillLoading -> BooksState.Loading
                else -> allBooks.value
            }
        }
    }
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
                        progress >= AlreadyReadProgress || beforeBackburnerTime || book.progress.markedDone ->
                            Triple(currentlyReading, unread, (doneOrBackburner + book))
                        progress == 0.0 || book.progress.lastOpened == null || book.progress.timesOpened == 0L  ->
                            Triple(currentlyReading, (unread + book), doneOrBackburner)
                        else ->
                            Triple((currentlyReading + book), unread, doneOrBackburner)
                    }
                }
                is BookProgress.Basic -> {
                    when {
                        beforeBackburnerTime || book.progress.markedDone ->
                            Triple(currentlyReading, unread, (doneOrBackburner + book))
                        book.progress.lastOpened == null || book.progress.timesOpened == 0L  ->
                            Triple(currentlyReading, (unread + book), doneOrBackburner)
                        else ->
                            Triple((currentlyReading + book), unread, doneOrBackburner)
                    }
                }
            }
        }
    return BooksState.Loaded(currentlyReading, unread, doneOrBackburner, sorted)
}