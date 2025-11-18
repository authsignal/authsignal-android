package com.authsignal.models

import kotlinx.serialization.Serializable

@Serializable
data class VerifyPinResponse(
  val isVerified: Boolean,
  val token: String? = null,
  val userId: String? = null,
)
