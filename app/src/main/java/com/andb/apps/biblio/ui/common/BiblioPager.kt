package com.andb.apps.biblio.ui.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.adamglin.PhosphorIcons
import com.adamglin.phosphoricons.Regular
import com.adamglin.phosphoricons.regular.Caretleft
import com.adamglin.phosphoricons.regular.Caretright
import com.andb.apps.biblio.ui.theme.BiblioTheme

class BiblioPagerState<T>(
    private val items: List<BiblioPagerItem<T>>,
    private val minRowHeight: Dp,
    private val containerSize: Pair<Dp, Dp>,
    initialPageIndex: Int,
) {
    private val currentPageIndexState = mutableIntStateOf(initialPageIndex)
    private val currentPages = derivedStateOf { items.paginate(containerSize.first, containerSize.second, minRowHeight) }

    val currentPageIndex = derivedStateOf { currentPageIndexState.intValue }
    val currentPage = derivedStateOf { currentPages.value[currentPageIndexState.intValue] }
    val totalPages = derivedStateOf { currentPages.value.size }
    val totalItems get() = derivedStateOf { currentPages.value.sumOf { it.rows.sumOf { it.size } } }
    val canGoBack = derivedStateOf { currentPageIndexState.intValue > 0 }
    val canGoForward = derivedStateOf { currentPageIndexState.intValue < totalPages.value - 1 }

    fun scrollToPage(pageIndex: Int) {
        currentPageIndexState.intValue = pageIndex
    }
    fun nextPage() {
        if (canGoForward.value) {
            currentPageIndexState.intValue++
        }
    }
    fun previousPage() {
        if (canGoBack.value) {
            currentPageIndexState.intValue--
        }
    }
}
data class BiblioPagerItem<T>(
    val value: T,
    val width: BiblioPagerWidth,
    val content: @Composable () -> Unit,
)
sealed class BiblioPagerWidth {
    abstract val minWidth: Dp
    data class Fixed(val width: Dp) : BiblioPagerWidth() {
        override val minWidth: Dp = width
    }
    data class Fill(val min: Dp) : BiblioPagerWidth() {
        override val minWidth: Dp = min
    }
}

private val BottomBarHeight = 64.dp
@Composable
fun <T> BiblioPager(
    items: List<BiblioPagerItem<T>>,
    minRowHeight: Dp,
    modifier: Modifier = Modifier,
    initialPageIndex: Int = 0,
    bottomBar: @Composable (BiblioPagerState<T>) -> Unit
) {

    BoxWithConstraints(
        modifier = modifier,
    ) {
        val pagerSize = with(LocalDensity.current) { constraints.maxWidth.toDp() to (constraints.maxHeight.toDp() - BottomBarHeight) }
        val pagerState = remember(items, minRowHeight) { BiblioPagerState<T>(items, minRowHeight, pagerSize, initialPageIndex) }

        BiblioScaffold(
            modifier = Modifier.fillMaxSize(),
            bottomBar = {
                bottomBar.invoke(pagerState)
            }
        ) {
            val currentPage = pagerState.currentPage.value

            Column(
                modifier = Modifier.fillMaxSize(),
            ) {
                currentPage.rows.forEach {
                    Row(
                        modifier = Modifier
                            .then(when(currentPage.fillsHeight) {
                                true -> Modifier.weight(1f).heightIn(min = minRowHeight)
                                false -> Modifier.height(minRowHeight)
                            })
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        it.forEach {
                            Box(
                                modifier = when(it.width) {
                                    is BiblioPagerWidth.Fixed -> Modifier.width(it.width.width)
                                    is BiblioPagerWidth.Fill -> Modifier.weight(1f).widthIn(min = it.width.min)
                                }
                            ) {
                                it.content()
                            }
                        }
                    }
                }
            }
        }
    }
}

data class BiblioPagerPage<T>(
    val rows: List<List<BiblioPagerItem<T>>>,
    val fillsHeight: Boolean,
)
private fun <T> List<BiblioPagerItem<T>>.paginate(containerWidth: Dp, containerHeight: Dp, minRowHeight: Dp): List<BiblioPagerPage<T>> {
    val allRows = this.fold(listOf(emptyList<BiblioPagerItem<T>>() to 0.dp)) { acc, biblioPagerItem ->
        val (currentRow, currentRowWidth) = acc.last()
        if (currentRowWidth > containerWidth) {
            return@fold acc + (listOf(biblioPagerItem) to biblioPagerItem.width.minWidth)
        }
        when {
            (currentRowWidth + biblioPagerItem.width.minWidth) <= containerWidth ->
                acc.dropLast(1) + (currentRow + biblioPagerItem to currentRowWidth + biblioPagerItem.width.minWidth)

            else -> acc + (listOf(biblioPagerItem) to biblioPagerItem.width.minWidth)
        }
    }.map { it.first }
    val rowsPerPage: Int = (containerHeight / minRowHeight).toInt().coerceAtLeast(1)
    return allRows.chunked(rowsPerPage).map { BiblioPagerPage(it, it.size == rowsPerPage) }
}

@Composable
fun BiblioPageSwitcher(
    pagerState: BiblioPagerState<*>,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy((-8).dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BiblioButton(
            onClick = { pagerState.previousPage() },
            icon = PhosphorIcons.Regular.Caretleft,
            enabled = pagerState.canGoBack.value,
        )
        ExactText(
            text = "Page ${pagerState.currentPageIndex.value + 1}/${pagerState.totalPages.value}",
            color = BiblioTheme.colors.onBackgroundSecondary,
        )
        BiblioButton(
            onClick = { pagerState.nextPage() },
            icon = PhosphorIcons.Regular.Caretright,
            enabled = pagerState.canGoForward.value,
        )
    }
}