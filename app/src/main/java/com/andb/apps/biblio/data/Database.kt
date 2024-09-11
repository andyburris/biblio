package com.andb.apps.biblio.data

import android.content.Context
import app.cash.sqldelight.ColumnAdapter
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.andb.apps.biblio.BiblioDatabase
import com.andb.apps.biblio.SavedBook
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encodeToString
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json
import org.json.JSONObject
import org.readium.r2.shared.publication.Contributor

fun createDatabase(context: Context): BiblioDatabase {
    val driver = AndroidSqliteDriver(BiblioDatabase.Schema, context, "biblio.db")
    val database = BiblioDatabase(
        driver = driver,
        savedBookAdapter = SavedBook.Adapter(
            authorsAdapter = AuthorsAdapter,
            progressAdapter = ProgressAdapter,
            filePathsAdapter = StringListAdapter,
        )
    )
    return database
}

private object StringListAdapter: ColumnAdapter<List<String>, String> {
    override fun decode(databaseValue: String): List<String> = Json.decodeFromString(databaseValue)
    override fun encode(value: List<String>): String = Json.encodeToString(value)
}

private object AuthorsAdapter: ColumnAdapter<List<Contributor>, String> {
    override fun decode(databaseValue: String): List<Contributor> = Json.decodeFromString(
        deserializer = ListSerializer(ContributorAsStringSerializer),
        string = databaseValue,
    )
    override fun encode(value: List<Contributor>): String = Json.encodeToString(
        serializer = ListSerializer(ContributorAsStringSerializer),
        value = value
    )
}

private object ContributorAsStringSerializer : KSerializer<Contributor> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("Contributor", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: Contributor) {
        val string = value.toJSON().toString(0)
        encoder.encodeString(string)
    }

    override fun deserialize(decoder: Decoder): Contributor {
        val string = decoder.decodeString()
        return Contributor.fromJSON(JSONObject(string))!!
    }
}

private object ProgressAdapter: ColumnAdapter<BookProgress, String> {
    override fun decode(databaseValue: String): BookProgress = Json.decodeFromString<BookProgress>(databaseValue)
    override fun encode(value: BookProgress): String = Json.encodeToString(value)
}