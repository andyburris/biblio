package com.andb.apps.biblio.ui.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
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

class BiblioPagerState(
    private val items: List<BiblioPagerItem>,
    private val minRowHeight: Dp,
    private val containerSize: Pair<Dp, Dp>,
    initialPageIndex: Int,
) {
    private val currentPageIndexState = mutableIntStateOf(initialPageIndex)
    private val currentPages = derivedStateOf { items.paginate(containerSize.first, containerSize.second, minRowHeight) }

    val currentPageIndex = derivedStateOf { currentPageIndexState.intValue }
    val currentPage = derivedStateOf { currentPages.value.getOrNull(currentPageIndexState.intValue) }
    val totalPages = derivedStateOf { currentPages.value.size }
    val currentItemRange = derivedStateOf {
        val start = currentPages.value.take(currentPageIndexState.intValue).sumOf { it.rows.sumOf { it.items.size } }
        val end = start + (currentPages.value.getOrNull(currentPageIndexState.intValue)?.rows?.sumOf { it.items.size } ?: 0)
        start until end
    }
    val totalItems get() = derivedStateOf { currentPages.value.sumOf { it.rows.sumOf { it.items.size } } }
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
data class BiblioPagerItem(
    val width: BiblioPagerWidth,
    val content: @Composable (BiblioPagerState) -> Unit,
    val includeInCount: Boolean = true,
)
sealed class BiblioPagerWidth {
    abstract val minWidth: Dp
    data class Fixed(val width: Dp) : BiblioPagerWidth() {
        override val minWidth: Dp = width
    }
    data class Dynamic(val min: Dp) : BiblioPagerWidth() {
        override val minWidth: Dp = min
    }
    data class Fill(val overrideRowHeight: Dp? = null) : BiblioPagerWidth() {
        override val minWidth: Dp = (-1).dp
    }
}

private val BottomBarHeight = 64.dp
@Composable
fun  BiblioPager(
    items: List<BiblioPagerItem>,
    minRowHeight: Dp,
    modifier: Modifier = Modifier,
    initialPageIndex: Int = 0,
    header: @Composable (BiblioPagerState) -> Dp = { 0.dp },
    bottomBar: @Composable (BiblioPagerState) -> Unit,
    row: @Composable (row: BiblioPagerRow, modifier: Modifier, content: @Composable RowScope.() -> Unit) -> Unit = { items, modifier, content ->
        Row(
            modifier = modifier,
            horizontalArrangement = Arrangement.SpaceEvenly,
            content = content,
        )
    },
) {

    BoxWithConstraints(
        modifier = modifier,
    ) {
        val pagerSize = with(LocalDensity.current) { constraints.maxWidth.toDp() to (constraints.maxHeight.toDp() - BottomBarHeight) }
        val pagerState = remember(items, minRowHeight) { BiblioPagerState(items, minRowHeight, pagerSize, initialPageIndex) }

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
                header(pagerState)
                currentPage?.rows?.forEach { row ->
                    val rowHeight = (row.items.firstOrNull()?.width as? BiblioPagerWidth.Fill)
                        ?.overrideRowHeight
                        ?: minRowHeight
                    row(row,
                        Modifier
                            .then(when(currentPage.fillsHeight) {
                                true -> Modifier.weight(1f).heightIn(min = rowHeight)
                                false -> Modifier.height(rowHeight)
                            })
                            .fillMaxWidth(),
                    ) {
                        row.items.forEach {
                            Box(
                                modifier = when(it.width) {
                                    is BiblioPagerWidth.Fixed -> Modifier.width(it.width.width)
                                    is BiblioPagerWidth.Dynamic -> Modifier.weight(1f).widthIn(min = it.width.min)
                                    is BiblioPagerWidth.Fill -> Modifier.weight(1f)
                                }
                            ) {
                                it.content(pagerState)
                            }
                        }
                    }
                }
            }
        }
    }
}

data class BiblioPagerPage(
    val rows: List<BiblioPagerRow>,
    val fillsHeight: Boolean,
)
data class BiblioPagerRow(
    val items: List<BiblioPagerItem>,
    val fillsWidth: Boolean,
)
private fun  List<BiblioPagerItem>.paginate(containerWidth: Dp, containerHeight: Dp, minRowHeight: Dp): List<BiblioPagerPage> {
    val allRows = this.fold(listOf(emptyList<BiblioPagerItem>() to 0.dp)) { acc, biblioPagerItem ->
        val (currentRow, currentRowWidth) = acc.last()

        val isFill = biblioPagerItem.width is BiblioPagerWidth.Fill
        val mustOverflow = currentRowWidth > containerWidth
        val fitsOnCurrent = (currentRowWidth + biblioPagerItem.width.minWidth) <= containerWidth

        when {
            isFill -> acc + (listOf(biblioPagerItem) to containerWidth)
            mustOverflow -> acc + (listOf(biblioPagerItem) to biblioPagerItem.width.minWidth)
            fitsOnCurrent -> acc.dropLast(1) + (currentRow + biblioPagerItem to currentRowWidth + biblioPagerItem.width.minWidth)
            else -> acc + (listOf(biblioPagerItem) to biblioPagerItem.width.minWidth)
        }

    }
        .filter { it.first.isNotEmpty() }
        .let {
            val lastRowIndex = it.size - 1
            it.mapIndexed { index, (items, width) ->
                BiblioPagerRow(items, index != lastRowIndex)
            }
        }


    val pages = allRows.fold(listOf(emptyList<BiblioPagerRow>() to 0.dp)) { acc, row ->
        val (currentRows, currentHeight) = acc.last()

        val rowHeight = (row.items.firstOrNull()?.width as? BiblioPagerWidth.Fill)
            ?.overrideRowHeight
            ?: minRowHeight

        val mustOverflow = rowHeight > containerHeight
        val fitsOnCurrent = currentHeight + rowHeight <= containerHeight

        when {
            mustOverflow -> acc + (listOf(row) to rowHeight)
            fitsOnCurrent -> acc.dropLast(1) + ((currentRows.plusElement(row)) to (currentHeight + rowHeight))
            else -> acc + (listOf(row) to rowHeight)
        }
    }
        .filter { it.first.isNotEmpty() }
        .map { BiblioPagerPage(it.first, it.second >= (containerHeight - minRowHeight)) }
    return pages
}

@Composable
fun BiblioPageSwitcher(
    pagerState: BiblioPagerState,
    modifier: Modifier = Modifier,
) {
    when(pagerState.totalPages.value) {
        0, 1 -> {}
        else -> Row(
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
}