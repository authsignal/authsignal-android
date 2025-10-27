package com.authsignal.inapp.api.models

import kotlinx.serialization.Serializable

@Serializable
data class InAppVerifyResponse(
  val token: String,
  val userId: String,
  val userAuthenticatorId: String,
  val username: String? = null,
)
