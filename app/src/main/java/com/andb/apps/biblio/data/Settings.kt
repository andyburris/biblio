package com.andb.apps.biblio.data

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.datastore.core.CorruptionException
import androidx.datastore.core.DataStore
import androidx.datastore.core.Serializer
import androidx.datastore.dataStore
import com.andb.apps.biblio.Settings
import com.google.protobuf.InvalidProtocolBufferException
import java.io.InputStream
import java.io.OutputStream

//data class Settings(
//    val common: Common,
//    val home: Home,
//    val library: Library,
//) {
//    data class Common(
//        val showNumbers: Boolean,
//        val syncApp: SyncApp,
//    )
//    data class Home(
//        val pinnedApps: List<String>,
//    )
//    data class Library(
//        val view: View,
//    ) {
//        enum class View { Shelves, Grid }
//    }
//}
//
data class SettingsState(
    val settings: Settings,
    private val onUpdateSettings: suspend (transform: suspend (currentSettings: Settings) -> Settings) -> Settings,
) {
    suspend fun updateSettings(transform: suspend (t: Settings) -> Settings): Settings {
        return onUpdateSettings(transform)
    }
}

@Composable
fun DataStore<Settings>.rememberSettingsState(): SettingsState {
    val currentSettings: Settings = this.data.collectAsState(Settings.getDefaultInstance()).value
    val settingsState = remember(currentSettings) {
        SettingsState(currentSettings) { transform ->
            this.updateData { currentSettings ->
                transform(currentSettings)
            }
        }
    }
    return settingsState
}

val LocalSettings = compositionLocalOf {
    SettingsState(Settings.getDefaultInstance()) { transform ->
        Settings.getDefaultInstance()
    }
}

object SettingsSerializer : Serializer<Settings> {
    override val defaultValue: Settings = Settings.getDefaultInstance()

    override suspend fun readFrom(input: InputStream): Settings {
        try {
            return Settings.parseFrom(input)
        } catch (exception: InvalidProtocolBufferException) {
            throw CorruptionException("Cannot read proto.", exception)
        }
    }

    override suspend fun writeTo(
        t: Settings,
        output: OutputStream
    ) = t.writeTo(output)
}

val Context.settingsDataStore: DataStore<Settings> by dataStore(
    fileName = "settings.pb",
    serializer = SettingsSerializer
)