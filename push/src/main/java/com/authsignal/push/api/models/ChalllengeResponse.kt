package com.authsignal.push.api.models

import kotlinx.serialization.Serializable

@Serializable
data class ChallengeResponse(
  val challengeId: String? = null,
)
