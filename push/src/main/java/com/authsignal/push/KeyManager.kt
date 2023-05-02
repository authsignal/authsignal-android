package com.authsignal.push

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.spec.X509EncodedKeySpec

object KeyManager {
  private const val keyName = "authsignal_signing_key"

  fun getOrCreatePublicKey(): String? {
    return getPublicKey() ?: createKeyPair()
  }

  fun getPublicKey(): String? {
    val key = getKey() ?: return null

    return derivePublicKey(key)
  }

  fun getKey(): KeyStore.PrivateKeyEntry? {
    return try {
      val keyStore = KeyStore.getInstance("AndroidKeyStore")
      keyStore.load(null)
      keyStore.getEntry(keyName, null) as KeyStore.PrivateKeyEntry
    } catch (e: Exception) {
      return null
    }
  }

  fun deleteKey(): Boolean {
    return try {
      val keyStore = KeyStore.getInstance("AndroidKeyStore")
      keyStore.load(null)
      keyStore.deleteEntry(keyName)
      true
    } catch (e: java.lang.Exception) {
      return false
    }
  }

  fun derivePublicKey(key: KeyStore.PrivateKeyEntry): String {
    val spec = X509EncodedKeySpec(key.certificate.publicKey.encoded)

    return Encoder.toBase64String(spec.encoded)
  }

  private fun createKeyPair(): String? {
    val provider = "AndroidKeyStore"
    val algorithm = KeyProperties.KEY_ALGORITHM_EC
    val digests = KeyProperties.DIGEST_SHA256
    val purposes = KeyProperties.PURPOSE_SIGN

    val params = KeyGenParameterSpec.Builder(keyName, purposes)
      .setDigests(digests)
      .build()

    return try {
      val generator = KeyPairGenerator.getInstance(algorithm, provider)

      generator.initialize(params)

      val keyPair = generator.generateKeyPair()

      Encoder.toBase64String(keyPair.public.encoded)
    } catch (e: java.lang.Exception) {
      null
    }
  }
}