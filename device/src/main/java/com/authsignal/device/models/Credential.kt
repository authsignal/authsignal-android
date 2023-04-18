package com.authsignal.device.models

data class Credential(
  val credentialId: String,
  val createdAt: String,
  val lastAuthenticatedAt: String)