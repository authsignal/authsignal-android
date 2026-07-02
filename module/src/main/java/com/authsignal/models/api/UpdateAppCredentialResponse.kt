package com.authsignal.models.api

import kotlinx.serialization.Serializable

@Serializable
data class UpdateAppCredentialResponse(
  val userAuthenticatorId: String,
  val userId: String,
  val lastVerifiedAt: String,
  // Echoes the request's pushToken; absent for keep-alive calls that omit the token.
  val pushToken: String? = null,
  val expiresAt: String? = null,
)
