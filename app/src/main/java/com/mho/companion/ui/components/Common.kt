package com.mho.companion.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mho.companion.domain.model.AttackType
import com.mho.companion.domain.model.Element
import com.mho.companion.domain.model.Role
import com.mho.companion.ui.theme.AccentCyan
import com.mho.companion.ui.theme.GlassBorder
import com.mho.companion.ui.theme.GlassSurface
import com.mho.companion.ui.theme.GlassSurfaceHigh
import com.mho.companion.ui.theme.GoldStar
import com.mho.companion.ui.theme.TextPrimary
import com.mho.companion.ui.theme.TextSecondary
import com.mho.companion.ui.theme.accent
import com.mho.companion.ui.theme.ratingColor

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    accent: Color? = null,
    padding: Dp = 14.dp,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = RoundedCornerShape(18.dp)
    val borderColor = accent?.copy(alpha = 0.55f) ?: GlassBorder
    val visuals = Modifier
        .then(
            if (accent != null) {
                Modifier.shadow(
                    elevation = 10.dp,
                    shape = shape,
                    ambientColor = accent.copy(alpha = 0.35f),
                    spotColor = accent.copy(alpha = 0.35f)
                )
            } else {
                Modifier
            }
        )
        .clip(shape)
        .background(Brush.linearGradient(listOf(Color(0xF0161B22), Color(0xF00D1117))))
        .border(
            width = 1.dp,
            brush = Brush.verticalGradient(
                listOf(borderColor, borderColor.copy(alpha = 0.18f))
            ),
            shape = shape
        )
        .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)

    Column(
        modifier = modifier.then(visuals).padding(padding),
        content = content
    )
}

@Composable
fun ScreenHeader(
    kicker: String,
    title: String,
    onBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (onBack != null) {
            IconButton(onClick = onBack, modifier = Modifier.size(36.dp)) {
                Icon(
                    imageVector = Icons.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = TextPrimary
                )
            }
            Spacer(Modifier.width(4.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = kicker.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = AccentCyan
            )
            Text(
                text = title,
                style = MaterialTheme.typography.headlineMedium,
                color = TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        actions()
    }
}

@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    action: (@Composable () -> Unit)? = null
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = TextSecondary
        )
        Spacer(Modifier.weight(1f))
        action?.invoke()
    }
}

@Composable
fun ElementBadge(
    element: Element,
    modifier: Modifier = Modifier,
    filled: Boolean = false
) {
    val color = element.accent()
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(if (filled) color.copy(alpha = 0.18f) else Color.Transparent)
            .border(1.dp, color.copy(alpha = 0.7f), RoundedCornerShape(50))
            .padding(horizontal = 8.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(Modifier.width(5.dp))
        Text(
            text = element.label,
            style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.6.sp),
            color = color
        )
    }
}

@Composable
fun RoleBadge(role: Role, modifier: Modifier = Modifier) {
    val color = role.accent()
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(color.copy(alpha = 0.14f))
            .border(1.dp, color.copy(alpha = 0.55f), RoundedCornerShape(50))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(
            text = role.label.uppercase(),
            style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.8.sp),
            color = color
        )
    }
}

@Composable
fun Pill(
    text: String,
    color: Color = AccentCyan,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(color.copy(alpha = 0.12f))
            .border(1.dp, color.copy(alpha = 0.45f), RoundedCornerShape(50))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.6.sp),
            color = color
        )
    }
}

@Composable
fun HunterChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    accent: Color = AccentCyan
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(if (selected) accent.copy(alpha = 0.18f) else Color(0x66161B22))
            .border(
                1.dp,
                if (selected) accent.copy(alpha = 0.8f) else GlassBorder,
                RoundedCornerShape(50)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = if (selected) accent else TextSecondary,
            maxLines = 1
        )
    }
}

@Composable
fun StarRating(rating: Int, modifier: Modifier = Modifier, max: Int = 5, fontSize: Int = 12) {
    Text(
        text = "★".repeat(rating.coerceIn(0, max)) + "☆".repeat((max - rating).coerceAtLeast(0)),
        modifier = modifier,
        color = GoldStar,
        fontSize = fontSize.sp,
        letterSpacing = 1.sp
    )
}

@Composable
fun ThreatBadge(threat: Int) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(text = "★", color = GoldStar, fontSize = 12.sp)
        Spacer(Modifier.width(3.dp))
        Text(
            text = "$threat/10",
            style = MaterialTheme.typography.labelSmall,
            color = TextSecondary
        )
    }
}

@Composable
fun AttackIcon(
    type: AttackType,
    color: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.size(10.dp)) {
        val u = size.minDimension
        when (type) {
            AttackType.SEVER -> {
                drawLine(
                    color = color,
                    start = Offset(u * 0.18f, u * 0.82f),
                    end = Offset(u * 0.82f, u * 0.18f),
                    strokeWidth = u * 0.16f,
                    cap = StrokeCap.Round
                )
                drawLine(
                    color = color,
                    start = Offset(u * 0.52f, u * 0.48f),
                    end = Offset(u * 0.82f, u * 0.18f),
                    strokeWidth = u * 0.28f,
                    cap = StrokeCap.Round
                )
            }

            AttackType.BLUNT -> drawCircle(color = color, radius = u * 0.34f)

            AttackType.RANGED -> {
                drawLine(
                    color = color,
                    start = Offset(u * 0.12f, u * 0.5f),
                    end = Offset(u * 0.82f, u * 0.5f),
                    strokeWidth = u * 0.12f,
                    cap = StrokeCap.Round
                )
                drawLine(
                    color = color,
                    start = Offset(u * 0.55f, u * 0.25f),
                    end = Offset(u * 0.85f, u * 0.5f),
                    strokeWidth = u * 0.12f,
                    cap = StrokeCap.Round
                )
                drawLine(
                    color = color,
                    start = Offset(u * 0.55f, u * 0.75f),
                    end = Offset(u * 0.85f, u * 0.5f),
                    strokeWidth = u * 0.12f,
                    cap = StrokeCap.Round
                )
            }
        }
    }
}

@Composable
fun AttackTypeBadge(type: AttackType, modifier: Modifier = Modifier) {
    val color = when (type) {
        AttackType.SEVER -> Color(0xFFFF7B72)
        AttackType.BLUNT -> Color(0xFFE3B341)
        AttackType.RANGED -> Color(0xFF56D4DD)
    }
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(color.copy(alpha = 0.12f))
            .border(1.dp, color.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AttackIcon(type = type, color = color)
        Spacer(Modifier.width(4.dp))
        Text(
            text = type.label.uppercase(),
            style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.5.sp),
            color = color
        )
    }
}

@Composable
fun Monogram(
    text: String,
    accent: Color,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp
) {
    val shape = RoundedCornerShape(size * 0.3f)
    Box(
        modifier = modifier
            .size(size)
            .clip(shape)
            .background(
                Brush.linearGradient(
                    listOf(accent.copy(alpha = 0.32f), Color(0xFF0D1117))
                )
            )
            .border(1.dp, accent.copy(alpha = 0.55f), shape),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text.take(1).uppercase(),
            color = accent,
            fontWeight = FontWeight.Bold,
            fontSize = (size.value * 0.4f).sp
        )
    }
}

@Composable
fun HunterButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    accent: Color = AccentCyan,
    enabled: Boolean = true,
    leading: (@Composable () -> Unit)? = null
) {
    Button(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = accent.copy(alpha = 0.14f),
            contentColor = accent,
            disabledContainerColor = GlassSurfaceHigh,
            disabledContentColor = TextSecondary
        ),
        border = BorderStroke(
            1.dp,
            if (enabled) accent.copy(alpha = 0.55f) else GlassBorder.copy(alpha = 0.4f)
        ),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
    ) {
        leading?.invoke()
        if (leading != null) Spacer(Modifier.width(6.dp))
        Text(text = text, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
fun CopyGlyph(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(15.dp)) {
        val u = size.minDimension
        val stroke = u * 0.09f
        drawRoundRect(
            color = color,
            topLeft = Offset(u * 0.28f, u * 0.30f),
            size = Size(u * 0.48f, u * 0.48f),
            cornerRadius = CornerRadius(u * 0.08f),
            style = Stroke(stroke)
        )
        drawRoundRect(
            color = color,
            topLeft = Offset(u * 0.18f, u * 0.18f),
            size = Size(u * 0.46f, u * 0.46f),
            cornerRadius = CornerRadius(u * 0.08f),
            style = Stroke(stroke)
        )
    }
}

@Composable
fun ExpandIndicator(expanded: Boolean, tint: Color = TextSecondary) {
    Icon(
        imageVector = if (expanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
        contentDescription = if (expanded) "Collapse" else "Expand",
        tint = tint,
        modifier = Modifier.size(18.dp)
    )
}

@Composable
fun BulletLine(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = TextSecondary,
    accent: Color = AccentCyan,
    index: Int? = null
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .padding(top = 6.dp)
                .size(5.dp)
                .clip(CircleShape)
                .background(accent)
        )
        Spacer(Modifier.width(9.dp))
        Text(
            text = if (index != null) "$index. $text" else text,
            style = MaterialTheme.typography.bodyMedium,
            color = color
        )
    }
}

@Composable
fun StatBlock(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    accent: Color = AccentCyan
) {
    Column(modifier = modifier) {
        Text(
            text = label.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = TextSecondary
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            color = accent
        )
    }
}

@Composable
fun LoadingPane(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator(color = AccentCyan, strokeWidth = 2.dp)
        Spacer(Modifier.height(12.dp))
        Text(
            text = "Loading hunter data…",
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary
        )
    }
}

@Composable
fun EmptyState(title: String, body: String, modifier: Modifier = Modifier) {
    GlassCard(modifier = modifier.fillMaxWidth()) {
        Text(text = title, style = MaterialTheme.typography.titleMedium, color = TextPrimary)
        Spacer(Modifier.height(4.dp))
        Text(text = body, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
    }
}

@Composable
fun HitzoneLegend(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        listOf(3 to "Weakest", 2 to "Good", 1 to "Low", 0 to "Immune").forEach { (rating, label) ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(ratingColor(rating))
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.2.sp),
                    color = TextSecondary
                )
            }
        }
    }
}

@Composable
fun KeyValueRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    valueColor: Color = TextPrimary
) {
    Row(modifier = modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary,
            modifier = Modifier.width(92.dp)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            color = valueColor,
            modifier = Modifier.weight(1f)
        )
    }
}
