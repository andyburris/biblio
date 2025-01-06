package com.andb.apps.biblio.data

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.core.content.FileProvider
import app.cash.sqldelight.coroutines.asFlow
import com.andb.apps.biblio.BuildConfig
import com.andb.apps.biblio.SavedBook
import com.andb.apps.biblio.SyncApp
import com.andb.apps.biblio.ui.home.ReadiumUtils
import com.andb.apps.biblio.ui.home.StoragePermissionState
import com.andb.apps.biblio.ui.library.LibraryShelf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.combineTransform
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.transformLatest
import kotlinx.coroutines.launch
import org.readium.r2.shared.publication.Contributor
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.publication.services.cover
import org.readium.r2.shared.publication.services.locateProgression
import org.readium.r2.shared.util.getOrElse
import java.io.File
import java.time.LocalDateTime
import java.util.UUID
import kotlin.time.Duration.Companion.days
import kotlin.time.toJavaDuration


val ACCEPTED_EXTENSIONS = listOf("epub")

class BookRepository(
    private val context: Context,
    private val coroutineScope: CoroutineScope,
    private val allFilesFlow: SharedFlow<Result<List<File>>>,
    private val syncAppFlow: Flow<SyncApp>,
) {
    private val readium = ReadiumUtils(context)
    private val database = createDatabase(context)
    private val coverStorage = CoverStorage(context)

    private val databaseBookFlow = database.savedBookQueries
        .selectAll()
        .asFlow()
        .map { query -> query.executeAsList() }

    private val getProgressFlow = allFilesFlow.combine(syncAppFlow) { result, syncApp ->
        return@combine { book: SavedBook ->
            when(syncApp) {
                SyncApp.SYNC_APP_KOREADER -> {
                    val progressFiles = result
                        .getOrElse { emptyList() }
                        .filter { it.name == "metadata.epub.lua" }
//                    println("syncing from koreader, progressFiles = ${progressFiles.map { it.path }}")
                    val bookMetadataFolderNames = book.filePaths.map { bookFile ->
                        val bookFileNoExtension = bookFile
                            .takeLastWhile { it != '/' }
                            .dropLastWhile { it != '.' }
                            .dropLast(1)
                        "${bookFileNoExtension}.sdr"
                    }
                    val matchingProgress = progressFiles.find { it.parentFile?.name in bookMetadataFolderNames }
//                    println("found progress file for ${bookMetadataFolderNames}: $matchingProgress")
                    if (matchingProgress != null) {
                        val progress = matchingProgress
                            .readText()
                            .replaceBefore("[\"percent_finished\"] = ", "")
                            .removePrefix("[\"percent_finished\"] = ")
                            .replaceAfter(",", "")
                            .removeSuffix(",")
                            .toDoubleOrNull()
                        if (progress != null) book.progress.withProgress(progress) else book.progress
                    } else {
                        book.progress
                    }
                }
                SyncApp.SYNC_APP_MOON_READER -> {
                    val progressFiles = result
                        .getOrElse { emptyList() }
                        .filter { it.extension == "po" }
//                    println("syncing from moon+ reader, progressFiles = ${progressFiles.map { it.path }}")
                    val matchingProgress = progressFiles.find { progressFile ->
                        book.filePaths.any { bookFile -> progressFile.nameWithoutExtension == bookFile.takeLastWhile { it != '/' } }
                    }
                    when(matchingProgress){
                        null -> book.progress
                        else -> {
                            val percent = matchingProgress.readText().takeLastWhile { it != ':' }.dropLast(1).toDouble() / 100
                            book.progress.withProgress(percent)
                        }
                    }
                }
                SyncApp.SYNC_APP_NONE, SyncApp.UNRECOGNIZED -> book.progress
            }
        }
    }.onStart { emit { book -> book.progress } }

    private val bookFilesFlow: Flow<List<File>> = allFilesFlow
        .map { r -> r.getOrElse { emptyList() }.filter { it.extension in ACCEPTED_EXTENSIONS } }

    val savingPublicationsFlow = combine(
        bookFilesFlow,
        databaseBookFlow,
        getProgressFlow,
    ) { files, savedBooks, getProgress ->
        Triple(files, savedBooks, getProgress)
    }.mapLatest { (files, savedBooks, getProgress) ->
        getNewPublicationsFromStorage(files, savedBooks) to getProgress
    }.transformLatest { (storagePublications, getProgress) ->
        emit(true)
        updateBooksInDatabase(storagePublications, getProgress)
        emit(false)
    }.shareIn(coroutineScope, SharingStarted.Lazily, 1)

    val updateProgressFlow = combineTransform(
        databaseBookFlow,
        getProgressFlow,
    ) { books, getProgress ->
        emit(true)
        updateProgressInDatabase(books, getProgress)
        emit(false)
    }

    val syncingWithDatabaseFlow = combine(
        savingPublicationsFlow,
        updateProgressFlow,
    ) { saving, updating -> saving || updating }

    val fullBooks = databaseBookFlow.map { books ->
        books.map { book -> book.toBook(cover = coverStorage.getCover(book.id)) }
    }

    private data class StoragePublications(
        val newPublicationFiles: List<Pair<Publication, File>>,
        val deletedBooks: List<SavedBook>,
    )
    private suspend fun getNewPublicationsFromStorage(
        files: List<File>,
        existingBooks: List<SavedBook>,
    ): StoragePublications {
        println("getting publications from storage with ${existingBooks.size} existing books")
        val existingPaths = existingBooks.flatMap { it.filePaths }
        val currentPaths = files.map { it.path }
        val newFiles = files.filter { it.path !in existingPaths }
        val deletedBooks = existingBooks.filter { book -> book.filePaths.none { it in currentPaths } }

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

        println("done getting ${loadedBooks.size} publications from storage")
        return StoragePublications(loadedBooks, deletedBooks)
    }

    private suspend fun updateBooksInDatabase(
        storagePublications: StoragePublications,
        getProgress: (SavedBook) -> BookProgress,
    ) {
        val (newPublications, deletedBooks) = storagePublications
        val deduplicatedNewPublications = newPublications
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
        println("saving ${deduplicatedNewPublications.size} new pubs to database, deleting ${deletedBooks.size} books")
        val savedBooks: List<SavedBook> = deduplicatedNewPublications.map { (pub, files) ->
            SavedBook(
                id = UUID.randomUUID().toString(),
                identifier = pub.metadata.identifier,
                title = pub.metadata.title,
                authors = pub.metadata.authors.distinctBy { it.identifier ?: it.name },
                progress = BookProgress.Basic(addedAt = LocalDateTime.now(), timesOpened = 0L),
                length = (pub.metadata.numberOfPages ?: pub.locateProgression(1.0)?.locations?.position)?.toLong(),
                filePaths = files.map{ it.path },
            ).let { it.copy(progress = getProgress(it)) }
        }
        deduplicatedNewPublications.zip(savedBooks).map { (pub, book) ->
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
        println("done saving ${savedBooks.size} publications to database")
    }

    suspend fun updateProgressInDatabase(books: List<SavedBook>, getProgress: (SavedBook) -> BookProgress) {
        val withUpdatedProgress = books.filter { book -> book.progress != getProgress(book) }
        withUpdatedProgress.forEach {
            database.savedBookQueries.updateProgress(getProgress(it), it.id)
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
    rememberCoroutineScope().launch {
        syncingWithDatabaseFlow.collect {
            if (permissionState.isGranted) isFirstLoad.value = false
        }
    }
    return fullBooks.map { books ->
        when {
            !permissionState.isGranted -> BooksState.NoPermission
            isFirstLoad.value && books.isEmpty() -> BooksState.Loading
            else -> books.categorize()
        }
    }.collectAsState(initial = BooksState.Loading)
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
                    when(book.progress.markedAs) {
                        LibraryShelf.CurrentlyReading -> Triple((currentlyReading + book), unread, doneOrBackburner)
                        LibraryShelf.UpNext -> Triple(currentlyReading, (unread + book), doneOrBackburner)
                        LibraryShelf.DoneOrBackburner -> Triple(currentlyReading, unread, (doneOrBackburner + book))
                        null -> when {
                            book.progress.percent >= AlreadyReadProgress || beforeBackburnerTime ->
                                Triple(currentlyReading, unread, (doneOrBackburner + book))
                            book.progress.percent == 0.0 ->
                                Triple(currentlyReading, (unread + book), doneOrBackburner)
                            else ->
                                Triple((currentlyReading + book), unread, doneOrBackburner)
                        }
                    }
                }
                is BookProgress.Basic -> {
                    when(book.progress.markedAs) {
                        LibraryShelf.CurrentlyReading -> Triple((currentlyReading + book), unread, doneOrBackburner)
                        LibraryShelf.UpNext -> Triple(currentlyReading, (unread + book), doneOrBackburner)
                        LibraryShelf.DoneOrBackburner -> Triple(currentlyReading, unread, (doneOrBackburner + book))
                        null -> when {
                            beforeBackburnerTime ->
                                Triple(currentlyReading, unread, (doneOrBackburner + book))
                            book.progress.lastOpened == null || book.progress.timesOpened == 0L  ->
                                Triple(currentlyReading, (unread + book), doneOrBackburner)
                            else ->
                                Triple((currentlyReading + book), unread, doneOrBackburner)
                        }
                    }
                }
            }
        }
    return BooksState.Loaded(currentlyReading, unread, doneOrBackburner, sorted)
}