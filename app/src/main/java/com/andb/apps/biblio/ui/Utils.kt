package com.andb.apps.biblio.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

@Composable
fun <T> State<T>.asStateFlow(): StateFlow<T> {
    val stateFlow = remember { MutableStateFlow(value) }
    LaunchedEffect(value) {
        stateFlow.value = value
    }
    return stateFlow
}