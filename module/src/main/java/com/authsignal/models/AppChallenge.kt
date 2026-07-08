package com.authsignal.models

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable
data class AppChallenge(
  val challengeId: String,
  val userId: String,
  val actionCode: String?,
  val idempotencyKey: String?,
  val deviceId: String?,
  val userAgent: String?,
  val ipAddress: String?,
  val custom: JsonObject? = null,
  val user: ChallengeUser? = null,
  val expiresAt: Long? = null,
)
