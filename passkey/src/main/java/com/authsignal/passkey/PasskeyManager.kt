package com.authsignal.passkey

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.credentials.*
import androidx.credentials.exceptions.*
import androidx.credentials.exceptions.publickeycredential.CreatePublicKeyCredentialDomException
import com.authsignal.passkey.models.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.decodeFromString

private const val TAG = "authsignal"

class PasskeyManager(context: Context, private val activity: Activity) {
  private val credentialManager = CredentialManager.create(context)

  suspend fun register(requestJson: String): PasskeyRegistrationCredential? {
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

      Json.decodeFromString<PasskeyRegistrationCredential>(responseJson)
    } catch (e : CreateCredentialException){
      handleCreateCredentialFailure(e)

      null
    }
  }

  suspend fun auth(requestJson: String): PasskeyAuthenticationCredential? {
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

      Json.decodeFromString<PasskeyAuthenticationCredential>(responseJson)
    } catch (e : GetCredentialException){
      Log.e(TAG, "getCredential failed with exception: " + e.message.toString())

      null
    }
  }

  private fun handleCreateCredentialFailure(e: CreateCredentialException) {
    when (e) {
      is CreatePublicKeyCredentialDomException -> {
        Log.w(TAG, "Error ${e.domError}")
        Log.w(TAG, "Error message ${e.message}")
      }
      is CreateCredentialCancellationException -> {
        Log.i(TAG, "Credential registration was cancelled by the user.")
      }
      else -> Log.w(TAG, "Unexpected exception type ${e::class.java.name}")
    }
  }
}