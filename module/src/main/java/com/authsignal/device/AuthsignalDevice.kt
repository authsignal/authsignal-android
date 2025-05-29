package com.authsignal.device

import android.os.Build
import com.authsignal.TokenCache
import com.authsignal.models.AuthsignalResponse
import com.authsignal.device.api.DeviceAPI
import com.authsignal.device.models.DeviceChallenge
import com.authsignal.device.models.DeviceCredential
import java.security.Signature
import kotlin.math.floor

class AuthsignalDevice(
  tenantID: String,
  baseURL: String) {
  private val api = DeviceAPI(tenantID, baseURL)

  suspend fun getCredential(): AuthsignalResponse<DeviceCredential> {
    val publicKeyResponse = KeyManager.getPublicKey()

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
  ): AuthsignalResponse<Boolean> {
    val userToken = token ?: TokenCache.shared.token ?: return TokenCache.shared.handleTokenNotSetError()

    val publicKeyResponse = KeyManager.getOrCreatePublicKey(
      userAuthenticationRequired,
      timeout,
      authorizationType
    )

    val publicKey = publicKeyResponse.data ?: return AuthsignalResponse(
      data = false,
      error = publicKeyResponse.error,
      errorCode = publicKeyResponse.errorCode,
    )

    val device = deviceName ?: getDeviceName()

    return api.addCredential(userToken, publicKey, device)
  }

  suspend fun removeCredential(signer: Signature? = null): AuthsignalResponse<Boolean> {
    val keyResponse = KeyManager.getKey()

    val key = keyResponse.data
      ?: return AuthsignalResponse(error = keyResponse.error)

    val publicKey = KeyManager.derivePublicKey(key)

    val message = getTimeBasedDataToSign()

    val signatureResponse = if (signer != null) {
      Signer.finishSigning(message, signer)
    } else {
      Signer.sign(message, key)
    }

    val signature = signatureResponse.data ?: return AuthsignalResponse(
      error = signatureResponse.error,
      errorCode = signatureResponse.errorCode
    )

    val removeCredentialResponse = api.removeCredential(publicKey, signature)

    return AuthsignalResponse(
      data = KeyManager.deleteKey(),
      error = removeCredentialResponse.error,
      errorCode = removeCredentialResponse.errorCode,
    )
  }

  suspend fun getChallenge(): AuthsignalResponse<DeviceChallenge?> {
    val publicKeyResponse = KeyManager.getPublicKey()

    val publicKey = publicKeyResponse.data
      ?: return AuthsignalResponse(
        error = publicKeyResponse.error,
        errorCode = publicKeyResponse.errorCode
      )

    val deviceChallengeResponse = api.getChallenge(publicKey)

    val deviceChallengeData = deviceChallengeResponse.data
      ?: return AuthsignalResponse(
        data = null,
        error = deviceChallengeResponse.error,
        errorCode = deviceChallengeResponse.errorCode
      )

    val challengeId = deviceChallengeData.challengeId
      ?: return AuthsignalResponse(
        data = null,
        error = deviceChallengeResponse.error,
        errorCode = deviceChallengeResponse.errorCode
      )

    val userId = deviceChallengeData.userId
      ?: return AuthsignalResponse(data = null)

    val deviceChallenge = DeviceChallenge(
      challengeId = challengeId,
      userId = userId,
      actionCode = deviceChallengeData.actionCode,
      idempotencyKey = deviceChallengeData.idempotencyKey,
      userAgent = deviceChallengeData.userAgent,
      ipAddress = deviceChallengeData.ipAddress,
      deviceId = deviceChallengeData.deviceId,
    )

    return AuthsignalResponse(data = deviceChallenge)
  }

  suspend fun claimChallenge(
    challengeId: String,
    signer: Signature? = null
  ): AuthsignalResponse<Boolean> {
    val keyResponse = KeyManager.getKey()

    val key = keyResponse.data
      ?: return AuthsignalResponse(error = keyResponse.error)

    val signatureResponse = if (signer != null) {
      Signer.finishSigning(challengeId, signer)
    } else {
      Signer.sign(challengeId, key)
    }

    val signature = signatureResponse.data ?: return AuthsignalResponse(error = signatureResponse.error)

    val publicKey = KeyManager.derivePublicKey(key)

    return api.claimChallenge(challengeId, publicKey, signature)
  }

  suspend fun updateChallenge(
    challengeId: String,
    approved: Boolean,
    verificationCode: String? = null,
    signer: Signature? = null
  ): AuthsignalResponse<Boolean> {
    val keyResponse = KeyManager.getKey()

    val key = keyResponse.data
      ?: return AuthsignalResponse(error = keyResponse.error)

    val signatureResponse = if (signer != null) {
      Signer.finishSigning(challengeId, signer)
    } else {
      Signer.sign(challengeId, key)
    }

    val signature = signatureResponse.data ?: return AuthsignalResponse(error = signatureResponse.error)

    val publicKey = KeyManager.derivePublicKey(key)

    return api.updateChallenge(challengeId, publicKey, signature, approved, verificationCode)
  }

  fun startSigning(): Signature? {
    val keyResponse = KeyManager.getKey()

    val key = keyResponse.data
      ?: return null

    return Signer.startSigning(key)
  }

  private fun getTimeBasedDataToSign(): String {
    val secondsSinceEpoch = (System.currentTimeMillis() / 1000).toDouble()

    return floor(secondsSinceEpoch / (60 * 10)).toString()
  }

  private fun getDeviceName(): String {
    val manufacturer = Build.MANUFACTURER
    val model = Build.MODEL

    if (model.startsWith(manufacturer)) {
      return model
    }

    return "$manufacturer $model"
  }
}