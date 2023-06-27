package com.authsignal.passkey

import android.app.Activity
import android.content.Context
import com.authsignal.passkey.api.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class AuthsignalPasskey(
  tenantID: String,
  baseURL: String,
  context: Context,
  activity: Activity) {
  private val api = PasskeyAPI(tenantID, baseURL)
  private val manager = PasskeyManager(context, activity)

  suspend fun signUp(token: String, userName: String): String? {
    val optsResponse = api.registrationOptions(token, userName) ?: return null

    val options = optsResponse.options.copy(
      authenticatorSelection = optsResponse.options.authenticatorSelection.copy(
        requireResidentKey = false,
        userVerification = "required",
      ),
    )

    val optionsJson = Json.encodeToString(options)

    val credential = manager.register(optionsJson) ?: return null

    val addAuthenticatorResponse = api.addAuthenticator(
      token,
      optsResponse.challengeId,
      credential,
    )

    return addAuthenticatorResponse?.accessToken
  }

  suspend fun signIn(token: String): String? {
    val optsResponse = api.authenticationOptions(token) ?: return null

    val optionsJson = Json.encodeToString(optsResponse.options)

    val credential = manager.auth(optionsJson) ?: return null

    val verifyResponse = api.verify(
      token,
      optsResponse.challengeId,
      credential,
    )

    return verifyResponse?.accessToken
  }
}