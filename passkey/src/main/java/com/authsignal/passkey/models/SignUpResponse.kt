package com.authsignal.passkey.api.models

import kotlinx.serialization.Serializable

@Serializable
data class SignUpResponse(
  val token: String? = null,
)
