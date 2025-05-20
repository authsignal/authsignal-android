package com.authsignal.device.models

data class DeviceChallenge(
  val challengeId: String,
  val userId: String,
  val actionCode: String?,
  val idempotencyKey: String?,
  val deviceId: String?,
  val userAgent: String?,
  val ipAddress: String?,
)