package com.mho.companion

import android.content.Context
import com.mho.companion.data.local.BackupManager
import com.mho.companion.data.local.UserDataRepository
import com.mho.companion.data.repository.ContentRepository
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout

class AppContainer(context: Context) {

    private val appContext = context.applicationContext

    val userDataRepository = UserDataRepository(appContext)

    private val httpClient = HttpClient(OkHttp) {
        expectSuccess = true
        install(HttpTimeout) {
            connectTimeoutMillis = 10_000
            requestTimeoutMillis = 15_000
            socketTimeoutMillis = 15_000
        }
    }

    val contentRepository = ContentRepository(appContext, httpClient)
    val backupManager = BackupManager(appContext, userDataRepository)
}
