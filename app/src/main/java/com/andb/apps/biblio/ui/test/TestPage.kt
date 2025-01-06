package com.andb.apps.biblio.ui.test

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.andb.apps.biblio.data.Book
import com.andb.apps.biblio.data.BookCover
import com.andb.apps.biblio.data.BooksState
import com.andb.apps.biblio.ui.common.BiblioBottomBar
import com.andb.apps.biblio.ui.common.BiblioScaffold
import com.andb.apps.biblio.ui.common.rotateWithBounds
import com.andb.apps.biblio.ui.home.ImageCover
import com.andb.apps.biblio.ui.home.joinAuthorsToString
import com.andb.apps.biblio.ui.theme.BiblioTheme

@Composable
fun TestPage(
    modifier: Modifier = Modifier,
    books: BooksState,
    onNavigateBack: () -> Unit,
) {
    BiblioScaffold(
        modifier = modifier,
        bottomBar = {
            BiblioBottomBar(pageTitle = "Test", onNavigateBack = onNavigateBack)
        }
    ) {
        BookDataList(books = books)
    }
}

@Composable
private fun BookDataList(
    books: BooksState,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        when (books) {
            is BooksState.Loaded -> books.allBooks.forEach { book ->
                BookDataItem(book = book)
            }
            is BooksState.Loading -> Text(text = "Loading...")
            is BooksState.NoPermission -> Text("No permission")
        }
    }
}

@Composable
private fun BookDataItem(
    book: Book,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        when(book.cover) {
            is BookCover.Available -> ImageCover(
                cover = book.cover,
                publication = book,
                height = 64.dp,
                modifier = Modifier.heightIn(max = 64.dp)
            )
            BookCover.Unavailable -> {}
        }
        Column {
            Text(book.title ?: "Untitled", style = BiblioTheme.typography.title)
            Text("authors = ${book.authors.joinAuthorsToString()}")
            Text("id = ${book.id}")
            Text("identifier = ${book.identifier}")
            Text("progress = ${book.progress}")
            Text("length = ${book.length}")
            Text("filePaths = ${book.filePaths}")
        }
    }
}

@Composable
private fun RotationTest(
    modifier: Modifier = Modifier,
) {
    val infiniteTransition = rememberInfiniteTransition("rotation")
    val rotation = infiniteTransition.animateFloat(
        label = "rotation",
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(5000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        )
    )

    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier
                    .size(32.dp)
                    .background(Color.Red))
            Column(
                modifier = Modifier
                    .border(1.dp, Color.Green)
                    .background(BiblioTheme.colors.surface)
                    .width(100.dp)
                    .height(200.dp)
                    .rotateWithBounds(rotation.value)
                    .padding(16.dp)
            ) {
                Text(
                    "Testing a decently long string of text",
                    modifier = Modifier
                        .border(1.dp, Color.Red)
                        .fillMaxSize()
                )
            }
            Box(
                Modifier
                    .size(32.dp)
                    .background(Color.Red))
        }
    }
}