package com.andb.apps.biblio.data

import android.graphics.Bitmap
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
    data class Available(val image: Bitmap) : BookCover()
}
data class Book(
    val id: String,
    val title: String?,
    val authors: List<Contributor>,
    val cover: BookCover,
    val progress: BookProgress,
    val length: Int?,
    val filePath: String,
)

@Serializable
sealed class BookProgress() {
    abstract val lastOpened: LocalDateTime
    abstract val timesOpened: Long

    @Serializable data class Progress(
        val percent: Double,
        @Serializable(with = LocalDateTimeSerializer::class)
        override val lastOpened: LocalDateTime,
        override val timesOpened: Long,
    ) : BookProgress()
    @Serializable data class Basic(
        @Serializable(with = LocalDateTimeSerializer::class)
        override val lastOpened: LocalDateTime,
        override val timesOpened: Long,
    ) : BookProgress()
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