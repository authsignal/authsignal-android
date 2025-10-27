package com.authsignal.models.api

import kotlinx.serialization.Serializable

@Serializable
data class AddAppCredentialRequest(
  val publicKey: String,
  val deviceName: String,
  val devicePlatform: String,
)