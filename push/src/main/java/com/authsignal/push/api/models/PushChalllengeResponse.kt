package com.authsignal.push.api.models

import kotlinx.serialization.Serializable

@Serializable
data class PushChallengeResponse(
  val challengeId: String? = null,
)
