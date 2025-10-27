package com.authsignal.models

import kotlinx.serialization.Serializable

@Serializable
data class AppCredential(
  val credentialId: String,
  val createdAt: String,
  val userId: String,
  val lastAuthenticatedAt: String? = null,
)