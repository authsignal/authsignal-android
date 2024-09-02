package com.authsignal.push

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Log
import com.authsignal.Encoder
import com.authsignal.models.AuthsignalResponse
import java.security.InvalidAlgorithmParameterException
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.spec.X509EncodedKeySpec

private const val TAG = "com.authsignal.push"
private const val keyName = "authsignal_signing_key"

object KeyManager {
  fun getOrCreatePublicKey(userAuthenticationRequired: Boolean): AuthsignalResponse<String> {
    val publicKey = getPublicKey()

    if (publicKey != null) {
      return AuthsignalResponse(data = publicKey)
    }

    return createKeyPair(userAuthenticationRequired)
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

  private fun createKeyPair(userAuthenticationRequired: Boolean): AuthsignalResponse<String> {
    val provider = "AndroidKeyStore"
    val algorithm = KeyProperties.KEY_ALGORITHM_EC
    val digests = KeyProperties.DIGEST_SHA256
    val purposes = KeyProperties.PURPOSE_SIGN

    val params = KeyGenParameterSpec.Builder(keyName, purposes)
      .setDigests(digests)
      .setUserAuthenticationRequired(userAuthenticationRequired)
      .build()

    return try {
      val generator = KeyPairGenerator.getInstance(algorithm, provider)

      generator.initialize(params)

      val keyPair = generator.generateKeyPair()

      val data = Encoder.toBase64String(keyPair.public.encoded)

      AuthsignalResponse(data = data)
    } catch (e : InvalidAlgorithmParameterException){
      Log.e(TAG, "createKeyPair failed: ${e.message}")

      AuthsignalResponse(error = e.message, errorType = "InvalidAlgorithmParameterException")
    } catch (e : Exception){
      Log.e(TAG, "createKeyPair failed: ${e.message}")

      AuthsignalResponse(error = e.message, errorType = "UnknownKeyGenerationException")
    }
  }
}