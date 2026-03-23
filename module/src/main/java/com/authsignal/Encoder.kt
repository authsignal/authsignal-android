package com.authsignal

import android.util.Base64
import org.json.JSONObject

object Encoder {
  fun toBase64String(input: ByteArray): String {
    return String(Base64.encode(input, Base64.NO_WRAP))
  }

  fun getJwtClaim(token: String, claim: String): String? {
    val parts = token.split(".")
    if (parts.size != 3) return null

    return try {
      val payload = String(Base64.decode(parts[1], Base64.URL_SAFE or Base64.NO_WRAP))
      val json = JSONObject(payload)
      if (json.has(claim)) json.getString(claim)
      else if (json.has("other")) json.getJSONObject("other").optString(claim, null)
      else null
    } catch (_: Exception) {
      null
    }
  }
}