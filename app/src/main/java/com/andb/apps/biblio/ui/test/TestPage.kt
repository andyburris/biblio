package com.andb.apps.biblio.ui.test

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.andb.apps.biblio.data.SyncServer
import com.andb.apps.biblio.ui.common.BiblioBottomBar
import com.andb.apps.biblio.ui.common.BiblioScaffold
import com.andb.apps.biblio.ui.common.ExactText
import com.andb.apps.biblio.ui.theme.BiblioTheme
import java.io.File

@Composable
fun TestPage(
    server: SyncServer,
    modifier: Modifier = Modifier,
    onNavigateBack: () -> Unit,
) {
    val context = LocalContext.current

    val file = server.fileFlow.collectAsState().value.first
    LaunchedEffect(file) {
        println("file flow updated in composition")
    }
    val files = file.walkTopDown().toList()

    BiblioScaffold(
        modifier = modifier,
        bottomBar = { BiblioBottomBar("Test", onNavigateBack = onNavigateBack) },
    ) {
        LazyColumn(
            modifier = Modifier
        ) {
            item {
                ExactText(
                    "Server running at: ${server.myServerSocket}",
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .background(BiblioTheme.colors.surface)
                        .padding(16.dp)
                )
            }
            item {
                ExactText("Files in ${file.absolutePath}")
            }
            items(files) { file ->
                TestFileItem(file = file)
            }
        }
    }
}

@Composable
private fun TestFileItem(
    file: File,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .padding(16.dp)
    ) {
        ExactText(
            file.absolutePath,
        )
    }
}