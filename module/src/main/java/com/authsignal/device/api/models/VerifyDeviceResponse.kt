package com.authsignal.device.api.models

import kotlinx.serialization.Serializable

@Serializable
data class VerifyDeviceResponse(
  val token: String,
  val userId: String,
  val userAuthenticatorId: String,
  val username: String? = null,
)
