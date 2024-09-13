package com.andb.apps.biblio.data

import android.content.Context
import com.andb.apps.biblio.SavedBook
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.io.File

sealed class SyncApp {
    data object MoonReader: SyncApp()
}

fun Flow<Pair<File, Boolean>>.toProgressFileFlow() = this.map { (file, _) ->
    println("file flow updated")
    file.walkTopDown().filter { it.extension == "po" }.toList()
}

fun SavedBook.getProgressFor(
    progressFiles: List<File>,
    app: SyncApp = SyncApp.MoonReader,
): BookProgress {
    val matchingProgress = progressFiles.find { progressFile ->
        this.filePaths.any { bookFile -> progressFile.nameWithoutExtension == bookFile.takeLastWhile { it != '/' } }
    }
    return when(matchingProgress){
        null -> this.progress
        else -> {
            val percent = when(app){
                SyncApp.MoonReader -> matchingProgress.readText().takeLastWhile { it != ':' }.dropLast(1).toDouble() / 100
            }
            this.progress.withProgress(percent)
        }
    }
}

//fun File.watchAsFlow(): Flow<File> = flow {
//        emit(this@watchAsFlow)
//        val watchService = this@watchAsFlow.toPath().fileSystem.newWatchService()
//
//        // Register for file create, delete, and modify events
//        this@watchAsFlow.toPath().register(watchService, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY)
//
//        try {
//            while (true) {
//                val key = watchService.take()
//                key.pollEvents().forEach { event ->
//                    val eventPath = event.context() as Path
//                    emit(this@watchAsFlow.resolve(eventPath.toFile()))  // Emit the File object
//                }
//                if (!key.reset()) break
//            }
//        } finally {
//            watchService.close()
//        }
//}.flowOn(Dispatchers.IO)