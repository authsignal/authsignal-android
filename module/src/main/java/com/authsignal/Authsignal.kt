package com.authsignal

import android.app.Activity
import com.authsignal.email.AuthsignalEmail
import com.authsignal.passkey.AuthsignalPasskey
import com.authsignal.push.AuthsignalPush
import com.authsignal.sms.AuthsignalSMS
import com.authsignal.totp.AuthsignalTOTP

class Authsignal(
  tenantID: String,
  baseURL: String,
  activity: Activity
) {
  val passkey = AuthsignalPasskey(tenantID = tenantID, baseURL = baseURL, activity = activity)
  val push = AuthsignalPush(tenantID = tenantID, baseURL = baseURL)
  val email = AuthsignalEmail(tenantID = tenantID, baseURL = baseURL)
  val sms = AuthsignalSMS(tenantID = tenantID, baseURL = baseURL)
  val totp = AuthsignalTOTP(tenantID = tenantID, baseURL = baseURL)

  fun setToken(token: String) {
    TokenCache.shared.token = token
  }
}