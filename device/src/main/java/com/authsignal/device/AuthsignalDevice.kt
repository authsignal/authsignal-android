package com.authsignal.device

class AuthsignalDevice() {
  private val api = DeviceAPI()

  suspend fun addCredential(accessToken: String): Boolean {
    val publicKey = KeyManager.getOrCreatePublicKey() ?: return false

    return api.addCredential(accessToken, publicKey)
  }

  suspend fun removeCredential(): Boolean {
    val key = KeyManager.getKey() ?: return false

    val publicKey = KeyManager.derivePublicKey(key)

    val challengeId = api.startChallenge(publicKey) ?: return false

    val signature = Signer.sign(challengeId, key) ?: return false

    val success = api.removeCredential(challengeId, publicKey, signature)

    if (success) {
      KeyManager.deleteKey()
    }

    return success
  }

  suspend fun getChallenge(): String? {
    val publicKey = KeyManager.getPublicKey() ?: return null

    return api.getChallenge(publicKey)
  }

  suspend fun updateChallenge(challengeId: String, approved: Boolean): Boolean {
    val key = KeyManager.getKey() ?: return false

    val publicKey = KeyManager.derivePublicKey(key)

    val signature = Signer.sign(challengeId, key) ?: return false

    return api.updateChallenge(challengeId, publicKey, signature, approved)
  }
}