package com.andb.apps.biblio

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.andb.apps.biblio.ui.apps.AppsPage
import com.andb.apps.biblio.ui.home.HomePage
import com.andb.apps.biblio.ui.theme.BiblioTheme

class MainActivity : ComponentActivity() {



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val navController = rememberNavController()
            BiblioTheme {
                NavHost(
                    navController = navController,
                    startDestination = "home",
                    enterTransition = { EnterTransition.None },
                    exitTransition = { ExitTransition.None },
                ) {
                    composable("home") {
                        Scaffold (Modifier.fillMaxSize()) { innerPadding ->
                            HomePage(
                                modifier = Modifier.padding(innerPadding),
                                onNavigateToApps = { navController.navigate("apps") }
                            )
                        }
                    }
                    composable("apps") {
                        Scaffold (Modifier.fillMaxSize()) { innerPadding ->
                            AppsPage(
                                modifier = Modifier.padding(innerPadding),
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }
                    }
                }
            }
        }
    }
}