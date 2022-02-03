package com.andb.apps.biblio

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.twotone.Book
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.andb.apps.biblio.data.Book
import com.andb.apps.biblio.data.BookRepository
import com.andb.apps.biblio.ui.theme.BiblioTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val paths = assets.list("/")!!.toList()
        println("paths = $paths")
        setContent {
            BiblioTheme {
                // A surface container using the 'background' color from the theme
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colors.background) {
                    val books = remember { BookRepository().books(paths, assets) }.collectAsState()
                    LazyColumn {
                        items(books.value) {
                            BookItem(book = it)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BookItem(book: Book) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .background(Color.Black.copy(alpha = 0.05f), CircleShape)
                .size(40.dp),
            contentAlignment = Alignment.Center,
        ) {
            Icon(imageVector = Icons.TwoTone.Book, contentDescription = null)
        }
        Text(text = book.title)
    }
}