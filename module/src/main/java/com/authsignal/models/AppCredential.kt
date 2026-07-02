package com.authsignal.models

import java.util.Date
import kotlinx.serialization.Serializable

@Serializable
data class AppCredential(
  val credentialId: String,
  val createdAt: String,
  val userId: String,
  val lastAuthenticatedAt: String? = null,
  val expiresAt: Long? = null,
) {
  val isExpired: Boolean
    get() {
      val expiresAt = expiresAt ?: return false

      return Date(expiresAt * 1000).before(Date())
    }
}
