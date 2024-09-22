package com.authsignal.passkey

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.credentials.*
import androidx.credentials.exceptions.*
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
    } catch (e : CreateCredentialCancellationException){
      AuthsignalResponse(
        error = "The user canceled the passkey creation request.",
        errorType = "user_canceled"
      )
    } catch (e : CreateCredentialException){
      Log.e(TAG, "createCredential failed: ${e.message}")

      AuthsignalResponse(error = "Unexpected exception: ${e.message}")
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
    } catch(e: GetCredentialCancellationException) {
      AuthsignalResponse(
        error = "The user canceled the passkey authentication request.",
        errorType = "user_canceled"
      )
    } catch(e: NoCredentialException) {
      AuthsignalResponse(
        error = "No credential is available for the passkey authentication request.",
        errorType = "no_credential"
      )
    } catch (e : GetCredentialException){
      Log.e(TAG, "getCredential failed: ${e.message}")

      AuthsignalResponse(error = "Unexpected exception: ${e.message}")
    }
  }
}