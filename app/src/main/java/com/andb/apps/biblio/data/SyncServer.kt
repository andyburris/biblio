package com.andb.apps.biblio.data

import com.andb.apps.biblio.SavedBook
import com.andb.apps.biblio.SyncApp
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import org.nanohttpd.protocols.http.IHTTPSession
import org.nanohttpd.protocols.http.NanoHTTPD
import org.nanohttpd.protocols.http.request.Method
import org.nanohttpd.protocols.http.response.Response
import org.nanohttpd.protocols.http.response.Response.newChunkedResponse
import org.nanohttpd.protocols.http.response.Response.newFixedLengthResponse
import org.nanohttpd.protocols.http.response.Status
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import kotlin.io.path.Path
import kotlin.io.path.createParentDirectories

class SyncServer(
    private val directory: File,
    port: Int = 8080,
) : NanoHTTPD("127.0.0.1", port) {
    private val mutableFileFlow = MutableStateFlow((directory to false))
    val fileFlow: StateFlow<Pair<File, Boolean>> = mutableFileFlow

    override fun serve(session: IHTTPSession): Response {
        println("Request: ${session.method} ${session.uri}")
        val uri = session.uri
        val file = File(directory, uri)

        val ret = when (session.method) {
            Method.GET -> handleGet(file)
            Method.PROPFIND -> handlePropfind(file, session.headers.get("depth"), session)
            Method.PUT -> handlePut(file, session)
            Method.DELETE -> handleDelete(file)
            Method.MKCOL -> handleMkcol(file)
            else -> newFixedLengthResponse(Status.METHOD_NOT_ALLOWED, MIME_PLAINTEXT, "Method not allowed")
        }

        mutableFileFlow.value = mutableFileFlow.value.first to !mutableFileFlow.value.second

        println("Request: ${session.method} ${session.uri} -> ${ret.status}")
        return ret
    }

    private fun handleGet(file: File): Response {
        if (!file.exists()) {
            return newFixedLengthResponse(Status.NOT_FOUND, MIME_PLAINTEXT, "File not found")
        }
        if (file.isDirectory) {
            return newFixedLengthResponse(Status.FORBIDDEN, MIME_PLAINTEXT, "Cannot GET a directory")
        }
        return try {
            val fis = FileInputStream(file)
            newChunkedResponse(Status.OK, getMimeTypeForFile(file.name), fis)
        } catch (e: FileNotFoundException) {
            newFixedLengthResponse(Status.NOT_FOUND, MIME_PLAINTEXT, "File not found")
        }
    }

    private fun handlePropfind(file: File, depth: String?, session: IHTTPSession): Response {
        // Parse depth: "infinity" = -1, null or invalid = 0, otherwise use the provided integer value
        val depthInt = when (depth) {
            "infinity" -> -1
            else -> depth?.toIntOrNull() ?: 0
        }

        val properties = StringBuilder()
        val xml = StringBuilder()

        fun addFileProperties(f: File, level: Int) {
            if (level < 0) return // Stop recursion when level is negative
            val path = f.path.replace("\\", "/")
            properties.append("<d:response xmlns:d=\"DAV:\" xmlns:s=\"http://schemas.xmlsoap.org/soap/envelope/\">")
            properties.append("<d:href>$path</d:href>")
            properties.append("<d:propstat><d:prop>")
            properties.append("<d:displayname>${f.name}</d:displayname>")
            properties.append("<d:creationdate>${f.lastModified()}</d:creationdate>")
            properties.append("<d:getcontentlength>${f.length()}</d:getcontentlength>")
            properties.append("</d:prop></d:propstat></d:response>")

            // If it's a directory and depth is 1 or infinity (-1), add children
            if (f.isDirectory && (depthInt == 1 || depthInt == -1)) {
                f.listFiles()?.forEach { child ->
                    addFileProperties(child, if (depthInt == -1) -1 else level - 1)
                }
            }
        }

        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>")
        xml.append("<d:multistatus xmlns:d=\"DAV:\">")
        addFileProperties(file, depthInt)
        xml.append(properties)
        xml.append("</d:multistatus>")

        return newFixedLengthResponse(Status.OK, "application/xml", xml.toString())
    }

    private fun handlePut(file: File, session: IHTTPSession): Response {
        return try {
            val contentLength = session.headers["content-length"]?.toInt() ?: 0
            val inputStream = session.inputStream
            if(!file.exists()) {
                Path(file.path).createParentDirectories()
                file.createNewFile()
            }
            FileOutputStream(file).use { fos ->
                val buffer = ByteArray(1024)
                var len: Int = -1
                var read: Long = 0
                while (read < contentLength && inputStream.read(buffer).also { len = it } != -1) {
                    fos.write(buffer, 0, len)
                    read += len
                }
            }
            newFixedLengthResponse(Status.CREATED, MIME_PLAINTEXT, "File created/updated")
        } catch (e: IOException) {
            e.printStackTrace()
            newFixedLengthResponse(Status.INTERNAL_ERROR, MIME_PLAINTEXT, "Error writing file")
        }
    }

    private fun handleDelete(file: File): Response {
        if (!file.exists()) {
            return newFixedLengthResponse(Status.NOT_FOUND, MIME_PLAINTEXT, "File not found")
        }
        return if (file.delete()) {
            newFixedLengthResponse(Status.NO_CONTENT, MIME_PLAINTEXT, "")
        } else {
            newFixedLengthResponse(Status.FORBIDDEN, MIME_PLAINTEXT, "Could not delete file")
        }
    }

    private fun handleMkcol(file: File): Response {
        if (file.exists()) {
            return newFixedLengthResponse(Status.METHOD_NOT_ALLOWED, MIME_PLAINTEXT, "Collection already exists")
        }
        return if (file.mkdir()) {
            newFixedLengthResponse(Status.CREATED, MIME_PLAINTEXT, "Collection created")
        } else {
            newFixedLengthResponse(Status.CONFLICT, MIME_PLAINTEXT, "Could not create collection")
        }
    }
}


fun Flow<Pair<File, Boolean>>.toProgressFileFlow() = this.map { (file, _) ->
    file.walkTopDown().filter { it.extension == "po" }.toList()
}

fun SavedBook.getProgressFor(
    progressFiles: List<File>,
    app: SyncApp = SyncApp.SYNC_APP_MOON_READER,
): BookProgress {
    val matchingProgress = progressFiles.find { progressFile ->
        this.filePaths.any { bookFile -> progressFile.nameWithoutExtension == bookFile.takeLastWhile { it != '/' } }
    }
    return when(matchingProgress){
        null -> this.progress
        else -> when(app) {
            SyncApp.SYNC_APP_MOON_READER -> {
                val percent = matchingProgress.readText().takeLastWhile { it != ':' }.dropLast(1).toDouble() / 100
                this.progress.withProgress(percent)
            }
            else -> this.progress
        }
    }
}