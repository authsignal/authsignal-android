package com.authsignal.passkey.api.models

import kotlinx.serialization.Serializable

@Serializable
data class ChallengeResponse(
  val challengeId: String,
)
