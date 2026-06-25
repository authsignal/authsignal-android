package com.authsignal.models.api

import kotlinx.serialization.Serializable

@Serializable
data class AppUpdateSignResponse(
  val challengeId: String? = null,
  val message: String? = null,
)
