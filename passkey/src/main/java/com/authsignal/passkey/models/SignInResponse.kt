package com.authsignal.passkey.models

import kotlinx.serialization.Serializable

@Serializable
data class SignInResponse(
  val isVerified: Boolean,
  val token: String? = null,
  val userId: String? = null,
  val userAuthenticatorId: String? = null,
  val userName: String? = null,
  val userDisplayName: String? = null,
)
