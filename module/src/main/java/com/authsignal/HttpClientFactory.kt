package com.authsignal

import android.os.Build
import io.ktor.client.*
import io.ktor.client.engine.android.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json

object HttpClientFactory {
  fun create(): HttpClient {
    return HttpClient(Android) {
      install(ContentNegotiation) {
        json(Json {
          prettyPrint = true
          ignoreUnknownKeys = true
        })
      }
      install(UserAgent) {
        agent = "Mozilla/5.0 (Linux; Android ${Build.VERSION.RELEASE}; " +
          "${Build.MANUFACTURER} ${Build.MODEL}) " +
          "AuthsignalAndroidSDK/${BuildConfig.VERSION_NAME}"
      }
    }
  }
}
