package com.authsignal.qr.api.models

import com.authsignal.models.ChallengeUser
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable
data class ClaimChallengeResponse(
  val success: Boolean,
  val userAgent: String? = null,
  val ipAddress: String? = null,
  val actionCode: String? = null,
  val idempotencyKey: String? = null,
  val custom: JsonObject? = null,
  val user: ChallengeUser? = null,
)
