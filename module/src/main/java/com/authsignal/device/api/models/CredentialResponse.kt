package com.authsignal.device.api.models

import kotlinx.serialization.Serializable

@Serializable
data class CredentialResponse(
  val userAuthenticatorId: String,
  val userId: String,
  val verifiedAt: String,
  val lastVerifiedAt: String? = null,
)
