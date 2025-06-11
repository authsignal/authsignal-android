package com.authsignal.device.api.models

import kotlinx.serialization.Serializable

@Serializable
data class VerifyDeviceRequest(
  val challengeId: String,
  val publicKey: String,
  val signature: String,
)