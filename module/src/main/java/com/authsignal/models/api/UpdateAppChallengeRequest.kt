package com.authsignal.models.api

import kotlinx.serialization.Serializable

@Serializable
data class UpdateAppChallengeRequest(
  val publicKey: String,
  val challengeId: String,
  val signature: String,
  val approved: Boolean,
  val verificationCode: String?,
)