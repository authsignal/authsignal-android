package com.authsignal.device

class AuthsignalDevice(tenantId: String) {
  private val tenantId = tenantId

  private val api = DeviceAPI(tenantId)

  suspend fun addCredential(accessToken: String): Boolean {
    val publicKey = KeyManager.getOrCreatePublicKey() ?: return false

    return api.addCredential(accessToken, publicKey)
  }

  suspend fun removeCredential(): Boolean {
    val key = KeyManager.getKey() ?: return false

    val publicKey = KeyManager.derivePublicKey(key)

    val signature = Signer.sign(tenantId, key) ?: return false

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

  suspend fun updateChallenge(challengeId: String, approved: Boolean): Boolean {
    val key = KeyManager.getKey() ?: return false

    val publicKey = KeyManager.derivePublicKey(key)

    val signature = Signer.sign(challengeId, key) ?: return false

    return api.updateChallenge(challengeId, publicKey, signature, approved)
  }
}