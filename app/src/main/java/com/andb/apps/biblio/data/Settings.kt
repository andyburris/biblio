package com.andb.apps.biblio.data

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.datastore.core.CorruptionException
import androidx.datastore.core.DataStore
import androidx.datastore.core.Serializer
import androidx.datastore.dataStore
import com.adamglin.PhosphorIcons
import com.adamglin.phosphoricons.Regular
import com.adamglin.phosphoricons.regular.Arrowsclockwise
import com.adamglin.phosphoricons.regular.Books
import com.adamglin.phosphoricons.regular.Percent
import com.adamglin.phosphoricons.regular.Pushpin
import com.adamglin.phosphoricons.regular.Squaresfour
import com.andb.apps.biblio.LibraryView
import com.andb.apps.biblio.Settings
import com.andb.apps.biblio.SyncApp
import com.google.protobuf.InvalidProtocolBufferException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.InputStream
import java.io.OutputStream

data class SettingState<T>(
    val name: String,
    val value: T,
    private val onUpdate: suspend (T) -> Unit,
    val isActivated: Boolean?,
    val icon: ImageVector,
    val stateDescription: String,
) {
    fun update(newValue: T) {
        CoroutineScope(Dispatchers.IO).launch {
            onUpdate(newValue)
        }
    }
}

data class SyncInfo(
    val app: SyncApp,
    val username: String,
    val password: String,
)
data class SettingsState(
    val settings: Settings,
    private val onUpdateSettings: suspend (transform: suspend (currentSettings: Settings) -> Settings) -> Settings,
) {
    data class CommonSettings(
        val showNumbers: SettingState<Boolean>,
        val syncState: SettingState<SyncInfo>
    )
    val common = CommonSettings(
        showNumbers = SettingState(
            name = "Show Numbers",
            value = settings.common.showNumbers,
            onUpdate = {
                onUpdateSettings { currentSettings ->
                    currentSettings.toBuilder().setCommon(currentSettings.common.toBuilder().setShowNumbers(it)).build()
                }
            },
            isActivated = settings.common.showNumbers,
            icon = PhosphorIcons.Regular.Percent,
            stateDescription = if(settings.common.showNumbers) "On" else "Off"
        ),
        syncState = SettingState(
            name = "Sync with reader app",
            value = SyncInfo(settings.common.syncApp, "biblio", "a378d4"),
            onUpdate = {
                onUpdateSettings { currentSettings ->
                    currentSettings.toBuilder().setCommon(
                        currentSettings.common.toBuilder()
                            .setSyncApp(it.app)
//                            .setSyncUsername(it.username)
//                            .setSyncPassword(it.password)
                    ).build()
                }
            },
            isActivated = settings.common.syncApp != SyncApp.SYNC_APP_NONE,
            icon = PhosphorIcons.Regular.Arrowsclockwise,
            stateDescription = settings.common.syncApp.name
        )
    )

    data class LibrarySettings(
        val view: SettingState<LibraryView>,
    )
    val library = LibrarySettings(
        view = SettingState(
            name = "Library view",
            value = settings.library.view,
            onUpdate = {
                onUpdateSettings { currentSettings ->
                    currentSettings.toBuilder().setLibrary(currentSettings.library.toBuilder().setView(it)).build()
                }
            },
            isActivated = settings.library.view != LibraryView.UNRECOGNIZED,
            icon = when(settings.library.view) {
                LibraryView.LIBRARY_VIEW_GRID -> PhosphorIcons.Regular.Squaresfour
                else -> PhosphorIcons.Regular.Books
            },
            stateDescription = when(settings.library.view) {
                LibraryView.LIBRARY_VIEW_GRID -> "Grid"
                LibraryView.LIBRARY_VIEW_SHELVES -> "Shelves"
                else -> "Unrecognized"
            }
        )
    )

    data class AppsSettings(
        val pinnedApps: SettingState<List<String>>,
    )
    val apps = AppsSettings(
        pinnedApps = SettingState(
            name = "Pinned Apps",
            value = settings.home.pinnedAppsList,
            onUpdate = {
                onUpdateSettings { currentSettings ->
                    currentSettings.toBuilder().setHome(currentSettings.home.toBuilder()
                        .clearPinnedApps()
                        .addAllPinnedApps(it))
                    .build()
                }
            },
            isActivated = settings.home.pinnedAppsList.isNotEmpty(),
            icon = PhosphorIcons.Regular.Pushpin,
            stateDescription = if(settings.home.pinnedAppsList.isEmpty()) "None" else settings.home.pinnedAppsList.joinToString { it }
        )
    )
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