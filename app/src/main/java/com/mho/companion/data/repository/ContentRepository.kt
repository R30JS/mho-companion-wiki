package com.mho.companion.data.repository

import android.content.Context
import android.util.Log
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import com.mho.companion.data.local.userDataStore
import com.mho.companion.domain.model.BuildsData
import com.mho.companion.domain.model.CharactersData
import com.mho.companion.domain.model.CodesData
import com.mho.companion.domain.model.Content
import com.mho.companion.domain.model.GuidesData
import com.mho.companion.domain.model.ModuleVersions
import com.mho.companion.domain.model.MonstersData
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json

sealed interface SyncState {
    data object Idle : SyncState
    data object Syncing : SyncState
    data class Done(
        val message: String,
        val updated: List<String> = emptyList(),
        val at: Long = 0L
    ) : SyncState

    data class Error(val message: String) : SyncState
}

data class SyncResult(val success: Boolean, val message: String)

/**
 * Offline-first content source:
 * 1. bundled assets are always the baseline,
 * 2. a synced module in filesDir wins when its version is greater,
 * 3. sync downloads only modules whose remote version is newer.
 */
class ContentRepository(
    private val context: Context,
    private val http: HttpClient
) {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
    }

    private val contentDir = File(context.filesDir, "content").apply { mkdirs() }

    private val _content = MutableStateFlow<Content?>(null)
    val content: StateFlow<Content?> = _content.asStateFlow()

    private val _syncState = MutableStateFlow<SyncState>(SyncState.Idle)
    val syncState: StateFlow<SyncState> = _syncState.asStateFlow()

    suspend fun load() = withContext(Dispatchers.IO) {
        runCatching { readContent() }
            .onSuccess { _content.value = it }
            .onFailure { error ->
                Log.e(TAG, "Failed to load bundled content", error)
                if (_content.value == null) _content.value = Content()
            }
    }

    suspend fun ensureLoaded(): Content? {
        if (_content.value == null) load()
        return _content.value
    }

    suspend fun sync(): SyncResult = withContext(Dispatchers.IO) {
        val base = runCatching {
            _content.value?.versions ?: readAsset("version.json", ModuleVersions.serializer())
        }.getOrNull()?.remoteBaseUrl?.trim()?.trimEnd('/').orEmpty()

        if (base.isEmpty()) {
            val message = "Remote sync is not configured. Bundled offline data stays active."
            _syncState.value = SyncState.Done(message, emptyList(), System.currentTimeMillis())
            return@withContext SyncResult(false, message)
        }

        _syncState.value = SyncState.Syncing
        runCatching {
            val remote = json.decodeFromString(
                ModuleVersions.serializer(),
                http.get("$base/version.json").bodyAsText()
            )
            val asset = readAsset("version.json", ModuleVersions.serializer())
            val prefs = context.userDataStore.data.first()
            val updated = mutableListOf<String>()

            downloadIfNewer(base, "codes", remote.codes_v, effective("codes", asset.codes_v, prefs), updated)
            downloadIfNewer(base, "builds", remote.builds_v, effective("builds", asset.builds_v, prefs), updated)
            downloadIfNewer(base, "characters", remote.characters_v, effective("characters", asset.characters_v, prefs), updated)
            downloadIfNewer(base, "monsters", remote.monsters_v, effective("monsters", asset.monsters_v, prefs), updated)
            downloadIfNewer(base, "guides", remote.guides_v, effective("guides", asset.guides_v, prefs), updated)

            if (updated.isNotEmpty()) {
                _content.value = readContent()
            }

            val message = if (updated.isEmpty()) {
                "All modules are already up to date."
            } else {
                "Updated ${updated.joinToString(", ")}."
            }
            _syncState.value = SyncState.Done(message, updated, System.currentTimeMillis())
            SyncResult(true, message)
        }.getOrElse { error ->
            Log.w(TAG, "Content sync failed", error)
            val message = error.message?.takeIf { it.isNotBlank() } ?: "Sync failed. Check your connection."
            _syncState.value = SyncState.Error(message)
            SyncResult(false, message)
        }
    }

    private suspend fun readContent(): Content {
        val versions = readAsset("version.json", ModuleVersions.serializer())
        val prefs = context.userDataStore.data.first()

        val codes = loadModule("codes", versions.codes_v, CodesData.serializer(), prefs)
        val builds = loadModule("builds", versions.builds_v, BuildsData.serializer(), prefs)
        val characters = loadModule("characters", versions.characters_v, CharactersData.serializer(), prefs)
        val monsters = loadModule("monsters", versions.monsters_v, MonstersData.serializer(), prefs)
        val guides = loadModule("guides", versions.guides_v, GuidesData.serializer(), prefs)

        val installed = mapOf(
            "codes" to effective("codes", versions.codes_v, prefs),
            "builds" to effective("builds", versions.builds_v, prefs),
            "characters" to effective("characters", versions.characters_v, prefs),
            "monsters" to effective("monsters", versions.monsters_v, prefs),
            "guides" to effective("guides", versions.guides_v, prefs)
        )

        return Content(
            codes = codes,
            builds = builds,
            characters = characters,
            monsters = monsters,
            guides = guides,
            versions = versions,
            installedVersions = installed,
            loadedAt = System.currentTimeMillis()
        )
    }

    private fun <T> loadModule(
        name: String,
        assetVersion: Int,
        serializer: KSerializer<T>,
        prefs: androidx.datastore.preferences.core.Preferences
    ): T {
        val cachedFile = File(contentDir, "$name.json")
        val cachedVersion = prefs[intPreferencesKey("mod_${name}_v")] ?: 0
        if (cachedVersion >= assetVersion && cachedFile.exists()) {
            val cachedText = runCatching { cachedFile.readText() }.getOrNull()
            if (cachedText != null) {
                val parsed = runCatching { json.decodeFromString(serializer, cachedText) }.getOrNull()
                if (parsed != null) return parsed
            }
        }
        return readAsset("$name.json", serializer)
    }

    private fun <T> readAsset(file: String, serializer: KSerializer<T>): T {
        val text = context.assets.open(file).bufferedReader().use { it.readText() }
        return json.decodeFromString(serializer, text)
    }

    private fun effective(
        name: String,
        assetVersion: Int,
        prefs: androidx.datastore.preferences.core.Preferences
    ): Int = maxOf(assetVersion, prefs[intPreferencesKey("mod_${name}_v")] ?: 0)

    private suspend fun downloadIfNewer(
        base: String,
        name: String,
        remoteVersion: Int,
        localVersion: Int,
        updated: MutableList<String>
    ) {
        if (remoteVersion <= localVersion) return
        val text = http.get("$base/$name.json").bodyAsText()
        json.parseToJsonElement(text) // validate before replacing the cached module
        val target = File(contentDir, "$name.json")
        val temp = File(contentDir, "$name.json.tmp")
        temp.writeText(text)
        if (!temp.renameTo(target)) {
            target.writeText(text)
            temp.delete()
        }
        context.userDataStore.edit { prefs ->
            prefs[intPreferencesKey("mod_${name}_v")] = remoteVersion
        }
        updated += name
    }

    private companion object {
        const val TAG = "ContentRepository"
    }
}
