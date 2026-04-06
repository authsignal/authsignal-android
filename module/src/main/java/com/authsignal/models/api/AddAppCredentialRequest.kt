package com.authsignal.models.api

import kotlinx.serialization.Serializable

@Serializable
data class AddAppCredentialRequest(
  val publicKey: String,
  val deviceName: String,
  val devicePlatform: String,
  val performAttestation: AddAppCredentialDeviceIntegrity? = null,
)

@Serializable
data class AddAppCredentialDeviceIntegrity(
  val provider: String,
  val token: String,
  val keyId: String? = null,
)