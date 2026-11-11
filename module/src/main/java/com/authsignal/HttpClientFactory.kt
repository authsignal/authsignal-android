package com.authsignal

import android.os.Build
import android.util.Log
import io.ktor.client.*
import io.ktor.client.engine.android.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.headers
import io.ktor.http.HttpHeaders
import io.ktor.http.encodedPath
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json

private const val TAG = "com.authsignal"

object HttpClientFactory {
  fun create(): HttpClient = create(null)

  fun create(tenantID: String?, retries: Int = RetryPolicy.DEFAULT_RETRIES): HttpClient {
    return HttpClient(Android) {
      engine {
        connectTimeout = RetryPolicy.REQUEST_TIMEOUT_MILLIS
        socketTimeout = RetryPolicy.REQUEST_TIMEOUT_MILLIS
      }
      if (retries > 0) {
        install(HttpRequestRetry) {
          retryIf(retries) { request, response ->
            RetryPolicy.shouldRetry(
              request.method,
              request.url.encodedPath,
              response.status.value,
              response.headers[HttpHeaders.RetryAfter],
            )
          }
          retryOnExceptionIf(retries) { request, cause ->
            RetryPolicy.shouldRetry(request.method, request.url.encodedPath, cause)
          }
          delayMillis(respectRetryAfterHeader = false) { retry ->
            RetryPolicy.delayMillis(retry, response?.headers?.get(HttpHeaders.RetryAfter)) ?: 0
          }
          modifyRequest { request ->
            val reason = response?.status?.value?.toString() ?: cause?.javaClass?.simpleName

            Log.i(TAG, "Retrying ${request.method.value} ${request.url.encodedPath} after $reason (retry $retryCount of $retries).")
          }
        }
      }
      install(ContentNegotiation) {
        json(Json {
          prettyPrint = true
          ignoreUnknownKeys = true
        })
      }
      install(UserAgent) {
        agent = "Mozilla/5.0 (Linux; Android ${Build.VERSION.RELEASE}; " +
          "${Build.MANUFACTURER} ${Build.MODEL}) " +
          AuthsignalRequestMetadata.userAgentProductTokens()
      }
      defaultRequest {
        headers {
          AuthsignalRequestMetadata.headers(tenantID).forEach { (name, value) ->
            append(name, value)
          }
        }
      }
    }
  }
}
