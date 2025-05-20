package com.authsignal.device.api.models

import kotlinx.serialization.Serializable

@Serializable
data class AddCredentialRequest(
  val publicKey: String,
  val deviceName: String,
  val devicePlatform: String,
)