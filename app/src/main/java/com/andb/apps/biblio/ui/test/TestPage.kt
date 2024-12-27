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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.andb.apps.biblio.ui.common.BiblioBottomBar
import com.andb.apps.biblio.ui.common.BiblioScaffold
import com.andb.apps.biblio.ui.common.rotateWithBounds
import com.andb.apps.biblio.ui.theme.BiblioTheme

@Composable
fun TestPage(
    modifier: Modifier = Modifier,
    onNavigateBack: () -> Unit,
) {
    BiblioScaffold(
        modifier = modifier,
        bottomBar = {
            BiblioBottomBar(pageTitle = "Test", onNavigateBack = onNavigateBack)
        }
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

        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(Modifier.size(32.dp).background(Color.Red))
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
                Box(Modifier.size(32.dp).background(Color.Red))
            }
        }
    }
}