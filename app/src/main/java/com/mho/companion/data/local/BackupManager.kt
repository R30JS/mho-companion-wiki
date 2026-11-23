package com.mho.companion.data.local

import android.content.Context
import android.net.Uri
import com.mho.companion.domain.model.BackupPayload
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class BackupManager(
    private val context: Context,
    private val userData: UserDataRepository
) {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        prettyPrint = true
    }

    suspend fun export(uri: Uri): Result<BackupPayload> = withContext(Dispatchers.IO) {
        runCatching {
            val payload = userData.snapshot()
            val text = json.encodeToString(payload)
            val stream = context.contentResolver.openOutputStream(uri, "wt")
                ?: error("Cannot open the selected destination.")
            stream.use { it.write(text.toByteArray(Charsets.UTF_8)) }
            payload
        }
    }

    suspend fun import(uri: Uri): Result<BackupPayload> = withContext(Dispatchers.IO) {
        runCatching {
            val text = context.contentResolver.openInputStream(uri)
                ?.bufferedReader()
                ?.use { it.readText() }
                ?: error("Cannot read the selected file.")
            val payload = json.decodeFromString<BackupPayload>(text)
            require(payload.app.startsWith("MHO", ignoreCase = true)) {
                "This is not an MHO Companion backup file."
            }
            require(payload.schema <= 1) {
                "This backup comes from a newer app version."
            }
            userData.restore(payload)
            payload
        }
    }
}
