package com.authsignal.inapp.api.models

import kotlinx.serialization.Serializable

@Serializable
data class InAppVerifyRequest(
  val challengeId: String,
  val publicKey: String,
  val signature: String,
)