package com.authsignal

import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Log
import com.authsignal.models.AuthsignalResponse
import java.security.InvalidAlgorithmParameterException
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.spec.X509EncodedKeySpec

private const val TAG = "com.authsignal"
private const val keyTagPrefix = "authsignal_signing_key"

class KeyManager(keySuffix: String) {
  private val keyTag = "${keyTagPrefix}_${keySuffix}"

  fun getOrCreatePublicKey(
    userAuthenticationRequired: Boolean,
    timeout: Int,
    authorizationType: Int,
    username: String? = null
  ): AuthsignalResponse<String> {
    // When a username is provided, only reuse a key scoped to that user
    // Falling back to a shared key would bind the same credential to multiple users
    val publicKeyResponse = getPublicKey(username = username, includeFallbacks = username == null)

    if (publicKeyResponse.data != null) {
      return AuthsignalResponse(data = publicKeyResponse.data)
    }

    return createKey(
      userAuthenticationRequired,
      timeout,
      authorizationType,
      username,
    )
  }

  fun getKey(
    username: String? = null,
    includeFallbacks: Boolean = true,
  ): AuthsignalResponse<KeyStore.PrivateKeyEntry> {
    return try {
      val keyStore = KeyStore.getInstance("AndroidKeyStore")

      keyStore.load(null)

      val keyTags = if (includeFallbacks) {
        listOf(getUserKeyTag(username = username), keyTag, getLegacyKeyTag()).distinct()
      } else {
        listOf(getUserKeyTag(username = username))
      }

      val entry = keyTags.firstNotNullOfOrNull { keyStore.getEntry(it, null) }

      AuthsignalResponse(data = entry as KeyStore.PrivateKeyEntry)
    } catch (e: Exception) {
      AuthsignalResponse(data = null)
    }
  }

  fun getPublicKey(
    username: String? = null,
    includeFallbacks: Boolean = true,
  ): AuthsignalResponse<String> {
    val keyResponse = getKey(username = username, includeFallbacks = includeFallbacks)

    val key = keyResponse.data ?: return AuthsignalResponse(data = null)

    val publicKey = derivePublicKey(key)

    return AuthsignalResponse(data = publicKey)
  }

  fun derivePublicKey(key: KeyStore.PrivateKeyEntry): String {
    val spec = X509EncodedKeySpec(key.certificate.publicKey.encoded)

    return Encoder.toBase64String(spec.encoded)
  }

  fun deleteKey(username: String? = null): Boolean {
    return try {
      val keyStore = KeyStore.getInstance("AndroidKeyStore")

      keyStore.load(null)

      val userKeyTag = getUserKeyTag(username = username)

      val keyEntry = keyStore.getEntry(userKeyTag, null)

      if (keyEntry != null) {
        keyStore.deleteEntry(userKeyTag)
      }

      // Shared keys may be in use by other users so only delete them when no username is provided
      if (username == null) {
        val legacyKeyTag = getLegacyKeyTag()

        val legacyKeyEntry = keyStore.getEntry(legacyKeyTag, null)

        if (legacyKeyEntry != null) {
          keyStore.deleteEntry(legacyKeyTag)
        }
      }

      true
    } catch (e: java.lang.Exception) {
      Log.e(TAG, "deleteKey failed: ${e.message}")

      return false
    }
  }

  private fun createKey(
    userAuthenticationRequired: Boolean,
    timeout: Int,
    authorizationType: Int,
    username: String?,
  ): AuthsignalResponse<String> {
    val provider = "AndroidKeyStore"
    val algorithm = KeyProperties.KEY_ALGORITHM_EC
    val digests = KeyProperties.DIGEST_SHA256
    val purposes = KeyProperties.PURPOSE_SIGN

    val keyTag = getUserKeyTag(username = username)

    val paramsBuilder = KeyGenParameterSpec.Builder(keyTag, purposes)
      .setDigests(digests)
      .setUserAuthenticationRequired(userAuthenticationRequired)

    if (userAuthenticationRequired && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
      paramsBuilder.setUserAuthenticationParameters(timeout, authorizationType)
    }

    val params = paramsBuilder.build()

    return try {
      val generator = KeyPairGenerator.getInstance(algorithm, provider)

      generator.initialize(params)

      val keyPair = generator.generateKeyPair()

      val data = Encoder.toBase64String(keyPair.public.encoded)

      AuthsignalResponse(data = data)
    } catch (e : InvalidAlgorithmParameterException){
      Log.e(TAG, "createKeyPair failed: ${e.message}")

      AuthsignalResponse(error = e.message, errorCode = "invalid_algorithm_parameter")
    } catch (e : Exception){
      Log.e(TAG, "createKeyPair failed: ${e.message}")

      AuthsignalResponse(error = e.message, errorCode = "unknown_key_generation_error")
    }
  }

  private fun getUserKeyTag(username: String?): String {
    val cleanUsername = username?.trim()

    return cleanUsername?.let { "${keyTag}_$it" } ?: keyTag
  }

  private fun getLegacyKeyTag(): String {
    return keyTagPrefix
  }
}