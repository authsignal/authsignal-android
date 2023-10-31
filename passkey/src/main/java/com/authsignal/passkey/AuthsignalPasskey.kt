package com.authsignal.passkey

import android.app.Activity
import android.content.Context
import com.authsignal.passkey.api.*
import com.authsignal.passkey.models.AuthsignalResponse
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.future.future
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.concurrent.CompletableFuture

class AuthsignalPasskey(
  tenantID: String,
  baseURL: String,
  context: Context,
  activity: Activity) {
  val api = PasskeyAPI(tenantID, baseURL)
  private val manager = PasskeyManager(context, activity)

  suspend fun signUp(token: String, userName: String): AuthsignalResponse<String> {
    val optsResponse = api.registrationOptions(token, userName)

    val optsData = optsResponse.data ?: return AuthsignalResponse(error = optsResponse.error)

    val options = optsData.options.copy(
      authenticatorSelection = optsData.options.authenticatorSelection.copy(
        requireResidentKey = false,
        userVerification = "required",
      ),
    )

    val optionsJson = Json.encodeToString(options)

    val registerResponse = manager.register(optionsJson)

    val credential = registerResponse.data ?: return AuthsignalResponse(error = registerResponse.error)

    val addAuthenticatorResponse = api.addAuthenticator(
      token,
      optsData.challengeId,
      credential,
    )

    val authenticatorData = addAuthenticatorResponse.data
      ?: return AuthsignalResponse(error = addAuthenticatorResponse.error)

    return AuthsignalResponse(data = authenticatorData.accessToken)
  }

  suspend fun signIn(token: String?): AuthsignalResponse<String> {
    val optsResponse = api.authenticationOptions(token)

    val optsData = optsResponse.data ?: return AuthsignalResponse(error = optsResponse.error)

    val optionsJson = Json.encodeToString(optsData.options)

    val authResponse = manager.auth(optionsJson)

    val credential =  authResponse.data ?: return AuthsignalResponse(error = authResponse.error)

    val verifyResponse = api.verify(
      optsData.challengeId,
      credential,
      token,
    )

    val verifyData = verifyResponse.data
      ?: return AuthsignalResponse(error = verifyResponse.error)

    return AuthsignalResponse(data = verifyData.accessToken)
  }

  @OptIn(DelicateCoroutinesApi::class)
  fun signUpAsync(token: String, userName: String): CompletableFuture<AuthsignalResponse<String>> =
    GlobalScope.future { signUp(token, userName) }

  @OptIn(DelicateCoroutinesApi::class)
  fun signInAsync(token: String? = null): CompletableFuture<AuthsignalResponse<String>> =
    GlobalScope.future { signIn(token) }
}