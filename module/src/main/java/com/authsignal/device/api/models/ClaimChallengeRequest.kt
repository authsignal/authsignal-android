package com.authsignal.device.api.models

import kotlinx.serialization.Serializable

@Serializable
data class ClaimChallengeRequest(
  val publicKey: String,
  val challengeId: String,
  val signature: String,
)