package com.authsignal.inapp

import android.content.Context
import com.authsignal.DeviceUtils
import com.authsignal.KeyManager
import com.authsignal.PinManager
import com.authsignal.SdkErrorCodes
import com.authsignal.Signer
import com.authsignal.TokenCache
import com.authsignal.inapp.api.InAppAPI
import com.authsignal.inapp.api.models.*
import com.authsignal.models.*
import java.security.Signature

class AuthsignalInApp(
  tenantID: String,
  baseURL: String,
  context: Context? = null) {
  private val api = InAppAPI(tenantID, baseURL)
  private val keyManager = KeyManager("in_app")
  private val pinManager = PinManager(context = context)

  suspend fun getCredential(username: String? = null): AuthsignalResponse<AppCredential> {
    val publicKeyResponse = keyManager.getPublicKey(username)

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
    username: String? = null,
  ): AuthsignalResponse<AppCredential> {
    val userToken = token ?: TokenCache.shared.token ?: return TokenCache.shared.handleTokenNotSetError()

    val publicKeyResponse = keyManager.getOrCreatePublicKey(
      userAuthenticationRequired,
      timeout,
      authorizationType,
      username,
    )

    val publicKey = publicKeyResponse.data ?: return AuthsignalResponse(
      error = publicKeyResponse.error,
      errorCode = publicKeyResponse.errorCode,
    )

    val device = deviceName ?: DeviceUtils.getDeviceName()

    return api.addCredential(userToken, publicKey, device)
  }

  suspend fun removeCredential(
    signer: Signature? = null,
    username: String? = null,
  ): AuthsignalResponse<Boolean> {
    val keyResponse = keyManager.getKey(username)

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
      data = keyManager.deleteKey(username),
      error = removeCredentialResponse.error,
      errorCode = removeCredentialResponse.errorCode,
    )
  }

  suspend fun verify(
    action: String? = null,
    username: String? = null,
  ): AuthsignalResponse<InAppVerifyResponse> {
    val challengeResponse = api.challenge(action = action)

    val challengeId = challengeResponse.data?.challengeId
      ?: return AuthsignalResponse(error = challengeResponse.error)

    val keyResponse = keyManager.getKey(username)

    val key = keyResponse.data
      ?: return AuthsignalResponse(error = keyResponse.error)

    val signatureResponse = Signer.sign(challengeId, key)

    val signature = signatureResponse.data ?: return AuthsignalResponse(error = signatureResponse.error)

    val publicKey = keyManager.derivePublicKey(key)

    val userToken = if (action == null) TokenCache.shared.token else null

    return api.verify(challengeId, publicKey, signature, userToken)
  }

  suspend fun createPin(
    pin: String,
    username: String,
    token: String? = null
  ): AuthsignalResponse<AppCredential> {
    if (!pinManager.validateFormat(pin)) {
      return AuthsignalResponse(
        error = "Invalid PIN format.",
        errorCode = SdkErrorCodes.InvalidPinFormat
      )
    }

    pinManager.createPin(pin, username)

    return addCredential(token, username)
  }

  suspend fun verifyPin(
    pin: String,
    username: String,
    action: String? = null
  ): AuthsignalResponse<VerifyPinResponse> {
    val pinResponse = pinManager.validatePin(pin, username)

    val isPinValid = pinResponse.data ?: return AuthsignalResponse(
      error = pinResponse.error,
      errorCode = pinResponse.errorCode,
    )

    if (isPinValid) {
      val verifyResponse = verify(action, username)

      verifyResponse.error?.let { error ->
        return AuthsignalResponse(
          error = error,
          errorCode = verifyResponse.errorCode
        )
      }

      verifyResponse.data?.let { verifyResponseData ->
        val data = VerifyPinResponse(
          isVerified = true,
          token = verifyResponseData.token,
          userId = verifyResponseData.userId
        )

        return AuthsignalResponse(data = data)
      }
    }

    val data = VerifyPinResponse(
      isVerified = false,
      token = null,
      userId = null
    )

    return AuthsignalResponse(data = data)
  }

  suspend fun deletePin(username: String): AuthsignalResponse<Boolean> {
    val pinManagerResponse = pinManager.deletePin(username)

    if (pinManagerResponse.data != true) {
      return AuthsignalResponse(
        error = pinManagerResponse.error,
        errorCode = pinManagerResponse.errorCode,
      )
    }

    return removeCredential(username = username)
  }

  fun getAllUsernames(): AuthsignalResponse<List<String>> {
    return pinManager.getAllUsernames()
  }
}