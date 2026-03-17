package com.authsignal.models.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class AppAttestationProvider {
  @SerialName("playIntegrity")
  PLAY_INTEGRITY,

  @SerialName("appAttest")
  APP_ATTEST,
}

@Serializable
data class AppAttestation(
  val provider: AppAttestationProvider,
  val token: String,
  val keyId: String? = null,
)
