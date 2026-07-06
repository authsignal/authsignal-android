package com.authsignal.models.api

import kotlinx.serialization.Serializable

@Serializable
data class UpdateAppCredentialResponse(
  val userAuthenticatorId: String,
  val userId: String,
  val lastVerifiedAt: String,
  val pushToken: String? = null,
  val expiresAt: String? = null,
)
