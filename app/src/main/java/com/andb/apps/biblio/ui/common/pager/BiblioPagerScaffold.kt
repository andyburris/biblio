package com.andb.apps.biblio.ui.common.pager

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.unit.Constraints

interface BiblioPagerScaffoldScope {
    val constraints: Constraints
    val placeables: List<Placeable>
}

@Composable
fun <T> BiblioPagerScaffoldSubcompose(
    modifier: Modifier = Modifier,
    topBar: (@Composable T.() -> Unit)? = null,
    content: @Composable T.() -> Unit,
    bottomBar: (@Composable T.() -> Unit)? = null,
    transform: @Composable BiblioPagerScaffoldScope.() -> T,
) {
    SubcomposeLayout(modifier = modifier) { constraints ->
        val looseConstraints = constraints.copy(minWidth = 0, minHeight = 0)

        // First subcomposition: Measure all components
        val placeables = subcompose("measure"){
            val looseScope = object : BiblioPagerScaffoldScope {
                override val constraints: Constraints = looseConstraints
                override val placeables: List<Placeable> = emptyList()
            }
            val transformedLooseScope = looseScope.transform()
            topBar?.invoke(transformedLooseScope)
            content(transformedLooseScope)
            bottomBar?.invoke(transformedLooseScope)
        }.map { it.measure(looseConstraints) }

        // Calculate heights
        val topBarHeight = if (topBar != null) placeables.firstOrNull()?.height ?: 0 else 0
        val bottomBarHeight = if (bottomBar != null) placeables.lastOrNull()?.height ?: 0 else 0
        val contentHeight = (constraints.maxHeight - topBarHeight - bottomBarHeight).coerceAtLeast(0)

        // Create content constraints
        val contentConstraints = constraints.copy(
            minHeight = 0,
            maxHeight = contentHeight
        )
        val finalConstraints = listOf(
            topBar?.let { topBarHeight },
            contentHeight,
            bottomBar?.let { bottomBarHeight }
        )
            .filterNotNull()
            .map { constraints.copy(minHeight = it, maxHeight = it) }

        // Second subcomposition: Final layout with proper constraints
        val finalPlaceables = subcompose("layout") {
            val finalScope = object : BiblioPagerScaffoldScope {
                override val constraints: Constraints = contentConstraints
                override val placeables: List<Placeable> = placeables
            }
            val transformedFinalScope = finalScope.transform()
            topBar?.invoke(transformedFinalScope)
            content(transformedFinalScope)
            bottomBar?.invoke(transformedFinalScope)
        }
            .zip(finalConstraints)
            .map { (measurable, constraints) -> measurable.measure(constraints) }

        // Layout
        layout(constraints.maxWidth, constraints.maxHeight) {
            var yPosition = 0
            finalPlaceables.forEachIndexed { index, placeable ->
                placeable.place(0, yPosition)
                yPosition += placeable.height
            }
        }
    }
}