package com.andb.apps.biblio

import android.os.Build
import android.os.Bundle
import android.view.WindowInsets
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.andb.apps.biblio.data.BookRepository
import com.andb.apps.biblio.data.booksAsState
import com.andb.apps.biblio.ui.apps.AppsPage
import com.andb.apps.biblio.ui.home.HomePage
import com.andb.apps.biblio.ui.home.rememberStoragePermissionState
import com.andb.apps.biblio.ui.library.LibraryPage
import com.andb.apps.biblio.ui.theme.BiblioTheme

class MainActivity : ComponentActivity() {



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val navController = rememberNavController()

            val context = LocalContext.current
            val bookRepository = remember { BookRepository(context) }
            val storagePermissionState = rememberStoragePermissionState()

            val booksState = bookRepository.booksAsState(storagePermissionState)

            BiblioTheme {
                NavHost(
                    navController = navController,
                    startDestination = "home",
                    enterTransition = { EnterTransition.None },
                    exitTransition = { ExitTransition.None },
                ) {
                    composable("home") {
                        Scaffold (
                            Modifier.fillMaxSize(),
                            containerColor = BiblioTheme.colors.background,
                        ) { innerPadding ->
                            HomePage(
                                booksState = booksState.value,
                                modifier = Modifier.padding(innerPadding),
                                onNavigateToApps = { navController.navigate("apps") },
                                onNavigateToLibrary = { navController.navigate("library") },
                                onRequestStoragePermission = { storagePermissionState.launchPermissionRequest() },
                                onOpenPublication = { bookRepository.openPublication(it) },
                            )
                        }
                    }
                    composable("apps") {
                        Scaffold (
                            Modifier.fillMaxSize(),
                            containerColor = BiblioTheme.colors.background,
                        ) { innerPadding ->
                            AppsPage(
                                modifier = Modifier.padding(innerPadding),
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }
                    }
                    composable("library") {
                        Scaffold (
                            Modifier.fillMaxSize(),
                            containerColor = BiblioTheme.colors.background,
                        ) { innerPadding ->
                            LibraryPage(
                                booksState = booksState.value,
                                modifier = Modifier.padding(innerPadding),
                                onNavigateBack = { navController.popBackStack() },
                                onOpenPublication = { bookRepository.openPublication(it) },
                            )
                        }
                    }
                }
            }
        }

        @Suppress("DEPRECATION")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.insetsController?.hide(WindowInsets.Type.systemBars())
        } else {
            window.setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN
            )
        }
    }
}