package com.authsignal.models.api

import kotlinx.serialization.Serializable

@Serializable
data class RemoveAppCredentialRequest(
  val publicKey: String,
  val signature: String,
)