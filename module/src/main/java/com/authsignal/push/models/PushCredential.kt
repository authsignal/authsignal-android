package com.authsignal.push.models

data class PushCredential(
  val credentialID: String,
  val createdAt: String,
  val lastAuthenticatedAt: String?)