package com.authsignal.device

import android.util.Base64
import java.nio.charset.StandardCharsets
import java.security.KeyStore.PrivateKeyEntry
import java.security.Signature

object Signer {
  fun sign(message: String, key: PrivateKeyEntry): String? {
    val msg: ByteArray = message.toByteArray(StandardCharsets.UTF_8)

    return try {
      val signer = Signature.getInstance("SHA256withECDSA")

      signer.initSign(key.privateKey)
      signer.update(msg)

      val signature = signer.sign()

      Base64.encode(signature, Base64.DEFAULT).toString()
    } catch (e: Exception) {
      null
    }
  }
}