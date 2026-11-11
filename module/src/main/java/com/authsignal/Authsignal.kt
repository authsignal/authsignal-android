package com.authsignal

import android.app.Activity
import android.content.Context
import com.authsignal.email.AuthsignalEmail
import com.authsignal.passkey.AuthsignalPasskey
import com.authsignal.push.AuthsignalPush
import com.authsignal.sms.AuthsignalSMS
import com.authsignal.totp.AuthsignalTOTP
import com.authsignal.qr.AuthsignalQRCode
import com.authsignal.inapp.AuthsignalInApp
import com.authsignal.whatsapp.AuthsignalWhatsApp

class Authsignal(
  tenantID: String,
  baseURL: String,
  activity: Activity? = null,
  context: Context? = null,
  deviceId: String? = null,
  retries: Int = 2,
) {
  val passkey = AuthsignalPasskey(tenantID = tenantID, baseURL = baseURL, activity = activity, deviceId = deviceId, retries = retries)
  val push = AuthsignalPush(tenantID = tenantID, baseURL = baseURL, context = context, retries = retries)
  val qr = AuthsignalQRCode(tenantID = tenantID, baseURL = baseURL, context = context, retries = retries)
  val inapp = AuthsignalInApp(tenantID = tenantID, baseURL = baseURL, context = context, retries = retries)
  val email = AuthsignalEmail(tenantID = tenantID, baseURL = baseURL, retries = retries)
  val sms = AuthsignalSMS(tenantID = tenantID, baseURL = baseURL, retries = retries)
  val whatsapp = AuthsignalWhatsApp(tenantID = tenantID, baseURL = baseURL, retries = retries)
  val totp = AuthsignalTOTP(tenantID = tenantID, baseURL = baseURL, retries = retries)

  init {
    val ctx = context ?: activity?.applicationContext
    if (ctx != null) {
      DeviceCache.shared.initialize(ctx, deviceId)
    }
  }

  fun setToken(token: String) {
    TokenCache.shared.token = token
  }

  fun clearToken() {
    TokenCache.shared.token = null
  }

  suspend fun getDeviceId(): String {
    return DeviceCache.shared.getDefaultDeviceId()
  }
}