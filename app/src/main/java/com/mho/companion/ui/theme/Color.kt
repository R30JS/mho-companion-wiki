package com.mho.companion.ui.theme

import androidx.compose.ui.graphics.Color
import com.mho.companion.domain.model.Element
import com.mho.companion.domain.model.Role

val OledBg = Color(0xFF0D1117)
val GlassSurface = Color(0xFF161B22)
val GlassSurfaceHigh = Color(0xFF1C2128)
val GlassBorder = Color(0xFF30363D)
val TextPrimary = Color(0xFFE6EDF3)
val TextSecondary = Color(0xFF8B949E)
val TextMuted = Color(0xFF6E7681)
val AccentCyan = Color(0xFF22D3EE)
val AccentGreen = Color(0xFF3FB950)
val AccentViolet = Color(0xFFBC8CFF)
val AccentOrange = Color(0xFFFF7B72)
val GoldStar = Color(0xFFFFC107)

val WeakRed = Color(0xFFFF4D4F)
val MidAmber = Color(0xFFFFA940)
val LowYellow = Color(0xFFE3B341)
val ImmuneGray = Color(0xFF484F58)

fun Element.accent(): Color = when (this) {
    Element.FIRE -> Color(0xFFFF5722)
    Element.WATER -> Color(0xFF03A9F4)
    Element.THUNDER -> Color(0xFFFFC107)
    Element.ICE -> Color(0xFF00E5FF)
    Element.DRAGON -> Color(0xFF9C27B0)
    Element.NONE -> Color(0xFF8B949E)
}

fun Role.accent(): Color = when (this) {
    Role.ASSAULT -> AccentOrange
    Role.DISRUPTOR -> AccentViolet
    Role.SUPPORT -> AccentGreen
}

fun ratingColor(rating: Int): Color = when {
    rating >= 3 -> WeakRed
    rating == 2 -> MidAmber
    rating == 1 -> LowYellow
    else -> ImmuneGray
}
