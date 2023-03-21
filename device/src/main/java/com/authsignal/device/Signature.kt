package com.authsignal.device

import java.nio.charset.StandardCharsets
import java.security.KeyStore.PrivateKeyEntry
import java.security.Signature
import java.util.Base64

object Signer {
  fun sign(message: String, key: PrivateKeyEntry): String? {
    val msg: ByteArray = message.toByteArray(StandardCharsets.UTF_8)

    return try {
      val signer = Signature.getInstance("SHA256withECDSA")

      signer.initSign(key.privateKey)
      signer.update(msg)

      val signature: ByteArray = signer.sign()

      Base64.getEncoder().encodeToString(signature)
    } catch (e: Exception) {
      null
    }
  }
}