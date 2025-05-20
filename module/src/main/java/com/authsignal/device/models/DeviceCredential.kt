package com.authsignal.device.models

data class DeviceCredential(
  val credentialId: String,
  val createdAt: String,
  val lastAuthenticatedAt: String?
)