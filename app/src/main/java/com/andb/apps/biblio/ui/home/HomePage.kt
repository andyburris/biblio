package com.andb.apps.biblio.ui.home

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.BatteryManager
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewModelScope
import com.andb.apps.biblio.ui.common.BiblioButton
import com.andb.apps.biblio.ui.common.ButtonStyle
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.util.getOrElse
import org.readium.r2.shared.util.toAbsoluteUrl
import java.text.SimpleDateFormat
import java.util.Date

@Composable
fun HomePage(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val readium = remember { ReadiumUtils(context) }
    val openPublication = remember { mutableStateOf<Publication?>(null) }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if(uri == null) return@rememberLauncherForActivityResult
        val url = requireNotNull(uri.toAbsoluteUrl())
        coroutineScope.launch {
            val asset = readium.assetRetriever.retrieve(url)
                .getOrElse {
                    return@launch
                }

            val publication = readium.publicationOpener.open(asset, allowUserInteraction = false)
                .getOrElse {
                    asset.close()
                    return@launch
                }
            Log.d("HomePage", "Publication toc: ${publication.manifest.tableOfContents}")
            Log.d("HomePage", "Publication context: ${publication.manifest.context}")
            Log.d("HomePage", "Publication resources: ${publication.manifest.resources}")
            Log.d("HomePage", "Publication readingOrder: ${publication.manifest.readingOrder}")
            Log.d("HomePage", "Publication metadata: ${publication.metadata}")
            openPublication.value = publication
        }
    }


    Column(modifier = modifier.fillMaxSize()) {
        Box(
            Modifier
                .fillMaxWidth()
                .weight(1f),
            contentAlignment = Alignment.Center,
        ) {
            when(val publication = openPublication.value) {
                null -> Button(onClick = { launcher.launch(arrayOf("*/*")) }) {
                    Text(text = "Open eBook")
                }
                else -> {
                    BookItem(publication = publication, Modifier.clickable { openPublication.value = null })
                }
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp, horizontal = 32.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val time = currentTimeAsState()
            val formatter = SimpleDateFormat("h:mm", java.util.Locale.ROOT)
            Text(text = formatter.format(time.value))

            BiblioButton(
                onClick = { /*TODO*/ },
                style = ButtonStyle.Outline,
            ) {
                val batteryState = currentBatteryAsState()
                Text(text = "${Math.round(batteryState.value.percent * 100)}%")
            }

            Spacer(modifier = Modifier.weight(1f))

            BiblioButton(
                onClick = { /*TODO*/ },
                style = ButtonStyle.Outline,
            ) {
                Text(text = "Apps")
            }
        }
    }
}