package com.mho.companion.domain.usecase

import com.mho.companion.domain.model.Adventurer
import com.mho.companion.domain.model.Element
import com.mho.companion.domain.model.Monster
import com.mho.companion.domain.model.Role

data class TeamPick(
    val role: Role,
    val adventurer: Adventurer,
    val score: Double,
    val reason: String
)

data class TeamRecommendation(
    val monster: Monster,
    val picks: List<TeamPick>,
    val primaryElement: Element,
    val onElementCount: Int
)

/**
 * Deterministic 1 Assault + 1 Disruptor + 1 Support recommendation.
 * Element match dominates the score, character rating breaks ties.
 */
object TeamSynergy {

    fun recommend(monster: Monster, characters: List<Adventurer>): TeamRecommendation {
        val primary = monster.weakness.primary
        val secondary = monster.weakness.secondary

        fun score(adventurer: Adventurer): Double {
            val elementScore = when {
                primary == Element.NONE && secondary == Element.NONE -> 1.0
                adventurer.element == primary -> 10.0
                adventurer.element == secondary -> 6.0
                else -> 0.0
            }
            return elementScore + adventurer.rating * 0.8
        }

        val picks = Role.entries.mapNotNull { role ->
            val best = characters
                .asSequence()
                .filter { it.role == role }
                .sortedWith(compareByDescending<Adventurer> { score(it) }.thenBy { it.name })
                .firstOrNull()
            best?.let {
                TeamPick(
                    role = role,
                    adventurer = it,
                    score = score(it),
                    reason = reasonFor(it, primary, secondary)
                )
            }
        }

        return TeamRecommendation(
            monster = monster,
            picks = picks,
            primaryElement = primary,
            onElementCount = picks.count { it.adventurer.element == primary }
        )
    }

    private fun reasonFor(adventurer: Adventurer, primary: Element, secondary: Element): String =
        when {
            adventurer.element == primary -> "${primary.label} matches the primary weakness"
            adventurer.element == secondary -> "${secondary.label} hits the secondary weakness"
            else -> "Off-element ${adventurer.role.label} utility"
        }
}
