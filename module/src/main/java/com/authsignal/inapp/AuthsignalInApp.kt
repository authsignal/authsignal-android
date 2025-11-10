package com.authsignal.inapp

import com.authsignal.DeviceUtils
import com.authsignal.KeyManager
import com.authsignal.Signer
import com.authsignal.TokenCache
import com.authsignal.inapp.api.InAppAPI
import com.authsignal.inapp.api.models.*
import com.authsignal.models.*
import java.security.Signature

class AuthsignalInApp(
  tenantID: String,
  baseURL: String) {
  private val api = InAppAPI(tenantID, baseURL)
  private val keyManager = KeyManager("in_app")

  suspend fun getCredential(): AuthsignalResponse<AppCredential> {
    val publicKeyResponse = keyManager.getPublicKey()

    val publicKey = publicKeyResponse.data
      ?: return AuthsignalResponse(
        error = publicKeyResponse.error,
        errorCode = publicKeyResponse.errorCode
      )

    return api.getCredential(publicKey)
  }

  suspend fun addCredential(
    token: String? = null,
    deviceName: String? = null,
    userAuthenticationRequired: Boolean = false,
    timeout: Int = 0,
    authorizationType: Int = 0,
  ): AuthsignalResponse<AppCredential> {
    val userToken = token ?: TokenCache.shared.token ?: return TokenCache.shared.handleTokenNotSetError()

    val publicKeyResponse = keyManager.getOrCreatePublicKey(
      userAuthenticationRequired,
      timeout,
      authorizationType
    )

    val publicKey = publicKeyResponse.data ?: return AuthsignalResponse(
      error = publicKeyResponse.error,
      errorCode = publicKeyResponse.errorCode,
    )

    val device = deviceName ?: DeviceUtils.getDeviceName()

    return api.addCredential(userToken, publicKey, device)
  }

  suspend fun removeCredential(signer: Signature? = null): AuthsignalResponse<Boolean> {
    val keyResponse = keyManager.getKey()

    val key = keyResponse.data
      ?: return AuthsignalResponse(error = keyResponse.error)

    val publicKey = keyManager.derivePublicKey(key)

    val signatureResponse = if (signer != null) {
      Signer.finishSigningWithTimeBasedMessage(signer)
    } else {
      Signer.signWithTimeBasedMessage(key)
    }

    val signature = signatureResponse.data ?: return AuthsignalResponse(
      error = signatureResponse.error,
      errorCode = signatureResponse.errorCode
    )

    val removeCredentialResponse = api.removeCredential(publicKey, signature)

    return AuthsignalResponse(
      data = keyManager.deleteKey(),
      error = removeCredentialResponse.error,
      errorCode = removeCredentialResponse.errorCode,
    )
  }

  suspend fun verify(action: String? = null): AuthsignalResponse<InAppVerifyResponse> {
    val challengeResponse = api.challenge(action = action)

    val challengeId = challengeResponse.data?.challengeId
      ?: return AuthsignalResponse(error = challengeResponse.error)

    val keyResponse = keyManager.getKey()

    val key = keyResponse.data
      ?: return AuthsignalResponse(error = keyResponse.error)

    val signatureResponse = Signer.sign(challengeId, key)

    val signature = signatureResponse.data ?: return AuthsignalResponse(error = signatureResponse.error)

    val publicKey = keyManager.derivePublicKey(key)

    val userToken = if (action == null) TokenCache.shared.token else null

    return api.verify(challengeId, publicKey, signature, userToken)
  }
}