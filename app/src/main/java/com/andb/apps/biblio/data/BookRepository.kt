package com.andb.apps.biblio.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.readium.r2.shared.publication.Publication

class BookRepository {
    fun books(): StateFlow<Book> {
        MutableStateFlow(TODO())
    }
}