package com.authsignal.push

import android.os.Build
import com.authsignal.TokenCache
import com.authsignal.models.AuthsignalResponse
import com.authsignal.push.api.PushAPI
import com.authsignal.push.models.PushCredential
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.future.future
import java.util.concurrent.CompletableFuture
import kotlin.math.floor

class AuthsignalPush(
  tenantID: String,
  baseURL: String) {
  private val api = PushAPI(tenantID, baseURL)

  suspend fun getCredential(): AuthsignalResponse<PushCredential> {
    val publicKey = KeyManager.getPublicKey()
      ?: return AuthsignalResponse(error = "Public key not found")

    return api.getCredential(publicKey)
  }

  suspend fun addCredential(
    token: String? = TokenCache.shared.token,
    deviceName: String? = null,
  ): AuthsignalResponse<Boolean> {
    val userToken = token ?: return TokenCache.shared.handleTokenNotSetError()

    val publicKey = KeyManager.getOrCreatePublicKey()
      ?: return AuthsignalResponse(error = "Error registering key pair")

    val device = deviceName ?: getDeviceName()

    return api.addCredential(userToken, publicKey, device)
  }

  suspend fun removeCredential(): AuthsignalResponse<Boolean> {
    val key = KeyManager.getKey()
      ?: return AuthsignalResponse(error = "Error retrieving key pair")

    val publicKey = KeyManager.derivePublicKey(key)

    val message = getTimeBasedDataToSign()

    val signature = Signer.sign(message, key)
      ?: return AuthsignalResponse(error = "Error generating signature")

    val removeCredentialResponse = api.removeCredential(publicKey, signature)

    removeCredentialResponse.data.let {
      KeyManager.deleteKey()
    }

    return removeCredentialResponse
  }

  suspend fun getChallenge(): AuthsignalResponse<String> {
    val publicKey = KeyManager.getPublicKey()
      ?: return AuthsignalResponse(error = "Public key not found")

    return api.getChallenge(publicKey)
  }

  suspend fun updateChallenge(
    challengeId: String,
    approved: Boolean,
    verificationCode: String? = null
  ): AuthsignalResponse<Boolean> {
    val key = KeyManager.getKey()
      ?: return AuthsignalResponse(error = "Error retrieving key pair")

    val publicKey = KeyManager.derivePublicKey(key)

    val signature = Signer.sign(challengeId, key)
      ?: return AuthsignalResponse(error = "Error generating signature")

    return api.updateChallenge(challengeId, publicKey, signature, approved, verificationCode)
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

  @OptIn(DelicateCoroutinesApi::class)
  fun getCredentialAsync(): CompletableFuture<AuthsignalResponse<PushCredential>> =
    GlobalScope.future { getCredential() }

  @OptIn(DelicateCoroutinesApi::class)
  fun addCredentialAsync(
    token: String? = null,
    deviceName: String? = null,
  ): CompletableFuture<AuthsignalResponse<Boolean>> =
    GlobalScope.future { addCredential(token, deviceName) }

  @OptIn(DelicateCoroutinesApi::class)
  fun removeCredentialAsync(): CompletableFuture<AuthsignalResponse<Boolean>> =
    GlobalScope.future { removeCredential() }

  @OptIn(DelicateCoroutinesApi::class)
  fun getChallengeAsync(): CompletableFuture<AuthsignalResponse<String>> =
    GlobalScope.future { getChallenge() }

  @OptIn(DelicateCoroutinesApi::class)
  fun updateChallengeAsync(
    challengeId: String,
    approved: Boolean,
    verificationCode: String? = null,
  ): CompletableFuture<AuthsignalResponse<Boolean>> =
    GlobalScope.future { updateChallenge(challengeId, approved, verificationCode) }
}