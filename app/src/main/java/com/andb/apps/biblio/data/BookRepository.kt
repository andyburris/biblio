package com.andb.apps.biblio.data

import android.content.res.AssetManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.readium.r2.shared.fetcher.Fetcher
import org.readium.r2.shared.fetcher.FileFetcher
import org.readium.r2.shared.fetcher.Resource
import org.readium.r2.shared.fetcher.ResourceTry
import org.readium.r2.shared.publication.Link
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.publication.asset.FileAsset
import org.readium.r2.shared.publication.asset.PublicationAsset
import org.readium.r2.shared.util.Try
import org.readium.r2.shared.util.mediatype.MediaType
import org.readium.r2.streamer.parser.epub.EpubParser
import java.io.File
import java.io.InputStream

class BookRepository {
    fun books(paths: List<String>, assetManager: AssetManager): StateFlow<List<Book>> {
        val parser = EpubParser()
        val stateFlow = MutableStateFlow(emptyList<Book>())
        CoroutineScope(Dispatchers.IO).launch {
            paths.map {
                parser._parse(AssetAsset(it), fetcher = AssetFetcher(assetManager), fallbackTitle = it.takeLastWhile { it != '/' })
            }
        }
        return stateFlow
    }
}

class AssetAsset(val path: String) : PublicationAsset {
    override val name: String
        get() = path.takeLastWhile { it != '/' }
    override suspend fun createFetcher(dependencies: PublicationAsset.Dependencies, credentials: String?): Try<Fetcher, Publication.OpeningException> = TODO()
    override suspend fun mediaType(): MediaType = MediaType.BINARY

}

class AssetFetcher(private val assetManager: AssetManager) : Fetcher {
    private val openedPaths = mutableListOf<String>()
    override suspend fun close() {
        assetManager.close()
    }

    override fun get(link: Link): Resource = AssetResource(link, assetManager.open(link.href))

    override suspend fun links(): List<Link> = assetManager.list(".")!!.map { Link(it) }

}

class AssetResource(private val link: Link, private val stream: InputStream) : Resource {
    override suspend fun close() {
        stream.close()
    }

    override suspend fun length(): ResourceTry<Long> {
        return Try.success(stream.available().toLong())
    }

    override suspend fun link(): Link = link

    override suspend fun read(range: LongRange?): ResourceTry<ByteArray> {
        return when(range) {
            null -> Try.success(stream.readBytes())
            else -> {
                val outArray = range.map { it.toByte() }.toByteArray()
                stream.read(outArray)
                Try.success(outArray)
            }
        }
    }

}