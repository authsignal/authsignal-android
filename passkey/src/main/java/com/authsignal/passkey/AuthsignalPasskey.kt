package com.authsignal.passkey

import android.app.Activity
import android.content.Context
import com.authsignal.passkey.api.*
import com.authsignal.passkey.models.*
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.future.future
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.UUID
import java.util.concurrent.CompletableFuture

class AuthsignalPasskey(
  tenantID: String,
  baseURL: String,
  context: Context,
  activity: Activity) {
  private val api = PasskeyAPI(tenantID, baseURL)
  private val manager = PasskeyManager(context, activity)
  private val activity = activity
  private val passkeyLocalKey = "@as_passkey_credential_id"
  private val defaultDeviceLocalKey = "@as_device_id"

  suspend fun signUp(token: String, userName: String? = null, displayName: String? = null): AuthsignalResponse<SignUpResponse> {
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

    val signUpResponse = SignUpResponse(
      token = authenticatorData.accessToken,
    )

    return AuthsignalResponse(data = signUpResponse)
  }

  suspend fun signIn(action: String? = null, token: String? = null): AuthsignalResponse<SignInResponse> {
    val challengeID = action?.let {
      val challengeResponse = api.challenge(it)

      challengeResponse.data?.challengeId
    }

    val optsResponse = api.authenticationOptions(token, challengeID)

    val optsData = optsResponse.data ?: return AuthsignalResponse(error = optsResponse.error)

    val optionsJson = Json.encodeToString(optsData.options)

    val authResponse = manager.auth(optionsJson)

    val credential =  authResponse.data ?: return AuthsignalResponse(
      error = authResponse.error,
      errorType = authResponse.errorType,
    )

    val deviceID =  getDefaultDeviceID()

    val verifyResponse = api.verify(
      optsData.challengeId,
      credential,
      token,
      deviceID,
    )

    val verifyData = verifyResponse.data
      ?: return AuthsignalResponse(error = verifyResponse.error)

    if (verifyData.isVerified) {
      with (activity.getPreferences(Context.MODE_PRIVATE).edit()) {
        putString(passkeyLocalKey, credential.rawId)
        apply()
      }
    }

    val signInResponse = SignInResponse(
      isVerified = verifyData.isVerified,
      token = verifyData.accessToken,
      userId = verifyData. userId,
      userAuthenticatorId = verifyData.userAuthenticatorId,
      userName = verifyData.username,
      userDisplayName = verifyData.userDisplayName,
    )

    return AuthsignalResponse(data = signInResponse)
  }

  suspend fun isAvailableOnDevice(): AuthsignalResponse<Boolean> {
    val preferences = activity.getPreferences(Context.MODE_PRIVATE)
    val credentialID = preferences.getString(passkeyLocalKey, null)
      ?: return AuthsignalResponse(data = false)

    val passkeyAuthenticatorResponse = api.getPasskeyAuthenticator(credentialID)

    return if (passkeyAuthenticatorResponse.error != null) {
      AuthsignalResponse(data = false, error = passkeyAuthenticatorResponse.error)
    } else {
      AuthsignalResponse(data = true)
    }
  }

  private fun getDefaultDeviceID(): String {
    val preferences = activity.getPreferences(Context.MODE_PRIVATE)
    val defaultDeviceID = preferences.getString(defaultDeviceLocalKey, null)

    if (defaultDeviceID != null) {
      return defaultDeviceID
    }

    val newDefaultDeviceID = UUID.randomUUID().toString()

    with (activity.getPreferences(Context.MODE_PRIVATE).edit()) {
      putString(passkeyLocalKey, newDefaultDeviceID)
      apply()
    }

    return newDefaultDeviceID
  }

  @OptIn(DelicateCoroutinesApi::class)
  fun signUpAsync(token: String, userName: String? = null, displayName: String? = null): CompletableFuture<AuthsignalResponse<SignUpResponse>> =
    GlobalScope.future { signUp(token, userName, displayName) }

  @OptIn(DelicateCoroutinesApi::class)
  fun signInAsync(action: String? = null, token: String? = null): CompletableFuture<AuthsignalResponse<SignInResponse>> =
    GlobalScope.future { signIn(action, token) }

  @OptIn(DelicateCoroutinesApi::class)
  fun isAvailableOnDeviceAsync(): CompletableFuture<AuthsignalResponse<Boolean>> =
    GlobalScope.future { isAvailableOnDevice() }
}