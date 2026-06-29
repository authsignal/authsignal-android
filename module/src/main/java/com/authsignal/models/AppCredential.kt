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
    // The server emits `expiresAt` via Luxon `DateTime.toISO()`, which yields
    // fractional seconds plus a numeric timezone offset that includes a colon,
    // e.g. "2026-06-30T12:42:38.416+12:00". minSdk is 23 with no core-library
    // desugaring, so `java.time` is unavailable; `SimpleDateFormat`'s `Z` token
    // does not accept a colon in the offset, so normalise before parsing:
    //   - "...Z"      -> "...+0000"
    //   - "...+12:00" -> "...+1200"
    val normalised = value
      .replace("Z", "+0000")
      .replace(Regex("([+-]\\d{2}):(\\d{2})$"), "$1$2")

    val patterns = listOf(
      "yyyy-MM-dd'T'HH:mm:ss.SSSZ",
      "yyyy-MM-dd'T'HH:mm:ssZ",
    )

    for (pattern in patterns) {
      try {
        val formatter = SimpleDateFormat(pattern, Locale.US).apply {
          // Reject malformed input rather than coercing it, so an unparseable
          // value falls through to `null` (fail-open => not expired).
          isLenient = false
        }

        return formatter.parse(normalised)
      } catch (_: Exception) {
        // Try the next pattern.
      }
    }

    return null
  }
}
