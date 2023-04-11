package com.authsignal.device

import java.time.Instant
import kotlin.math.floor

fun Authsignal(region: AuthsignalRegion = AuthsignalRegion.US): Authsignal {
  val baseURL = when(region) {
    AuthsignalRegion.US -> "https://challenge.authsignal.com/v1"
    AuthsignalRegion.AU -> "https://au-challenge.authsignal.com/v1"
    AuthsignalRegion.EU -> "https://eu-challenge.authsignal.com/v1"
  }

  return Authsignal(baseURL)
}

class Authsignal(baseURL: String) {
  private val api = ChallengeAPI(baseURL)

  suspend fun addCredential(accessToken: String): Boolean {
    val publicKey = KeyManager.getOrCreatePublicKey() ?: return false

    return api.addCredential(accessToken, publicKey)
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
}