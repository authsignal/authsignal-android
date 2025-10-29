package com.authsignal.models.api

import kotlinx.serialization.Serializable

@Serializable
data class AppCredentialResponse(
  val userAuthenticatorId: String,
  val userId: String,
  val verifiedAt: String,
  val lastVerifiedAt: String? = null,
)