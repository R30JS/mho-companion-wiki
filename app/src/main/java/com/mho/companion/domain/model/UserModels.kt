package com.mho.companion.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class WishlistItem(
    val id: String,
    val label: String,
    val source: String = "",
    val target: Int = 1,
    val have: Int = 0
) {
    val done: Boolean get() = have >= target
    val progress: Float get() = if (target <= 0) 0f else (have.toFloat() / target).coerceIn(0f, 1f)
}

@Serializable
data class CustomLoadout(
    val id: String,
    val name: String,
    val element: Element = Element.NONE,
    val weaponType: WeaponType = WeaponType.LONG_SWORD,
    val weaponName: String = "",
    val notes: String = "",
    val createdAt: Long = 0L
)

@Serializable
data class BackupPayload(
    val app: String = "MHO Companion Wiki",
    val schema: Int = 1,
    val exportedAt: String = "",
    val checklist: List<String> = emptyList(),
    val wishlist: List<WishlistItem> = emptyList(),
    val customLoadouts: List<CustomLoadout> = emptyList()
)
