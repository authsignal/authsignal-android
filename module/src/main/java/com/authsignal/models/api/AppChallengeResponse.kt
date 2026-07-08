package com.authsignal.models.api

import com.authsignal.models.ChallengeUser
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable
data class AppChallengeResponse(
  val userAuthenticatorId: String,
  val challengeId: String? = null,
  val userId: String? = null,
  val actionCode: String? = null,
  val idempotencyKey: String? = null,
  val userAgent: String? = null,
  val deviceId: String? = null,
  val ipAddress: String? = null,
  val custom: JsonObject? = null,
  val user: ChallengeUser? = null,
  val expiresAt: Long? = null,
)
