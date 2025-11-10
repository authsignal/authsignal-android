package com.authsignal.models.api

import kotlinx.serialization.Serializable

@Serializable
data class ChallengeRequest(
  val action: String,
)
