package com.andb.apps.biblio.data

import android.content.Context
import android.os.Environment
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.measureTimedValue

private val ROOT_DIR_PATH: String = Environment.getExternalStorageDirectory().absolutePath
private val ROOT_DIR = File(ROOT_DIR_PATH)
private val ANDROID_DIR = File("$ROOT_DIR_PATH/Android")
private val DATA_DIR = File("$ROOT_DIR_PATH/data")

fun CoroutineScope.walkFileFlow(
    context: Context,
    withFilesInterval: Duration = 10.seconds,
    withoutFilesInterval: Duration = 1.seconds,
): SharedFlow<Result<List<File>>> {
    val sharedFlow = MutableSharedFlow<Result<List<File>>>(replay = 1)
    val appStorage = context.getExternalFilesDir(null)!!

    suspend fun walkFiles(root: File): Result<List<File>> = withContext(Dispatchers.IO) {
        try {
            measureTimedValue {
                val files = root.walk()
                    .onEnter { dir -> dir != ANDROID_DIR &&
                                dir != DATA_DIR &&
                                !File(dir, ".nomedia").exists()
                    }
                    .toList()
                Result.success(files)
            }
            .also { println("took ${it.duration} to walk files") }
            .value
        } catch (e: SecurityException) {
            Result.failure(e) // Handle no permission error
        } catch (e: Exception) {
            Result.failure(e) // Handle other errors
        }
    }

    this.launch {
        while (true) {
            val externalResult = walkFiles(ROOT_DIR)
            val appResult = walkFiles(appStorage)
            val result = when {
                externalResult.isFailure -> externalResult
                appResult.isFailure -> appResult
                else -> Result.success(externalResult.getOrNull()!! + appResult.getOrNull()!!)
            }
            sharedFlow.emit(result) // Emit the result to the SharedFlow
            delay(
                when {
                    result.isSuccess -> withFilesInterval.inWholeMilliseconds
                    else -> withoutFilesInterval.inWholeMilliseconds
                }
            )
        }
    }

    return sharedFlow
}
