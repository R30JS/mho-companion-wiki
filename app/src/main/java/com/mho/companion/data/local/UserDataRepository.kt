package com.mho.companion.data.local

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.mho.companion.domain.model.BackupPayload
import com.mho.companion.domain.model.CustomLoadout
import com.mho.companion.domain.model.WishlistItem
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class UserDataRepository(private val context: Context) {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        prettyPrint = true
    }

    private object Keys {
        val checklist = stringPreferencesKey("checklist_items")
        val wishlist = stringPreferencesKey("farming_wishlist")
        val loadouts = stringPreferencesKey("custom_loadouts")
    }

    val checklist: Flow<Set<String>> = context.userDataStore.data.map { prefs ->
        decode<Set<String>>(prefs[Keys.checklist], emptySet())
    }

    val wishlist: Flow<List<WishlistItem>> = context.userDataStore.data.map { prefs ->
        decode<List<WishlistItem>>(prefs[Keys.wishlist], emptyList())
    }

    val customLoadouts: Flow<List<CustomLoadout>> = context.userDataStore.data.map { prefs ->
        decode<List<CustomLoadout>>(prefs[Keys.loadouts], emptyList())
    }

    suspend fun setChecklistItem(id: String, checked: Boolean) {
        context.userDataStore.edit { prefs ->
            val current = decode<Set<String>>(prefs[Keys.checklist], emptySet()).toMutableSet()
            if (checked) current += id else current -= id
            prefs[Keys.checklist] = encode(current)
        }
    }

    suspend fun addWishlist(label: String, source: String = "", target: Int = 1) {
        editWishlist { items ->
            if (items.any { it.label.equals(label, ignoreCase = true) && it.source == source }) {
                items
            } else {
                items + WishlistItem(
                    id = UUID.randomUUID().toString(),
                    label = label,
                    source = source,
                    target = target.coerceIn(1, 999),
                    have = 0
                )
            }
        }
    }

    suspend fun adjustWishlistHave(id: String, delta: Int) {
        editWishlist { items ->
            items.map { item ->
                if (item.id == id) {
                    item.copy(have = (item.have + delta).coerceIn(0, item.target))
                } else {
                    item
                }
            }
        }
    }

    suspend fun setWishlistTarget(id: String, target: Int) {
        editWishlist { items ->
            items.map { item ->
                if (item.id == id) {
                    val safeTarget = target.coerceIn(1, 999)
                    item.copy(target = safeTarget, have = item.have.coerceAtMost(safeTarget))
                } else {
                    item
                }
            }
        }
    }

    suspend fun removeWishlist(id: String) {
        editWishlist { items -> items.filterNot { it.id == id } }
    }

    suspend fun saveLoadout(loadout: CustomLoadout) {
        context.userDataStore.edit { prefs ->
            val current = decode<List<CustomLoadout>>(prefs[Keys.loadouts], emptyList())
            val next = if (current.any { it.id == loadout.id }) {
                current.map { if (it.id == loadout.id) loadout else it }
            } else {
                current + loadout.copy(createdAt = if (loadout.createdAt == 0L) System.currentTimeMillis() else loadout.createdAt)
            }
            prefs[Keys.loadouts] = encode(next)
        }
    }

    suspend fun deleteLoadout(id: String) {
        context.userDataStore.edit { prefs ->
            val current = decode<List<CustomLoadout>>(prefs[Keys.loadouts], emptyList())
            prefs[Keys.loadouts] = encode(current.filterNot { it.id == id })
        }
    }

    suspend fun snapshot(): BackupPayload {
        val prefs = context.userDataStore.data.first()
        return BackupPayload(
            exportedAt = ZonedDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME),
            checklist = decode<List<String>>(prefs[Keys.checklist], emptyList()),
            wishlist = decode<List<WishlistItem>>(prefs[Keys.wishlist], emptyList()),
            customLoadouts = decode<List<CustomLoadout>>(prefs[Keys.loadouts], emptyList())
        )
    }

    suspend fun restore(payload: BackupPayload) {
        context.userDataStore.edit { prefs ->
            prefs[Keys.checklist] = encode(payload.checklist)
            prefs[Keys.wishlist] = encode(payload.wishlist)
            prefs[Keys.loadouts] = encode(payload.customLoadouts)
        }
    }

    suspend fun clearAll() {
        context.userDataStore.edit { prefs ->
            prefs.remove(Keys.checklist)
            prefs.remove(Keys.wishlist)
            prefs.remove(Keys.loadouts)
        }
    }

    private suspend fun editWishlist(block: (List<WishlistItem>) -> List<WishlistItem>) {
        context.userDataStore.edit { prefs ->
            val current = decode<List<WishlistItem>>(prefs[Keys.wishlist], emptyList())
            prefs[Keys.wishlist] = encode(block(current))
        }
    }

    private inline fun <reified T> decode(raw: String?, fallback: T): T {
        if (raw.isNullOrBlank()) return fallback
        return runCatching { json.decodeFromString<T>(raw) }.getOrElse { fallback }
    }

    private inline fun <reified T> encode(value: T): String = json.encodeToString(value)
}
