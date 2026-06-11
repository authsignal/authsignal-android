package com.authsignal.models

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable
data class ChallengeUser(
  val custom: JsonObject? = null,
)
