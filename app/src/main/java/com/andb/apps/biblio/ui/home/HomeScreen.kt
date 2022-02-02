package com.andb.apps.biblio.ui.home

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext

@Composable
fun HomeScreen() {
    Column(Modifier.fillMaxSize()) {
        StatusBar(modifier = Modifier.fillMaxWidth())
    }
}