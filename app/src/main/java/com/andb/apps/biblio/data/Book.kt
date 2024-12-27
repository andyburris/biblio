package com.andb.apps.biblio.data

import android.graphics.Bitmap
import com.andb.apps.biblio.SavedBook
import com.andb.apps.biblio.ui.library.LibraryShelf
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import org.readium.r2.shared.publication.Contributor
import java.time.LocalDateTime

sealed class BookCover {
    data object Unavailable : BookCover()
    data class Available(
        val image: Bitmap,
        val blurredSpine: Bitmap,
        val brightenedImage: Bitmap,
        val isDark: Boolean,
    ) : BookCover()
}
data class Book(
    val id: String,
    val identifier: String?,
    val title: String?,
    val authors: List<Contributor>,
    val cover: BookCover,
    val progress: BookProgress,
    val length: Long?,
    val filePaths: List<String>,
)

fun SavedBook.toBook(cover: BookCover) = Book(
    id = id,
    identifier = identifier,
    title = title,
    authors = authors,
    cover = cover,
    progress = progress,
    length = length,
    filePaths = filePaths
)

fun Book.toSavedBook() = SavedBook(
    id = id,
    identifier = identifier,
    title = title,
    authors = authors,
    progress = progress,
    length = length,
    filePaths = filePaths
)

const val AlreadyReadProgress = 0.925
@Serializable
sealed class BookProgress() {
    abstract val lastOpened: LocalDateTime?
    abstract val addedAt: LocalDateTime
    abstract val timesOpened: Long
    abstract val markedAs: LibraryShelf?

    @Serializable data class Progress(
        val percent: Double,
        @Serializable(with = LocalDateTimeSerializer::class)
        override val addedAt: LocalDateTime,
        override val timesOpened: Long,
        @Serializable(with = LocalDateTimeSerializer::class)
        override val lastOpened: LocalDateTime? = null,
        override val markedAs: LibraryShelf? = null,
        ) : BookProgress()
    @Serializable data class Basic(
        @Serializable(with = LocalDateTimeSerializer::class)
        override val addedAt: LocalDateTime,
        override val timesOpened: Long,
        @Serializable(with = LocalDateTimeSerializer::class)
        override val lastOpened: LocalDateTime? = null,
        override val markedAs: LibraryShelf? = null,
        ) : BookProgress()

    fun increaseOpened(): BookProgress {
        return when(this) {
            is Progress -> copy(
                timesOpened = timesOpened + 1,
                lastOpened = LocalDateTime.now(),
                markedAs = null
            )
            is Basic -> copy(
                timesOpened = timesOpened + 1,
                lastOpened = LocalDateTime.now(),
                markedAs = null
            )
        }
    }

    fun toUpNext(): BookProgress {
        return when(this) {
            is Progress -> copy(markedAs = LibraryShelf.UpNext)
            is Basic -> copy(markedAs = LibraryShelf.UpNext)
        }
    }

    fun toCurrentlyReading(): BookProgress {
        return when(this) {
            is Progress -> copy(markedAs = LibraryShelf.CurrentlyReading)
            is Basic -> copy(markedAs = LibraryShelf.CurrentlyReading)
        }
    }

    fun toAlreadyRead(): BookProgress {
        return when(this) {
            is Progress -> copy(markedAs = LibraryShelf.DoneOrBackburner)
            is Basic -> copy(markedAs = LibraryShelf.DoneOrBackburner)
        }
    }

    fun withProgress(percent: Double): Progress {
        return when(this) {
            is Progress -> this.copy(percent = percent)
            is Basic -> Progress(percent, addedAt, timesOpened, lastOpened)
        }
    }
}

private object LocalDateTimeSerializer : KSerializer<LocalDateTime> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("LocalDateTime", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: LocalDateTime) {
        val string = value.toString()
        encoder.encodeString(string)
    }

    override fun deserialize(decoder: Decoder): LocalDateTime {
        val string = decoder.decodeString()
        return LocalDateTime.parse(string)
    }
}