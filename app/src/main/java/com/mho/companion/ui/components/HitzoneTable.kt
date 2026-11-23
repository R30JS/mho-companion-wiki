package com.mho.companion.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mho.companion.domain.model.Hitzone
import com.mho.companion.ui.theme.GlassBorder
import com.mho.companion.ui.theme.GlassSurfaceHigh
import com.mho.companion.ui.theme.TextPrimary
import com.mho.companion.ui.theme.TextSecondary
import com.mho.companion.ui.theme.ratingColor

@Composable
fun HitzoneTable(
    hitzones: List<Hitzone>,
    modifier: Modifier = Modifier,
    scale: Float = 1f,
    showNotes: Boolean = true
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, GlassBorder, RoundedCornerShape(12.dp))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(GlassSurfaceHigh)
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "PART",
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary,
                modifier = Modifier.weight(1.25f)
            )
            listOf("SEV", "BLUNT", "RNG").forEach {
                Text(
                    text = it,
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary,
                    modifier = Modifier.weight(0.9f)
                )
            }
        }
        hitzones.forEachIndexed { index, zone ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(if (index % 2 == 0) Color(0x33161B22) else Color.Transparent)
                    .padding(horizontal = 10.dp, vertical = if (scale > 1f) 10.dp else 7.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1.25f)) {
                    Text(
                        text = zone.part,
                        style = if (scale > 1f) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyMedium,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (showNotes && zone.note.isNotBlank() && scale >= 1f) {
                        Text(
                            text = zone.note,
                            style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.1f),
                            color = TextSecondary,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                HitzoneCell(zone.sever, Modifier.weight(0.9f), scale)
                HitzoneCell(zone.blunt, Modifier.weight(0.9f), scale)
                HitzoneCell(zone.ranged, Modifier.weight(0.9f), scale)
            }
            if (index != hitzones.lastIndex) {
                HorizontalDivider(color = GlassBorder.copy(alpha = 0.35f))
            }
        }
    }
}

@Composable
private fun HitzoneCell(rating: Int, modifier: Modifier, scale: Float) {
    val color = ratingColor(rating)
    Box(
        modifier = modifier
            .padding(horizontal = 3.dp)
            .height((if (scale > 1f) 38 else 26).dp)
            .clip(RoundedCornerShape(6.dp))
            .background(color.copy(alpha = if (rating == 0) 0.14f else 0.24f))
            .border(1.dp, color.copy(alpha = 0.55f), RoundedCornerShape(6.dp)),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            repeat(3) { dot ->
                Box(
                    modifier = Modifier
                        .size((if (scale > 1f) 5 else 4).dp)
                        .clip(CircleShape)
                        .background(if (dot < rating) color else color.copy(alpha = 0.16f))
                )
            }
            Text(
                text = "$rating",
                style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.sp),
                color = color,
                modifier = Modifier.padding(start = 1.dp)
            )
        }
    }
}
