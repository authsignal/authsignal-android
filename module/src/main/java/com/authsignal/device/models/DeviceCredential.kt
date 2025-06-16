package com.authsignal.device.models

data class DeviceCredential(
  val credentialId: String,
  val createdAt: String,
  val userId: String,
  val lastAuthenticatedAt: String? = null
)