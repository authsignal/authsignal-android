package com.authsignal.models.api

import kotlinx.serialization.Serializable

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
)
