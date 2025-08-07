package com.authsignal.passkey

import android.app.Activity
import android.content.Context
import com.authsignal.TokenCache
import com.authsignal.models.AuthsignalResponse
import com.authsignal.passkey.api.*
import com.authsignal.passkey.models.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.UUID

class AuthsignalPasskey(
  tenantID: String,
  baseURL: String,
  private val activity: Activity?,
  private val deviceId: String?) {
  private val api = PasskeyAPI(tenantID, baseURL)
  private val manager = PasskeyManager(activity)
  private val passkeyLocalKey = "@as_passkey_credential_id"
  private val defaultDeviceLocalKey = "@as_device_id"
  private val cache = TokenCache.shared

  suspend fun signUp(
    token: String? = null,
    username: String? = null,
    displayName: String? = null,
    preferImmediatelyAvailableCredentials: Boolean = true
  ): AuthsignalResponse<SignUpResponse> {
    val userToken = token ?: cache.token ?: return cache.handleTokenNotSetError()

    if (activity == null) {
      return PasskeySdkErrors.contextError()
    }

    val optsResponse = api.registrationOptions(userToken, username, displayName)

    val optsData = optsResponse.data ?: return AuthsignalResponse(
      error = optsResponse.error,
      errorCode = optsResponse.errorCode
    )

    val options = optsData.options.copy(
      authenticatorSelection = optsData.options.authenticatorSelection.copy(
        userVerification = "required",
      ),
      pubKeyCredParams = optsData.options.pubKeyCredParams.filter { it.alg != -8 },
    )

    val optionsJson = Json.encodeToString(options)

    val registerResponse = manager.register(optionsJson, preferImmediatelyAvailableCredentials)

    val credential = registerResponse.data ?: return AuthsignalResponse(
      error = registerResponse.error,
      errorCode = registerResponse.errorCode
    )

    val deviceId = deviceId ?: getDefaultDeviceId()

    val addAuthenticatorResponse = api.addAuthenticator(
      userToken,
      optsData.challengeId,
      credential,
      deviceId,
    )

    val authenticatorData = addAuthenticatorResponse.data
      ?: return AuthsignalResponse(
        error = addAuthenticatorResponse.error,
        errorCode = registerResponse.errorCode
      )

    if (authenticatorData.isVerified) {
      with (activity.getPreferences(Context.MODE_PRIVATE).edit()) {
        putString(passkeyLocalKey, credential.rawId)
        apply()
      }
    }

    val signUpResponse = SignUpResponse(
      token = authenticatorData.accessToken,
    )

    authenticatorData.accessToken.let {
      cache.token = it
    }

    return AuthsignalResponse(data = signUpResponse)
  }

  suspend fun signIn(
    action: String? = null,
    token: String? = null,
    preferImmediatelyAvailableCredentials: Boolean = true
  ): AuthsignalResponse<SignInResponse> {
    val userToken = if (action == null) token ?: cache.token else null

    if (activity == null) {
      return PasskeySdkErrors.contextError()
    }

    val challengeId = action?.let {
      val challengeResponse = api.challenge(it)

      challengeResponse.data?.challengeId
    }

    val optsResponse = api.authenticationOptions(userToken, challengeId)

    val optsData = optsResponse.data ?: return AuthsignalResponse(
      error = optsResponse.error,
      errorCode = optsResponse.errorCode
    )

    val optionsJson = Json.encodeToString(optsData.options)

    val authResponse = manager.auth(optionsJson, preferImmediatelyAvailableCredentials)

    val credential =  authResponse.data ?: return AuthsignalResponse(
      error = authResponse.error,
      errorCode = authResponse.errorCode,
    )

    val deviceId =  deviceId ?: getDefaultDeviceId()

    val verifyResponse = api.verify(
      optsData.challengeId,
      credential,
      userToken,
      deviceId,
    )

    val verifyData = verifyResponse.data
      ?: return AuthsignalResponse(
        error = verifyResponse.error,
        errorCode = verifyResponse.errorCode
      )

    if (verifyData.isVerified) {
      with (activity.getPreferences(Context.MODE_PRIVATE).edit()) {
        putString(passkeyLocalKey, credential.rawId)
        apply()
      }
    }

    verifyData.accessToken.let {
      cache.token = it
    }

    val signInResponse = SignInResponse(
      isVerified = verifyData.isVerified,
      token = verifyData.accessToken,
      userId = verifyData. userId,
      userAuthenticatorId = verifyData.userAuthenticatorId,
      username = verifyData.username,
      displayName = verifyData.userDisplayName,
    )

    return AuthsignalResponse(data = signInResponse)
  }

  suspend fun isAvailableOnDevice(): AuthsignalResponse<Boolean> {
    if (activity == null) {
      return PasskeySdkErrors.contextError()
    }

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

  private fun getDefaultDeviceId(): String {
    if (activity == null) {
      return "";
    }

    val preferences = activity.getPreferences(Context.MODE_PRIVATE)
    val defaultDeviceId = preferences.getString(defaultDeviceLocalKey, null)

    if (defaultDeviceId != null) {
      return defaultDeviceId
    }

    val newDefaultDeviceId = UUID.randomUUID().toString()

    with (activity.getPreferences(Context.MODE_PRIVATE).edit()) {
      putString(defaultDeviceLocalKey, newDefaultDeviceId)
      apply()
    }

    return newDefaultDeviceId
  }
}