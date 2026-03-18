package com.authsignal.models.api

data class AppAttestation(
  val token: String,
  val keyId: String? = null,
)
