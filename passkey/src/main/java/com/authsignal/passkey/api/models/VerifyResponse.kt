package com.authsignal.passkey.api.models

import kotlinx.serialization.Serializable

@Serializable
data class VerifyResponse(
  val isVerified: Boolean,
  val accessToken: String? = null,
  val userId: String? = null,
  val userAuthenticatorId: String? = null,
  val username: String? = null,
  val userDisplayName: String? = null,
)
