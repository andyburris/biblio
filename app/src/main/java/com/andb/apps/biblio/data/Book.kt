package com.andb.apps.biblio.data

import org.readium.r2.shared.publication.Publication

data class Book(
    val title: String,
)

fun Publication.toBook() = Book(
    title = this.metadata.title,
)