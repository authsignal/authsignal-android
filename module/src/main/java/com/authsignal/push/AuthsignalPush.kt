package com.authsignal.push

import android.content.Context
import com.authsignal.DeviceUtils
import com.authsignal.Encoder
import com.authsignal.KeyManager
import com.authsignal.SdkErrorCodes
import com.authsignal.Signer
import com.authsignal.TokenCache
import com.authsignal.inapp.PlayIntegrityManager
import com.authsignal.models.AuthsignalResponse
import com.authsignal.push.api.PushAPI
import com.authsignal.models.AppChallenge
import com.authsignal.models.AppCredential
import java.security.Signature

class AuthsignalPush(
  tenantID: String,
  baseURL: String,
  context: Context? = null) {
  private val api = PushAPI(tenantID, baseURL)
  private val keyManager = KeyManager("push")
  private val playIntegrityManager = PlayIntegrityManager(context)

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
    deviceIntegrity: Boolean = false,
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

    var deviceIntegrityToken: String? = null

    if (deviceIntegrity) {
      val nonce = Encoder.getJwtClaim(userToken, "idempotencyKey")
        ?: return AuthsignalResponse(
          error = "Failed to extract idempotencyKey from token.",
          errorCode = SdkErrorCodes.SdkError,
        )

      val integrityResponse = playIntegrityManager.requestToken(nonce)

      deviceIntegrityToken = integrityResponse.data ?: return AuthsignalResponse(
        error = integrityResponse.error,
        errorCode = integrityResponse.errorCode,
      )
    }

    return api.addCredential(userToken, publicKey, device, deviceIntegrityToken)
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

  suspend fun getChallenge(): AuthsignalResponse<AppChallenge?> {
    val publicKeyResponse = keyManager.getPublicKey()

    val publicKey = publicKeyResponse.data
      ?: return AuthsignalResponse(
        error = publicKeyResponse.error,
        errorCode = publicKeyResponse.errorCode
      )

    return api.getChallenge(publicKey)
  }

  suspend fun updateChallenge(
    challengeId: String,
    approved: Boolean,
    verificationCode: String? = null,
    signer: Signature? = null
  ): AuthsignalResponse<Boolean> {
    val keyResponse = keyManager.getKey()

    val key = keyResponse.data
      ?: return AuthsignalResponse(error = keyResponse.error)

    val signatureResponse = if (signer != null) {
      Signer.finishSigning(challengeId, signer)
    } else {
      Signer.sign(challengeId, key)
    }

    val signature = signatureResponse.data ?: return AuthsignalResponse(error = signatureResponse.error)

    val publicKey = keyManager.derivePublicKey(key)

    return api.updateChallenge(challengeId, publicKey, signature, approved, verificationCode)
  }

  fun startSigning(): Signature? {
    val keyResponse = keyManager.getKey()

    val key = keyResponse.data
      ?: return null

    return Signer.startSigning(key)
  }
}