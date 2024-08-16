package com.authsignal.models

import kotlinx.serialization.Serializable

@Serializable
data class VerifyResponse(
  val isVerified: Boolean,
  val accessToken: String? = null,
  val failureReason: String? = null,
)
