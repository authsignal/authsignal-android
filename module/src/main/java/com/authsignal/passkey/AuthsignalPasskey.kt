package com.authsignal.passkey

import android.app.Activity
import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.authsignal.TokenCache
import com.authsignal.models.AuthsignalResponse
import com.authsignal.passkey.api.*
import com.authsignal.passkey.models.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.UUID

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")
private val passkeyCredentialIdPreferencesKey = stringPreferencesKey("@as_passkey_credential_id")
private val defaultDeviceIdPreferencesKey = stringPreferencesKey("@as_device_id")

class AuthsignalPasskey(
  tenantID: String,
  baseURL: String,
  private val activity: Activity?,
  private val deviceId: String?) {
  private val api = PasskeyAPI(tenantID, baseURL)
  private val manager = PasskeyManager(activity)
  private val cache = TokenCache.shared
  private val dataStore = activity?.applicationContext?.dataStore

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
      dataStore?.edit { settings ->
        settings[passkeyCredentialIdPreferencesKey] = credential.rawId
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
      dataStore?.edit { settings ->
        settings[passkeyCredentialIdPreferencesKey] = credential.rawId
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
    val hasPasskeyCredentialAvailable = dataStore?.data
      ?.map { preferences ->
        preferences[passkeyCredentialIdPreferencesKey]
      }?.map { credentialId ->
        credentialId?.let {
          try {
            api.getPasskeyAuthenticator(it).error == null
          } catch (e: Exception) {
            false
          }
        } ?: false
      }?.first() ?: false

    return AuthsignalResponse(data = hasPasskeyCredentialAvailable)
  }

  private suspend fun getDefaultDeviceId(): String {
    val store = dataStore ?: return ""

    val defaultDeviceId = store.data
      .map { preferences -> preferences[defaultDeviceIdPreferencesKey] }
      .first()

    return defaultDeviceId ?: UUID.randomUUID().toString().also { newId ->
      store.edit { preferences ->
        preferences[defaultDeviceIdPreferencesKey] = newId
      }
    }
  }
}