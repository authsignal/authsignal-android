package com.authsignal.passkey.api.models

import com.authsignal.passkey.models.PasskeyAuthenticationCredential
import kotlinx.serialization.Serializable

@Serializable
data class VerifyRequest(
  val challengeId: String,
  val authenticationCredential: PasskeyAuthenticationCredential,
)
