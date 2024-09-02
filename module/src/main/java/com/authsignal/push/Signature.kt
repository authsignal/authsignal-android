package com.authsignal.push

import android.util.Log
import com.authsignal.Encoder
import java.nio.charset.StandardCharsets
import java.security.KeyStore.PrivateKeyEntry
import java.security.Signature

private const val TAG = "com.authsignal.push"

object Signer {
  fun sign(message: String, key: PrivateKeyEntry): String? {
    val msg: ByteArray = message.toByteArray(StandardCharsets.UTF_8)

    return try {
      val signer = Signature.getInstance("SHA256withECDSA")

      signer.initSign(key.privateKey)
      signer.update(msg)

      val signature = signer.sign()

      Encoder.toBase64String(signature)
    } catch (e: Exception) {
      Log.e(TAG, "Signature generation failed: $e")

      null
    }
  }
}