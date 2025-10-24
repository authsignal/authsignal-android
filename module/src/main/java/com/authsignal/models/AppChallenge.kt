package com.authsignal.models

import kotlinx.serialization.Serializable

@Serializable
data class AppChallenge(
  val challengeId: String,
  val userId: String,
  val actionCode: String?,
  val idempotencyKey: String?,
  val deviceId: String?,
  val userAgent: String?,
  val ipAddress: String?,
)