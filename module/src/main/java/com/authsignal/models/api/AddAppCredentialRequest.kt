package com.authsignal.models.api

import kotlinx.serialization.Serializable

@Serializable
data class AddAppCredentialRequest(
  val publicKey: String,
  val deviceName: String,
  val devicePlatform: String,
  val appAttestation: AddAppCredentialAppAttestation? = null,
)

@Serializable
data class AddAppCredentialAppAttestation(
  val provider: String,
  val token: String,
  val keyId: String? = null,
)