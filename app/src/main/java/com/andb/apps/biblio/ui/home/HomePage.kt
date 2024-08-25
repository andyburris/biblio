package com.andb.apps.biblio.ui.home

import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.provider.Settings
import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.andb.apps.biblio.BuildConfig
import com.andb.apps.biblio.data.BookRepository
import com.andb.apps.biblio.ui.common.BiblioButton
import com.andb.apps.biblio.ui.common.ButtonStyle
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import org.readium.r2.shared.publication.Publication
import java.text.SimpleDateFormat

sealed class BooksState {
    data object Loading : BooksState()
    data object NoPermission : BooksState()
    data class Loaded(val books: List<Publication>) : BooksState()
}

val RETURN_URI = Uri.parse("package:${BuildConfig.APPLICATION_ID}")

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun HomePage(
    modifier: Modifier = Modifier,
    onNavigateToApps: () -> Unit,
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val readium = remember { ReadiumUtils(context) }

    val bookRepository = remember { BookRepository(readium) }
    val allBooks = remember { mutableStateOf<BooksState>(BooksState.Loading) }
    LaunchedEffect(Unit) {
        if (!Environment.isExternalStorageManager()) {
            allBooks.value = BooksState.NoPermission
        } else {
            allBooks.value = bookRepository.getPublications().let { BooksState.Loaded(it) }
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        Column(
            Modifier
                .fillMaxWidth()
                .weight(1f),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            when(val publications = allBooks.value) {
                BooksState.Loading -> Text(
                    modifier = Modifier.padding(64.dp),
                    text = "Loading...",
                )
                BooksState.NoPermission -> Column(
                    modifier = Modifier.padding(64.dp)
                ) {
                    Text(text = "Biblio needs storage permissions to access your books")
                    BiblioButton(
                        onClick = {
                            Log.d("HomePage", "Requesting storage permissions")
                            context.startActivity(
                                Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION, RETURN_URI)
                            )
                        },
                        style = ButtonStyle.Outline,
                    ) {
                        Text(text = "Allow storage permissions")
                    }
                }
                is BooksState.Loaded -> when(publications.books.isNotEmpty()) {
                    true -> LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(32.dp),
                        contentPadding = PaddingValues(horizontal = 64.dp)
                    ) {
                        items(publications.books) { book ->
                            BookItem(publication = book)
                        }
                    }
                    false -> Text(text = "No books found")
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
                onClick = onNavigateToApps,
                style = ButtonStyle.Outline,
            ) {
                Text(text = "Apps")
            }
        }
    }
}