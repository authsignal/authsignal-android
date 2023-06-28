package com.authsignal.passkey.api.models

import kotlinx.serialization.Serializable

@Serializable
data class VerifyResponse(
  val isVerified: Boolean,
  val accessToken: String? = null,
)
