package com.mho.companion.domain.model

import kotlinx.serialization.Serializable

@Serializable
enum class Element(val label: String) {
    FIRE("Fire"),
    WATER("Water"),
    THUNDER("Thunder"),
    ICE("Ice"),
    DRAGON("Dragon"),
    NONE("Raw")
}

@Serializable
enum class Role(val label: String) {
    ASSAULT("Assault"),
    DISRUPTOR("Disruptor"),
    SUPPORT("Support")
}

@Serializable
enum class AttackType(val label: String) {
    SEVER("Sever"),
    BLUNT("Blunt"),
    RANGED("Ranged")
}

@Serializable
enum class WeaponType(
    val label: String,
    val code: String,
    val attackType: AttackType
) {
    HEAVY_BOWGUN("Heavy Bowgun", "hbg", AttackType.RANGED),
    LONG_SWORD("Long Sword", "ls", AttackType.SEVER),
    DUAL_BLADES("Dual Blades", "db", AttackType.SEVER),
    BOW("Bow", "bow", AttackType.RANGED),
    GREAT_SWORD("Great Sword", "gs", AttackType.SEVER),
    CHARGE_BLADE("Charge Blade", "cb", AttackType.SEVER),
    HAMMER("Hammer", "hammer", AttackType.BLUNT),
    LANCE("Lance", "lance", AttackType.SEVER)
}
