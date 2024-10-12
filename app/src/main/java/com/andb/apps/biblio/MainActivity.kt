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
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.andb.apps.biblio.data.BookRepository
import com.andb.apps.biblio.data.BooksState
import com.andb.apps.biblio.data.LocalSettings
import com.andb.apps.biblio.data.SettingsState
import com.andb.apps.biblio.data.SyncServer
import com.andb.apps.biblio.data.booksAsState
import com.andb.apps.biblio.data.rememberSettingsState
import com.andb.apps.biblio.data.settingsDataStore
import com.andb.apps.biblio.ui.apps.AppsPage
import com.andb.apps.biblio.ui.apps.rememberAppsAsState
import com.andb.apps.biblio.ui.home.HomePage
import com.andb.apps.biblio.ui.home.rememberStoragePermissionState
import com.andb.apps.biblio.ui.library.LibraryPage
import com.andb.apps.biblio.ui.library.LibraryShelf
import com.andb.apps.biblio.ui.library.ShelfPage
import com.andb.apps.biblio.ui.settings.SettingsPage
import com.andb.apps.biblio.ui.test.TestPage
import com.andb.apps.biblio.ui.theme.BiblioTheme

class MainActivity : ComponentActivity() {

    private lateinit var server: SyncServer

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        server = SyncServer(getExternalFilesDir(null)!!)
        server.start()

        enableEdgeToEdge()
        setContent {
            val context = LocalContext.current
            val coroutineScope = rememberCoroutineScope()
            val navController = rememberNavController()
            val storagePermissionState = rememberStoragePermissionState()
            val settings = context.settingsDataStore.rememberSettingsState()
            val bookRepository = remember { BookRepository(context, coroutineScope, server) }



            val appsState = rememberAppsAsState()
            val booksState = bookRepository.booksAsState(storagePermissionState)

            CompositionLocalProvider(LocalSettings provides settings) {
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
                                    onNavigateToSettings = { navController.navigate("settings") },
                                    onRequestStoragePermission = { storagePermissionState.launchPermissionRequest() },
                                    onOpenBook = { bookRepository.openBook(it) },
                                )
                            }
                        }
                        composable("apps") {
                            Scaffold (
                                Modifier.fillMaxSize(),
                                containerColor = BiblioTheme.colors.background,
                            ) { innerPadding ->
                                AppsPage(
                                    appsState = appsState.value,
                                    modifier = Modifier.padding(innerPadding),
                                    onNavigateBack = { navController.popBackStack() }
                                )
                            }
                        }
                        composable("library") {
                            when(val books = booksState.value) {
                                is BooksState.Loaded -> Scaffold (
                                    Modifier.fillMaxSize(),
                                    containerColor = BiblioTheme.colors.background,
                                ) { innerPadding ->
                                    LibraryPage(
                                        booksState = books,
                                        modifier = Modifier.padding(innerPadding),
                                        onNavigateBack = { navController.popBackStack() },
                                        onOpenShelf = { navController.navigate("shelf/${it.name}") },
                                        onOpenBook = { bookRepository.openBook(it) },
                                    )
                                }
                                else -> navController.popBackStack()
                            }
                        }
                        composable("shelf/{shelf}") { backStackEntry ->
                            val shelf = backStackEntry.arguments?.getString("shelf")?.let { LibraryShelf.valueOf(it) } ?: return@composable
                            when(val books = booksState.value) {
                                is BooksState.Loaded -> Scaffold (
                                    Modifier.fillMaxSize(),
                                    containerColor = BiblioTheme.colors.background,
                                ) { innerPadding ->
                                    ShelfPage(
                                        shelf = shelf,
                                        books = when(shelf) {
                                            LibraryShelf.CurrentlyReading -> books.currentlyReading
                                            LibraryShelf.DoneOrBackburner -> books.doneOrBackburner
                                            LibraryShelf.UpNext -> books.unread
                                        },
                                        modifier = Modifier.padding(innerPadding),
                                        onNavigateBack = { navController.popBackStack() },
                                        onOpenBook = { bookRepository.openBook(it) },
                                    )
                                }
                                else -> navController.popBackStack()
                            }
                        }
                        composable("settings") {
                            Scaffold(
                                Modifier.fillMaxSize(),
                                containerColor = BiblioTheme.colors.background,
                            ) { innerPadding ->
                                SettingsPage(
                                    modifier = Modifier.padding(innerPadding),
                                    onNavigateBack = { navController.popBackStack() },
                                    onOpenTestScreen = { navController.navigate("test") },
                                )
                            }
                        }
                        composable("test") {
                            Scaffold(
                                Modifier.fillMaxSize(),
                                containerColor = BiblioTheme.colors.background,
                            ) { innerPadding ->
                                TestPage(
                                    modifier = Modifier.padding(innerPadding),
                                    onNavigateBack = { navController.popBackStack() },
                                )
                            }
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

    override fun onDestroy() {
        super.onDestroy()
        server.stop()
    }
}