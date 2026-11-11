package com.authsignal

import io.ktor.client.network.sockets.ConnectTimeoutException
import io.ktor.http.HttpMethod
import java.io.IOException
import java.net.ConnectException
import java.net.NoRouteToHostException
import java.net.UnknownHostException
import kotlin.math.min
import kotlin.random.Random

internal object RetryPolicy {
  const val DEFAULT_RETRIES = 2
  const val REQUEST_TIMEOUT_MILLIS = 20_000

  private const val BASE_DELAY_MILLIS = 500L
  private const val MAX_DELAY_MILLIS = 5_000L

  enum class Outcome {
    NOT_SENT,
    REJECTED,
    UNKNOWN,
    FINAL,
  }

  fun outcome(statusCode: Int, retryAfter: String? = null): Outcome? {
    return when (statusCode) {
      in 200..299 -> null
      408 -> Outcome.REJECTED
      429 -> if (retryAfter == null) Outcome.FINAL else Outcome.REJECTED
      500, 502, 503, 504 -> Outcome.UNKNOWN
      else -> Outcome.FINAL
    }
  }

  fun outcome(cause: Throwable): Outcome {
    return when (cause) {
      // The Android engine also throws this for read timeouts, so the request may have been sent
      is ConnectTimeoutException -> Outcome.UNKNOWN
      is UnknownHostException, is ConnectException, is NoRouteToHostException -> Outcome.NOT_SENT
      is IOException -> Outcome.UNKNOWN
      else -> Outcome.FINAL
    }
  }

  private val retrySafeMethods = setOf(HttpMethod.Get, HttpMethod.Head, HttpMethod.Options)

  // POST endpoints which only issue a fresh challenge, options or nonce, so repeating them is harmless
  private val retrySafePaths = listOf(
    "/client/challenge",
    "/client/user-authenticators/passkey/registration-options",
    "/client/user-authenticators/passkey/authentication-options",
    "/client/user-authenticators/push/challenge/sign",
  )

  fun isRetrySafe(method: HttpMethod, path: String): Boolean {
    return method in retrySafeMethods || retrySafePaths.any { path.endsWith(it) }
  }

  fun isRetryable(outcome: Outcome, method: HttpMethod, path: String): Boolean {
    return when (outcome) {
      Outcome.NOT_SENT, Outcome.REJECTED -> true
      Outcome.UNKNOWN -> isRetrySafe(method, path)
      Outcome.FINAL -> false
    }
  }

  fun shouldRetry(method: HttpMethod, path: String, statusCode: Int, retryAfter: String?): Boolean {
    val outcome = outcome(statusCode, retryAfter) ?: return false

    return isRetryable(outcome, method, path) && delayMillis(1, retryAfter) != null
  }

  fun shouldRetry(method: HttpMethod, path: String, cause: Throwable): Boolean {
    return isRetryable(outcome(cause), method, path)
  }

  fun delayMillis(retry: Int, retryAfter: String?): Long? {
    val retryAfterSeconds = retryAfter?.trim()?.toDoubleOrNull()

    if (retryAfterSeconds != null) {
      val millis = (retryAfterSeconds * 1000).toLong()

      return if (millis <= MAX_DELAY_MILLIS) maxOf(millis, 0) else null
    }

    val backoff = min(BASE_DELAY_MILLIS shl (retry - 1).coerceIn(0, 4), MAX_DELAY_MILLIS)

    return backoff / 2 + Random.nextLong(backoff / 2 + 1)
  }
}
