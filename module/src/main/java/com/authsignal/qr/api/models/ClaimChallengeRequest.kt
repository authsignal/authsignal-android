package com.authsignal.qr.api.models

import kotlinx.serialization.Serializable

@Serializable
data class ClaimChallengeRequest(
  val publicKey: String,
  val challengeId: String,
  val signature: String,
)