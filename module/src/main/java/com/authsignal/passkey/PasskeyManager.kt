package com.authsignal.passkey

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.credentials.*
import androidx.credentials.exceptions.*
import androidx.credentials.exceptions.publickeycredential.CreatePublicKeyCredentialDomException
import com.authsignal.models.AuthsignalResponse
import com.authsignal.passkey.models.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.decodeFromString

private const val TAG = "com.authsignal.passkey"

class PasskeyManager(context: Context, private val activity: Activity) {
  private val credentialManager = CredentialManager.create(context)

  suspend fun register(requestJson: String): AuthsignalResponse<PasskeyRegistrationCredential> {
    val createPublicKeyCredentialRequest = CreatePublicKeyCredentialRequest(
      requestJson = requestJson,
      preferImmediatelyAvailableCredentials = false,
    )

    return try {
      val credentialResponse = credentialManager.createCredential(
        request = createPublicKeyCredentialRequest,
        activity = activity,
      ) as CreatePublicKeyCredentialResponse

      val responseJson = credentialResponse.registrationResponseJson

      val data = Json.decodeFromString<PasskeyRegistrationCredential>(responseJson)

      AuthsignalResponse(data = data)
    } catch (e : CreateCredentialException){
      val error = mapCreateCredentialFailure(e)

      Log.e(TAG, "createCredential failed: $error")

      AuthsignalResponse(error = error)
    }
  }

  suspend fun auth(requestJson: String): AuthsignalResponse<PasskeyAuthenticationCredential> {
    val getPublicKeyCredentialOption = GetPublicKeyCredentialOption(
      requestJson,
      null,
      true,
    )

    val getCredentialRequest = GetCredentialRequest(
      listOf(getPublicKeyCredentialOption)
    )

    return try {
      val credentialResponse = credentialManager.getCredential(
        request = getCredentialRequest,
        activity = activity,
      )

      val publicKeyCredential = credentialResponse.credential as PublicKeyCredential

      val responseJson = publicKeyCredential.authenticationResponseJson

      val data = Json.decodeFromString<PasskeyAuthenticationCredential>(responseJson)

      AuthsignalResponse(data = data)
    } catch (e : GetCredentialException){
      val error = mapGetCredentialFailure(e)
      val errorType = e.type

      Log.e(TAG, "getCredential failed: $error")

      AuthsignalResponse(error = error, errorType = errorType)
    }
  }

  private fun mapGetCredentialFailure(e: GetCredentialException): String {
    return when (e) {
      is GetCredentialCancellationException -> "user_cancelled_credential"
      else -> "unexpected_exception ${e::class.java.name}"
    }
  }

  private fun mapCreateCredentialFailure(e: CreateCredentialException): String {
    return when (e) {
      is CreateCredentialCancellationException -> "user_cancelled_credential"
      is CreatePublicKeyCredentialDomException -> "dom_exception: ${e.message}"
      else -> "unexpected_exception ${e::class.java.name}"
    }
  }
}