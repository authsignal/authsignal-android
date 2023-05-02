package com.authsignal.push

import android.os.Build
import com.authsignal.push.models.Credential
import kotlin.math.floor

class AuthsignalPush(baseURL: String) {
  private val api = PushAPI(baseURL)

  suspend fun getCredential(): Credential? {
    val publicKey = KeyManager.getPublicKey() ?: return null

    return api.getCredential(publicKey)
  }

  suspend fun addCredential(accessToken: String, deviceName: String? = null): Boolean {
    val publicKey = KeyManager.getOrCreatePublicKey() ?: return false

    val device = deviceName ?: getDeviceName()

    return api.addCredential(accessToken, publicKey, device)
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
}