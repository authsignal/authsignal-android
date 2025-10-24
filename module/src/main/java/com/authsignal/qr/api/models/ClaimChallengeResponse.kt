package com.authsignal.qr.api.models

import kotlinx.serialization.Serializable

@Serializable
data class ClaimChallengeResponse(
  val success: Boolean,
  val userAgent: String? = null,
  val ipAddress: String? = null,
) 