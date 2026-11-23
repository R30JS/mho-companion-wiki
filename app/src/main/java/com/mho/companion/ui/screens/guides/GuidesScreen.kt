package com.mho.companion.ui.screens.guides

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mho.companion.domain.model.Ailment
import com.mho.companion.domain.model.GuideSection
import com.mho.companion.domain.model.Recipe
import com.mho.companion.ui.components.EmptyState
import com.mho.companion.ui.components.ExpandIndicator
import com.mho.companion.ui.components.FilterRow
import com.mho.companion.ui.components.GlassCard
import com.mho.companion.ui.components.HunterChip
import com.mho.companion.ui.components.LoadingPane
import com.mho.companion.ui.components.Pill
import com.mho.companion.ui.components.ScreenHeader
import com.mho.companion.ui.components.SectionHeader
import com.mho.companion.ui.components.ThumbZone
import com.mho.companion.ui.theme.AccentCyan
import com.mho.companion.ui.theme.AccentGreen
import com.mho.companion.ui.theme.GlassBorder
import com.mho.companion.ui.theme.GlassSurfaceHigh
import com.mho.companion.ui.theme.TextMuted
import com.mho.companion.ui.theme.TextPrimary
import com.mho.companion.ui.theme.TextSecondary
import com.mho.companion.ui.theme.WeakRed
import com.mho.companion.ui.theme.MidAmber

private enum class GuideCategory(val label: String) {
    ALL("ALL"),
    MISSIONS("MISSIONS"),
    STATUS("STATUS"),
    CRAFTING("CRAFTING")
}

@Composable
fun GuidesScreen(
    viewModel: GuidesViewModel,
    onOpenSettings: () -> Unit
) {
    val content by viewModel.content.collectAsStateWithLifecycle()
    var query by rememberSaveable { mutableStateOf("") }
    var category by rememberSaveable { mutableStateOf(GuideCategory.ALL.name) }
    var expandedId by rememberSaveable { mutableStateOf<String?>(null) }

    val selectedCategory = runCatching { GuideCategory.valueOf(category) }
        .getOrDefault(GuideCategory.ALL)

    Column(modifier = Modifier.fillMaxSize()) {
        ScreenHeader(
            kicker = "Field Guides",
            title = "Cheat Sheet",
            actions = {
                IconButton(onClick = onOpenSettings) {
                    Icon(
                        imageVector = Icons.Filled.Settings,
                        contentDescription = "Settings",
                        tint = TextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        )

        val data = content
        if (data == null) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                LoadingPane()
            }
        } else {
            val needle = query.trim()
            val sections = remember(data, needle) {
                data.guides.sections.filter { section ->
                    needle.isBlank() || listOf(section.title, section.subtitle).any {
                        it.contains(needle, ignoreCase = true)
                    } || section.items.any {
                        it.heading.contains(needle, ignoreCase = true) ||
                            it.body.contains(needle, ignoreCase = true)
                    }
                }
            }
            val ailments = remember(data, needle) {
                data.guides.ailments.filter { ailment ->
                    needle.isBlank() || listOf(
                        ailment.name,
                        ailment.effect,
                        ailment.tip,
                        ailment.cures.joinToString(" ")
                    ).any { it.contains(needle, ignoreCase = true) }
                }
            }
            val recipes = remember(data, needle) {
                data.guides.emergencyRecipes.filter { recipe ->
                    needle.isBlank() || listOf(
                        recipe.name,
                        recipe.effect,
                        recipe.ingredients.joinToString(" ")
                    ).any { it.contains(needle, ignoreCase = true) }
                }
            }

            val showSections = selectedCategory == GuideCategory.ALL || selectedCategory == GuideCategory.MISSIONS
            val showAilments = selectedCategory == GuideCategory.ALL || selectedCategory == GuideCategory.STATUS
            val showRecipes = selectedCategory == GuideCategory.ALL || selectedCategory == GuideCategory.CRAFTING

            val nothingVisible =
                (!showSections || sections.isEmpty()) &&
                    (!showAilments || ailments.isEmpty()) &&
                    (!showRecipes || recipes.isEmpty())

            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (showSections) {
                    sections.forEach { section ->
                        item(key = "section-${section.id}") {
                            GuideAccordion(
                                section = section,
                                expanded = expandedId == section.id,
                                onToggle = {
                                    expandedId = if (expandedId == section.id) null else section.id
                                }
                            )
                        }
                    }
                }

                if (showAilments && ailments.isNotEmpty()) {
                    item(key = "ailments") {
                        AilmentSheet(ailments)
                    }
                }

                if (showRecipes && recipes.isNotEmpty()) {
                    item(key = "recipes") {
                        RecipeSheet(recipes)
                    }
                }

                if (nothingVisible) {
                    item { EmptyState(title = "Nothing here", body = "Try another search or category.") }
                }
            }

            ThumbZone(
                query = query,
                onQueryChange = { query = it },
                placeholder = "Search guide, status, recipe…",
                filterRows = {
                    FilterRow {
                        GuideCategory.entries.forEach { entry ->
                            HunterChip(
                                label = entry.label,
                                selected = selectedCategory == entry,
                                onClick = { category = entry.name }
                            )
                        }
                    }
                }
            )
        }
    }
}

@Composable
private fun GuideAccordion(
    section: GuideSection,
    expanded: Boolean,
    onToggle: () -> Unit
) {
    GlassCard(accent = if (expanded) AccentCyan else null) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onToggle)
                .padding(vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = section.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary
                )
                Text(
                    text = section.subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
            ExpandIndicator(expanded = expanded, tint = if (expanded) AccentCyan else TextMuted)
        }
        AnimatedVisibility(visible = expanded) {
            Column(modifier = Modifier.padding(top = 8.dp)) {
                section.items.forEach { item ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        Text(
                            text = item.heading,
                            style = MaterialTheme.typography.titleSmall,
                            color = AccentCyan
                        )
                        Text(
                            text = item.body,
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AilmentSheet(ailments: List<Ailment>) {
    GlassCard(accent = AccentGreen) {
        SectionHeader(title = "Status ailment cheat-sheet") {
            Pill(text = "${ailments.size} CONDITIONS", color = AccentGreen)
        }
        Spacer(Modifier.height(6.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(GlassSurfaceHigh)
                .padding(horizontal = 8.dp, vertical = 6.dp)
        ) {
            Text(
                text = "STATUS",
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary,
                modifier = Modifier.width(92.dp)
            )
            Text(
                text = "EFFECT & CURE",
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary
            )
        }
        ailments.forEachIndexed { index, ailment ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 7.dp)
            ) {
                Column(modifier = Modifier.width(92.dp)) {
                    Text(
                        text = ailment.name,
                        style = MaterialTheme.typography.titleSmall,
                        color = TextPrimary
                    )
                    val severityColor = when (ailment.severity.lowercase()) {
                        "high" -> WeakRed
                        "medium" -> MidAmber
                        else -> AccentGreen
                    }
                    Text(
                        text = ailment.severity.uppercase(),
                        style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.4.sp),
                        color = severityColor
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = ailment.effect,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                    Text(
                        text = "CURE · ${ailment.cures.joinToString(" / ")}",
                        style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.2.sp),
                        color = AccentGreen
                    )
                    if (ailment.tip.isNotBlank()) {
                        Text(
                            text = ailment.tip,
                            style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.1f),
                            color = TextMuted
                        )
                    }
                }
            }
            if (index != ailments.lastIndex) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(GlassBorder.copy(alpha = 0.35f))
                )
            }
        }
    }
}

@Composable
private fun RecipeSheet(recipes: List<Recipe>) {
    GlassCard(accent = AccentCyan) {
        SectionHeader(title = "Emergency field recipes")
        Spacer(Modifier.height(5.dp))
        recipes.forEachIndexed { index, recipe ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 5.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = recipe.name,
                        style = MaterialTheme.typography.titleSmall,
                        color = TextPrimary,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = recipe.ingredients.joinToString("  +  "),
                        style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.1f),
                        color = AccentCyan
                    )
                }
                Text(
                    text = recipe.effect,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
            if (index != recipes.lastIndex) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(GlassBorder.copy(alpha = 0.35f))
                )
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(
            text = "Craft from the radial menu. Keep Honey and Antidote Herbs in the item pouch.",
            style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.1f),
            color = TextMuted
        )
    }
}
