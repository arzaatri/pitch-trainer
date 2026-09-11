package com.example.tonetrainer

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private val WheelItemHeight = 44.dp
private const val WHEEL_VISIBLE_ROWS = 5 // default must be odd; middle row is the selection

/**
 * A snapping "wheel" list: drag/fling to scroll, the centered row is the current selection.
 * Centering is achieved with symmetric content padding rather than a version-specific
 * snap-position API, so it works against the project's current Compose Foundation version.
 */
@Composable
fun WheelPicker(
    items: List<String>,
    selectedIndex: Int,
    onSelectedIndexChange: (Int) -> Unit,
    accentColor: Color,
    modifier: Modifier = Modifier,
    scrollEnabled: Boolean = true,
    visibleRows: Int = WHEEL_VISIBLE_ROWS, // must be odd; middle row is the selection
) {
    val density = LocalDensity.current
    val itemHeightPx = with(density) { WheelItemHeight.toPx() }
    val sidePadding = WheelItemHeight * (visibleRows / 2)
    val scope = rememberCoroutineScope()

    val listState = rememberLazyListState(initialFirstVisibleItemIndex = selectedIndex.coerceIn(items.indices))

    // Re-center when selectedIndex changes from outside (e.g. locked to a fixed value), not
    // just from the user's own scroll gesture.
    LaunchedEffect(selectedIndex, items) {
        val target = selectedIndex.coerceIn(items.indices)
        if (!listState.isScrollInProgress && listState.firstVisibleItemScrollOffset == 0 &&
            listState.firstVisibleItemIndex != target
        ) {
            listState.animateScrollToItem(target, 0)
        }
    }

    val liveCenterIndex by remember(items) {
        derivedStateOf {
            val raw = listState.firstVisibleItemIndex +
                (listState.firstVisibleItemScrollOffset / itemHeightPx).roundToInt()
            raw.coerceIn(items.indices)
        }
    }

    LaunchedEffect(listState, items) {
        snapshotFlow { listState.isScrollInProgress }
            .distinctUntilChanged()
            .collect { scrolling ->
                if (!scrolling) {
                    val settled = liveCenterIndex
                    onSelectedIndexChange(settled)
                    listState.animateScrollToItem(settled, 0)
                }
            }
    }

    Box(
        modifier = modifier
            .height(WheelItemHeight * visibleRows)
            .alpha(if (scrollEnabled) 1f else 0.5f),
        contentAlignment = Alignment.Center,
    ) {
        LazyColumn(
            state = listState,
            contentPadding = PaddingValues(vertical = sidePadding),
            modifier = Modifier.fillMaxHeight().fillMaxWidth(),
            userScrollEnabled = scrollEnabled,
        ) {
            itemsIndexed(items) { index, label ->
                val isCentered = index == liveCenterIndex
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(WheelItemHeight)
                        .then(
                            if (scrollEnabled) {
                                Modifier.clickable {
                                    scope.launch { listState.animateScrollToItem(index, 0) }
                                    onSelectedIndexChange(index)
                                }
                            } else {
                                Modifier
                            },
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = label,
                        fontSize = if (isCentered) 22.sp else 16.sp,
                        fontWeight = if (isCentered) FontWeight.Bold else FontWeight.Normal,
                        color = if (isCentered) accentColor else Color.Gray,
                    )
                }
            }
        }
    }
}
