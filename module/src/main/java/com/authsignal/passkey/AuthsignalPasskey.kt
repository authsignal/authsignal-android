package com.authsignal.passkey

import android.app.Activity
import android.os.Build
import android.util.Log
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.authsignal.DeviceCache
import com.authsignal.SdkErrorCodes
import com.authsignal.TokenCache
import com.authsignal.dataStore
import com.authsignal.models.AuthsignalResponse
import com.authsignal.passkey.api.*
import com.authsignal.passkey.api.models.Authenticator
import com.authsignal.passkey.models.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private const val TAG = "com.authsignal.passkey"

private val passkeyCredentialIdPreferencesKey = stringPreferencesKey("@as_passkey_credential_id")

class AuthsignalPasskey(
  tenantID: String,
  baseURL: String,
  private val activity: Activity? = null,
  private val deviceId: String? = null) {
  private val api = PasskeyAPI(tenantID, baseURL)
  private val manager = PasskeyManager(activity)
  private val cache = TokenCache.shared
  private val dataStore = activity?.applicationContext?.dataStore

  init {
    activity?.applicationContext?.let {
      DeviceCache.shared.initialize(it, deviceId)
    }
  }

  suspend fun signUp(
    token: String? = null,
    username: String? = null,
    displayName: String? = null,
    preferImmediatelyAvailableCredentials: Boolean = true,
    ignorePasskeyAlreadyExistsError: Boolean = false,
    syncCredentials: Boolean = true,
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

    if (ignorePasskeyAlreadyExistsError && registerResponse.errorCode == SdkErrorCodes.MatchedExcludedCredential) {
      return AuthsignalResponse()
    }

    val credential = registerResponse.data ?: return AuthsignalResponse(
      error = registerResponse.error,
      errorCode = registerResponse.errorCode
    )

    val deviceId = deviceId ?: DeviceCache.shared.getDefaultDeviceId()

    val addAuthenticatorResponse = api.addAuthenticator(
      userToken,
      optsData.challengeId,
      credential,
      deviceId,
    )

    val authenticatorData = addAuthenticatorResponse.data
      ?: return AuthsignalResponse(
        error = addAuthenticatorResponse.error,
        errorCode = addAuthenticatorResponse.errorCode
      )

    if (authenticatorData.isVerified) {
      storeCredentialId(credential.rawId, options.user.name)
    }

    val signUpResponse = SignUpResponse(
      token = authenticatorData.accessToken,
    )

    val accessToken = authenticatorData.accessToken

    accessToken.let {
      cache.token = it
    }

    if (syncCredentials && authenticatorData.isVerified && accessToken != null) {
      CoroutineScope(Dispatchers.IO).launch {
        syncPasskeysWithCredentialManager(
          rpId = options.rp.id,
          userId = options.user.id,
          credentialId = credential.rawId,
          token = accessToken,
        )
      }
    }

    return AuthsignalResponse(data = signUpResponse)
  }

  suspend fun signIn(
    action: String? = null,
    token: String? = null,
    preferImmediatelyAvailableCredentials: Boolean = true,
    syncCredentials: Boolean = true,
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

    val deviceId = deviceId ?: DeviceCache.shared.getDefaultDeviceId()

    val verifyResponse = api.verify(
      optsData.challengeId,
      credential,
      userToken,
      deviceId,
    )

    val verifyData = verifyResponse.data

    if (verifyData == null) {
      if (syncCredentials && verifyResponse.errorCode == SdkErrorCodes.UnknownCredential) {
        Log.i(TAG, "Passkey sync: signaling unknown credential to the system.")

        manager.signalUnknownCredential(optsData.options.rpId, credential.rawId)

        removeStoredCredentialId(credential.rawId)
      }

      return AuthsignalResponse(
        error = verifyResponse.error,
        errorCode = verifyResponse.errorCode
      )
    }

    if (verifyData.isVerified) {
      storeCredentialId(credential.rawId, verifyResponse.data.username)
    }

    val accessToken = verifyData.accessToken

    accessToken.let {
      cache.token = it
    }

    if (syncCredentials && verifyData.isVerified && accessToken != null) {
      CoroutineScope(Dispatchers.IO).launch {
        syncPasskeysWithCredentialManager(
          rpId = optsData.options.rpId,
          userId = credential.response.userHandle,
          credentialId = credential.rawId,
          token = accessToken,
        )
      }
    }

    val signInResponse = SignInResponse(
      isVerified = verifyData.isVerified,
      token = verifyData.accessToken,
      userId = verifyData.userId,
      userAuthenticatorId = verifyData.userAuthenticatorId,
      username = verifyData.username,
      displayName = verifyData.userDisplayName,
    )

    return AuthsignalResponse(data = signInResponse)
  }

  fun isSupported(): Boolean {
    return Build.VERSION.SDK_INT >= 28
  }

  suspend fun shouldPromptToCreatePasskey(username: String? = null): AuthsignalResponse<Boolean> {
    val existingCredentialId = getStoredCredentialId(username)
      ?: return AuthsignalResponse(data = true)

    val passkeyAuthenticatorResponse =  api.getPasskeyAuthenticator(existingCredentialId)

    if (passkeyAuthenticatorResponse.errorCode == SdkErrorCodes.InvalidCredential) {
      return AuthsignalResponse(data = true)
    }

    return AuthsignalResponse(
      data = false,
      error = passkeyAuthenticatorResponse.error,
      errorCode = passkeyAuthenticatorResponse.errorCode,
    )
  }

  @Deprecated("Use 'preferImmediatelyAvailableCredentials' to control what happens when a passkey isn't available.")
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

  private suspend fun syncPasskeysWithCredentialManager(
    rpId: String,
    userId: String,
    credentialId: String,
    token: String,
  ) {
    val authenticatorsResponse = api.getAuthenticators(token)

    val authenticators = authenticatorsResponse.data ?: return

    val credentialIds = buildAcceptedCredentialIds(authenticators, credentialId)

    Log.i(TAG, "Passkey sync: reporting ${credentialIds.size} accepted credential(s) to the system.")

    manager.signalAllAcceptedCredentials(rpId, userId, credentialIds)
  }

  private suspend fun storeCredentialId(credentialId: String, username: String?) {
    dataStore?.edit { settings ->
      settings[passkeyCredentialIdPreferencesKey] = credentialId

      username?.let {
        settings[getCredentialIdKey(it)] = credentialId
      }
    }
  }

  private suspend fun getStoredCredentialId(username: String?): String? {
    val key = getCredentialIdKey(username)

    return dataStore?.data?.map { preferences -> preferences[key] }?.first()
  }

  private fun getCredentialIdKey(username: String?): Preferences.Key<String> {
    return username?.let {
      stringPreferencesKey("${passkeyCredentialIdPreferencesKey.name}_${it}")
    } ?: passkeyCredentialIdPreferencesKey
  }

  private suspend fun removeStoredCredentialId(credentialId: String) {
    dataStore?.edit { settings ->
      val matchingKeys = settings.asMap().entries
        .filter { (key, value) ->
          key.name.startsWith(passkeyCredentialIdPreferencesKey.name) && value == credentialId
        }
        .map { it.key }

      matchingKeys.forEach { settings.remove(it) }
    }
  }
}

internal fun buildAcceptedCredentialIds(
  authenticators: List<Authenticator>,
  currentCredentialId: String,
): List<String> {
  val credentialIds = authenticators
    .filter { it.verificationMethod == "PASSKEY" }
    .mapNotNull { it.webauthnCredential?.credentialId }
    .toMutableList()

  if (!credentialIds.contains(currentCredentialId)) {
    credentialIds.add(currentCredentialId)
  }

  return credentialIds
}
