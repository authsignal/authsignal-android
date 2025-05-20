package com.authsignal.device.api.models

import kotlinx.serialization.Serializable

@Serializable
data class DeviceChallengeResponse(
  val challengeId: String? = null,
  val userId: String? = null,
  val actionCode: String? = null,
  val idempotencyKey: String? = null,
  val userAgent: String? = null,
  val deviceId: String? = null,
  val ipAddress: String? = null,
)
