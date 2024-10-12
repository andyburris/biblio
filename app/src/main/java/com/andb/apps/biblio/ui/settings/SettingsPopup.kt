package com.andb.apps.biblio.ui.settings

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.adamglin.PhosphorIcons
import com.adamglin.phosphoricons.Regular
import com.adamglin.phosphoricons.regular.Arrowright
import com.adamglin.phosphoricons.regular.Bookopentext
import com.adamglin.phosphoricons.regular.Gauge
import com.adamglin.phosphoricons.regular.Lightning
import com.adamglin.phosphoricons.regular.Moon
import com.adamglin.phosphoricons.regular.Percent
import com.adamglin.phosphoricons.regular.Pushpin
import com.adamglin.phosphoricons.regular.Sun
import com.adamglin.phosphoricons.regular.Sundim
import com.adamglin.phosphoricons.regular.Sunhorizon
import com.adamglin.phosphoricons.regular.Textaa
import com.andb.apps.biblio.data.LocalSettings
import com.andb.apps.biblio.ui.common.BiblioButton
import com.andb.apps.biblio.ui.common.ExactText
import com.andb.apps.biblio.ui.common.border
import com.andb.apps.biblio.ui.home.WifiState
import com.andb.apps.biblio.ui.home.currentBatteryAsState
import com.andb.apps.biblio.ui.home.toIcon
import com.andb.apps.biblio.ui.home.toPercentString
import com.andb.apps.biblio.ui.home.wifiSignalAsState
import com.andb.apps.biblio.ui.theme.BiblioTheme
import kotlinx.coroutines.launch

@Composable
fun SettingsPopup(
    modifier: Modifier = Modifier,
    content: LazyGridScope.() -> Unit,
) {

    LazyVerticalGrid(
        columns = GridCells.Adaptive(196.dp),
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, color = BiblioTheme.colors.divider, shape = RoundedCornerShape(8.dp))
            .background(BiblioTheme.colors.background, shape = RoundedCornerShape(8.dp))
            .clip(RoundedCornerShape(8.dp)),
    ) {
        content()
    }
}

@Composable
fun SettingsPopupHeader(
    title: String,
    modifier: Modifier = Modifier,
    onMore: () -> Unit,
) {
    Row(
        modifier = modifier
            .border(BiblioTheme.colors.divider, bottom = 1.dp)
            .padding(start = 16.dp, end = 8.dp, top = 4.dp, bottom = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ExactText(
            text = title,
            color = BiblioTheme.colors.onBackgroundSecondary,
            modifier = Modifier.weight(1f),
        )
        BiblioButton(
            onClick = onMore,
            rightIcon = PhosphorIcons.Regular.Arrowright,
            text = "More",
        )
    }
}