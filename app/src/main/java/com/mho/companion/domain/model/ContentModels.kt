package com.mho.companion.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class ModuleVersions(
    val schema: Int = 1,
    val remoteBaseUrl: String = "",
    val codes_v: Int = 1,
    val builds_v: Int = 1,
    val characters_v: Int = 1,
    val monsters_v: Int = 1,
    val guides_v: Int = 1
)

@Serializable
data class CodesData(
    val version: Int = 1,
    val updatedAt: String = "",
    val claimSteps: List<String> = emptyList(),
    val codes: List<RedeemCode> = emptyList()
)

@Serializable
data class RedeemCode(
    val id: String,
    val code: String,
    val rewards: List<String> = emptyList(),
    val status: String = "ACTIVE",
    val expires: String? = null,
    val note: String = ""
)

@Serializable
data class BuildsData(
    val version: Int = 1,
    val updatedAt: String = "",
    val elements: List<ElementBuild> = emptyList()
)

@Serializable
data class ElementBuild(
    val element: Element,
    val summary: String = "",
    val visageSet: VisageSet? = null,
    val coreVisages: List<CoreVisage> = emptyList(),
    val buddies: List<Buddy> = emptyList(),
    val weapons: List<WeaponBuild> = emptyList()
)

@Serializable
data class VisageSet(
    val name: String = "",
    val pieces: List<String> = emptyList(),
    val bonus: String = ""
)

@Serializable
data class CoreVisage(
    val name: String = "",
    val effect: String = ""
)

@Serializable
data class Buddy(
    val name: String = "",
    val role: String = "",
    val skill: String = ""
)

@Serializable
data class WeaponBuild(
    val id: String,
    val weaponType: WeaponType,
    val name: String,
    val tier: String = "Apex",
    val forgePath: String = "",
    val attack: Int = 0,
    val elementValue: Int = 0,
    val materials: List<MaterialRequirement> = emptyList(),
    val armorSkills: List<String> = emptyList(),
    val insight: String = ""
)

@Serializable
data class MaterialRequirement(
    val name: String,
    val qty: Int = 1
)

@Serializable
data class CharactersData(
    val version: Int = 1,
    val updatedAt: String = "",
    val characters: List<Adventurer> = emptyList()
)

@Serializable
data class Adventurer(
    val id: String,
    val name: String,
    val role: Role,
    val element: Element,
    val weaponType: WeaponType,
    val weaponName: String = "",
    val rating: Int = 3,
    val summary: String = "",
    val rotation: List<String> = emptyList(),
    val tips: List<String> = emptyList()
)

@Serializable
data class MonstersData(
    val version: Int = 1,
    val updatedAt: String = "",
    val monsters: List<Monster> = emptyList()
)

@Serializable
data class Monster(
    val id: String,
    val name: String,
    val title: String = "",
    val threat: Int = 1,
    val weakness: ElementWeakness = ElementWeakness(),
    val habitat: String = "",
    val ailments: List<String> = emptyList(),
    val hitzones: List<Hitzone> = emptyList(),
    val strategy: List<String> = emptyList(),
    val materials: List<MonsterMaterial> = emptyList()
)

@Serializable
data class ElementWeakness(
    val primary: Element = Element.NONE,
    val secondary: Element = Element.NONE
)

@Serializable
data class Hitzone(
    val part: String,
    val sever: Int = 0,
    val blunt: Int = 0,
    val ranged: Int = 0,
    val note: String = ""
)

@Serializable
data class MonsterMaterial(
    val name: String,
    val rarity: Int = 1,
    val source: String = ""
)

@Serializable
data class GuidesData(
    val version: Int = 1,
    val updatedAt: String = "",
    val sections: List<GuideSection> = emptyList(),
    val ailments: List<Ailment> = emptyList(),
    val emergencyRecipes: List<Recipe> = emptyList()
)

@Serializable
data class GuideSection(
    val id: String,
    val title: String,
    val subtitle: String = "",
    val items: List<GuideItem> = emptyList()
)

@Serializable
data class GuideItem(
    val heading: String = "",
    val body: String = ""
)

@Serializable
data class Ailment(
    val id: String,
    val name: String,
    val severity: String = "",
    val effect: String = "",
    val cures: List<String> = emptyList(),
    val tip: String = ""
)

@Serializable
data class Recipe(
    val name: String,
    val ingredients: List<String> = emptyList(),
    val effect: String = ""
)

/** Everything the UI needs, resolved from bundled assets or the offline cache. */
data class Content(
    val codes: CodesData = CodesData(),
    val builds: BuildsData = BuildsData(),
    val characters: CharactersData = CharactersData(),
    val monsters: MonstersData = MonstersData(),
    val guides: GuidesData = GuidesData(),
    val versions: ModuleVersions = ModuleVersions(),
    val installedVersions: Map<String, Int> = emptyMap(),
    val loadedAt: Long = 0L
) {
    fun elementBuild(element: Element): ElementBuild? =
        builds.elements.firstOrNull { it.element == element }

    fun weapon(element: Element, id: String): WeaponBuild? =
        elementBuild(element)?.weapons?.firstOrNull { it.id == id }

    fun weaponFor(adventurer: Adventurer): WeaponBuild? =
        elementBuild(adventurer.element)?.weapons?.firstOrNull { it.weaponType == adventurer.weaponType }

    fun monster(id: String?): Monster? =
        monsters.monsters.firstOrNull { it.id == id }

    fun adventurer(id: String?): Adventurer? =
        characters.characters.firstOrNull { it.id == id }
}
