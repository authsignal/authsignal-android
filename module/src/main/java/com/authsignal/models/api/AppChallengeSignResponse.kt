package com.authsignal.models.api

import kotlinx.serialization.Serializable

@Serializable
data class AppChallengeSignResponse(
  val message: String? = null,
)
