package com.authsignal.models

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.serialization.Serializable

@Serializable
data class AppCredential(
  val credentialId: String,
  val createdAt: String,
  val userId: String,
  val lastAuthenticatedAt: String? = null,
  val expiresAt: String? = null,
) {
  /**
   * Whether the credential's lease has lapsed, based on [expiresAt].
   *
   * Fail-open: returns `false` when the server provides no expiry (no expiry
   * configured) or when the value cannot be parsed, mirroring the server's
   * fail-open behaviour.
   */
  val isExpired: Boolean
    get() {
      val expiry = expiresAt?.let { parseIso8601(it) } ?: return false

      return expiry.before(Date())
    }

  private fun parseIso8601(value: String): Date? {
    // Normalise to a form SimpleDateFormat can parse on API 23 (no java.time):
    // "...Z" and "...+00:00" -> "...+0000".
    val normalised = value
      .replace("Z", "+0000")
      .replace(Regex("([+-]\\d{2}):(\\d{2})$"), "$1$2")

    val patterns = listOf(
      "yyyy-MM-dd'T'HH:mm:ss.SSSZ",
      "yyyy-MM-dd'T'HH:mm:ssZ",
    )

    for (pattern in patterns) {
      try {
        return SimpleDateFormat(pattern, Locale.US).parse(normalised)
      } catch (_: Exception) {
        // Try the next pattern.
      }
    }

    return null
  }
}
