package com.mho.companion.domain.usecase

import com.mho.companion.domain.model.ArmorPiece
import com.mho.companion.domain.model.ArmorSkill
import com.mho.companion.domain.model.Element
import com.mho.companion.domain.model.MaterialRequirement
import com.mho.companion.domain.model.WeaponBuild

data class MixSkillTotal(
    val name: String,
    val level: Int,
    val maxLevel: Int
) {
    val isMaxed: Boolean get() = level >= maxLevel
}

data class MixSetTotals(
    val skills: List<MixSkillTotal> = emptyList(),
    val maxRarity: Int = 0,
    val materials: List<MaterialRequirement> = emptyList(),
    val resistance: Map<Element, Int> = emptyMap(),
    val armorCount: Int = 0,
    val hasWeapon: Boolean = false
) {
    val totalMaterialCount: Int get() = materials.sumOf { it.qty }
    val equippedCount: Int get() = armorCount + if (hasWeapon) 1 else 0
}

object MixSetCalculator {

    private val weaponSkillPattern = Regex("""^(.+?)\s+(\d+)$""")

    private val skillAliases = mapOf(
        "Fire Attack" to "Fire Damage Up",
        "Water Attack" to "Water Damage Up",
        "Thunder Attack" to "Thunder Damage Up",
        "Ice Attack" to "Ice Damage Up",
        "Dragon Attack" to "Dragon Damage Up",
        "Charge Up" to "Charge Enhancement",
        "Spirit Gauge" to "Spirit Bind Blade"
    )

    private val maxLevels = mapOf(
        "Fire Damage Up" to 7,
        "Water Damage Up" to 7,
        "Thunder Damage Up" to 7,
        "Ice Damage Up" to 7,
        "Dragon Damage Up" to 7,
        "Charge Enhancement" to 5,
        "Spirit Bind Blade" to 5,
        "Offensive Defense" to 5,
        "Capacity Boost" to 3,
        "Combo Enhancement" to 5,
        "Morph Boost" to 5,
        "Flash Draw" to 5,
        "Weakness Exploit" to 5,
        "Critical Boost" to 3,
        "Critical Element" to 3,
        "Constitution" to 5,
        "Artillery" to 5,
        "Divine Blessing" to 3,
        "Evade Window" to 5,
        "Focus" to 3,
        "Guard" to 5,
        "Peak Performance" to 3,
        "Quick Sheath" to 3,
        "Slugger" to 3,
        "Stamina Surge" to 3,
        "Bow Charge Plus" to 3
    )

    fun weaponSkills(weapon: WeaponBuild?): List<ArmorSkill> =
        weapon?.armorSkills.orEmpty().map(::parseWeaponSkill)

    fun totals(weapon: WeaponBuild?, armors: Collection<ArmorPiece>): MixSetTotals {
        val merged = linkedMapOf<String, Pair<Int, Int>>()

        fun add(skill: ArmorSkill) {
            if (skill.skillName.isBlank() || skill.level <= 0) return
            val previous = merged[skill.skillName]
            val level = (previous?.first ?: 0) + skill.level
            val maxLevel = maxOf(previous?.second ?: 0, skill.maxLevel.coerceAtLeast(1))
            merged[skill.skillName] = level to maxLevel
        }

        armors.forEach { armor -> armor.skills.forEach(::add) }
        weaponSkills(weapon).forEach(::add)

        val skills = merged.map { (name, values) ->
            MixSkillTotal(
                name = name,
                level = values.first.coerceAtMost(values.second),
                maxLevel = values.second
            )
        }.sortedWith(
            compareByDescending<MixSkillTotal> { it.isMaxed }
                .thenByDescending { it.level }
                .thenBy { it.name }
        )

        return MixSetTotals(
            skills = skills,
            maxRarity = armors.maxOfOrNull { it.rarity } ?: 0,
            materials = materials(weapon, armors),
            resistance = armors.groupingBy { it.elementRes }.eachCount(),
            armorCount = armors.size,
            hasWeapon = weapon != null
        )
    }

    fun materials(weapon: WeaponBuild?, armors: Collection<ArmorPiece>): List<MaterialRequirement> {
        val totals = linkedMapOf<String, Int>()
        (armors.flatMap { it.materials } + weapon?.materials.orEmpty()).forEach { material ->
            if (material.name.isBlank() || material.qty <= 0) return@forEach
            totals[material.name] = (totals[material.name] ?: 0) + material.qty
        }
        return totals.map { (name, qty) -> MaterialRequirement(name = name, qty = qty) }
            .sortedWith(compareByDescending<MaterialRequirement> { it.qty }.thenBy { it.name })
    }

    fun parseWeaponSkill(raw: String): ArmorSkill {
        val trimmed = raw.trim()
        val match = weaponSkillPattern.matchEntire(trimmed)
        val rawName = match?.groupValues?.get(1)?.trim().orEmpty().ifBlank { trimmed }
        val level = match?.groupValues?.get(2)?.toIntOrNull() ?: 1
        val name = skillAliases[rawName] ?: rawName
        val maxLevel = maxOf(level, maxLevels[name] ?: 5)
        return ArmorSkill(skillName = name, level = level, maxLevel = maxLevel)
    }
}
