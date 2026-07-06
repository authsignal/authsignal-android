package com.authsignal.models.api

import kotlinx.serialization.Serializable

@Serializable
data class UpdateAppCredentialRequest(
  val challengeId: String,
  val publicKey: String,
  val signature: String,
  val pushToken: String? = null,
  val resetExpiry: Boolean = false,
)
