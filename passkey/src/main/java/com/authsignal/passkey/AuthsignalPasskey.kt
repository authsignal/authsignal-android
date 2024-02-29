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
  private val activity = activity
  private val passkeyLocalKey = "@as_passkey_credential_id"

  suspend fun signUp(token: String, userName: String? = null, displayName: String? = null): AuthsignalResponse<String> {
    val optsResponse = api.registrationOptions(token, userName, displayName)

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

    if (authenticatorData.isVerified) {
      with (activity.getPreferences(Context.MODE_PRIVATE).edit()) {
        putString(passkeyLocalKey, credential.rawId)
        apply()
      }
    }

    return AuthsignalResponse(data = authenticatorData.accessToken)
  }

  suspend fun signIn(token: String? = null): AuthsignalResponse<String> {
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

    if (verifyData.isVerified) {
      with (activity.getPreferences(Context.MODE_PRIVATE).edit()) {
        putString(passkeyLocalKey, credential.rawId)
        apply()
      }
    }

    return AuthsignalResponse(data = verifyData.accessToken)
  }

  suspend fun isAvailableOnDevice(): AuthsignalResponse<Boolean> {
    val preferences = activity.getPreferences(Context.MODE_PRIVATE)
    val credentialId = preferences.getString(passkeyLocalKey, null)
      ?: return AuthsignalResponse(data = false)

    val passkeyAuthenticatorResponse = api.getPasskeyAuthenticator(credentialId)

    return if (passkeyAuthenticatorResponse.error != null) {
      AuthsignalResponse(data = false, error = passkeyAuthenticatorResponse.error)
    } else {
      AuthsignalResponse(data = true)
    }
  }

  @OptIn(DelicateCoroutinesApi::class)
  fun signUpAsync(token: String, userName: String? = null, displayName: String? = null): CompletableFuture<AuthsignalResponse<String>> =
    GlobalScope.future { signUp(token, userName, displayName) }

  @OptIn(DelicateCoroutinesApi::class)
  fun signInAsync(token: String? = null): CompletableFuture<AuthsignalResponse<String>> =
    GlobalScope.future { signIn(token) }

  @OptIn(DelicateCoroutinesApi::class)
  fun isAvailableOnDeviceAsync(): CompletableFuture<AuthsignalResponse<Boolean>> =
    GlobalScope.future { isAvailableOnDevice() }
}