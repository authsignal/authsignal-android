package com.authsignal.qr

import com.authsignal.DeviceUtils
import com.authsignal.KeyManager
import com.authsignal.Signer
import com.authsignal.TokenCache
import com.authsignal.models.*
import com.authsignal.qr.api.QRCodeAPI
import com.authsignal.qr.api.models.ClaimChallengeResponse
import java.security.Signature

class AuthsignalQRCode(
  tenantID: String,
  baseURL: String) {
  private val api = QRCodeAPI(tenantID, baseURL)
  private val keyManager = KeyManager("qr_code")

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

  suspend fun claimChallenge(
    challengeId: String,
    signer: Signature? = null
  ): AuthsignalResponse<ClaimChallengeResponse> {
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

    return api.claimChallenge(challengeId, publicKey, signature)
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