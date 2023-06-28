package com.authsignal.passkey.api.models

import kotlinx.serialization.Serializable

@Serializable
data class AuthenticationOptsRequest(
  val username: String? = null,
)