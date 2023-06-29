package com.authsignal.push

import android.os.Build
import com.authsignal.push.api.PushAPI
import com.authsignal.push.models.PushCredential
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.future.future
import java.util.concurrent.CompletableFuture
import kotlin.math.floor

class AuthsignalPush(tenantID: String, baseURL: String) {
  private val api = PushAPI(tenantID, baseURL)

  suspend fun getCredential(): PushCredential? {
    val publicKey = KeyManager.getPublicKey() ?: return null

    return api.getCredential(publicKey)
  }

  suspend fun addCredential(token: String, deviceName: String? = null): Boolean {
    val publicKey = KeyManager.getOrCreatePublicKey() ?: return false

    val device = deviceName ?: getDeviceName()

    return api.addCredential(token, publicKey, device)
  }

  suspend fun removeCredential(): Boolean {
    val key = KeyManager.getKey() ?: return false

    val publicKey = KeyManager.derivePublicKey(key)

    val message = getTimeBasedDataToSign()

    val signature = Signer.sign(message, key) ?: return false

    val success = api.removeCredential(publicKey, signature)

    if (success) {
      KeyManager.deleteKey()
    }

    return success
  }

  suspend fun getChallenge(): String? {
    val publicKey = KeyManager.getPublicKey() ?: return null

    return api.getChallenge(publicKey)
  }

  suspend fun updateChallenge(
    challengeId: String,
    approved: Boolean,
    verificationCode: String? = null
  ): Boolean {
    val key = KeyManager.getKey() ?: return false

    val publicKey = KeyManager.derivePublicKey(key)

    val signature = Signer.sign(challengeId, key) ?: return false

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
  fun getCredentialAsync(): CompletableFuture<PushCredential?> =
    GlobalScope.future { getCredential() }

  @OptIn(DelicateCoroutinesApi::class)
  fun addCredentialAsync(token: String, deviceName: String? = null): CompletableFuture<Boolean> =
    GlobalScope.future { addCredential(token, deviceName) }

  @OptIn(DelicateCoroutinesApi::class)
  fun removeCredentialAsync(): CompletableFuture<Boolean> =
    GlobalScope.future { removeCredential() }

  @OptIn(DelicateCoroutinesApi::class)
  fun getChallengeAsync(): CompletableFuture<String?> =
    GlobalScope.future { getChallenge() }

  @OptIn(DelicateCoroutinesApi::class)
  fun updateChallengeAsync(
    challengeId: String,
    approved: Boolean,
    verificationCode: String? = null,
  ): CompletableFuture<Boolean> =
    GlobalScope.future { updateChallenge(challengeId, approved, verificationCode) }
}