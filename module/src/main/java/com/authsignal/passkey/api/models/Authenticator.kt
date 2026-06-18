package com.authsignal.passkey.api.models

import kotlinx.serialization.Serializable

@Serializable
data class Authenticator(
  val userAuthenticatorId: String? = null,
  val verificationMethod: String? = null,
  val webauthnCredential: WebauthnCredential? = null,
)

@Serializable
data class WebauthnCredential(
  val credentialId: String,
)
