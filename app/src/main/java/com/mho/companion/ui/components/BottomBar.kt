package com.mho.companion.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mho.companion.ui.theme.AccentCyan
import com.mho.companion.ui.theme.GlassBorder
import com.mho.companion.ui.theme.TextSecondary

data class MhoTab(
    val route: String,
    val label: String,
    val icon: ImageVector
)

val MhoTabs = listOf(
    MhoTab("hub", "Hub", Icons.Filled.Home),
    MhoTab("builds", "Builds", Icons.Filled.Build),
    MhoTab("bestiary", "Bestiary", Icons.Filled.Favorite),
    MhoTab("guides", "Guides", Icons.Filled.List)
)

@Composable
fun MhoBottomBar(
    currentRoute: String?,
    onSelect: (String) -> Unit
) {
    Surface(
        color = Color(0xF20D1117),
        border = BorderStroke(1.dp, GlassBorder.copy(alpha = 0.45f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .height(64.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            MhoTabs.forEach { tab ->
                val selected = currentRoute != null &&
                    (currentRoute == tab.route || currentRoute.startsWith(tab.route))
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable { onSelect(tab.route) },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center
                ) {
                    Icon(
                        imageVector = tab.icon,
                        contentDescription = tab.label,
                        tint = if (selected) AccentCyan else TextSecondary,
                        modifier = Modifier.size(21.dp)
                    )
                    Spacer(Modifier.height(3.dp))
                    Text(
                        text = tab.label,
                        style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.4.sp),
                        color = if (selected) AccentCyan else TextSecondary
                    )
                    Spacer(Modifier.height(3.dp))
                    Box(
                        modifier = Modifier
                            .width(if (selected) 14.dp else 0.dp)
                            .height(2.dp)
                            .clip(RoundedCornerShape(50))
                            .background(if (selected) AccentCyan else Color.Transparent)
                    )
                }
            }
        }
    }
}
