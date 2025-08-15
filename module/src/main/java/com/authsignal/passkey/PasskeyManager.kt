package com.authsignal.passkey

import android.content.Context
import android.os.Build
import android.util.Log
import androidx.credentials.*
import androidx.credentials.exceptions.*
import androidx.credentials.exceptions.domerrors.DomError
import androidx.credentials.exceptions.domerrors.InvalidStateError
import androidx.credentials.exceptions.publickeycredential.*
import com.authsignal.models.AuthsignalResponse
import com.authsignal.passkey.models.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.decodeFromString

private const val TAG = "com.authsignal.passkey"

class PasskeyManager(private val context: Context?) {
  private val credentialManager: CredentialManager? =
    if (context != null)
      CredentialManager.create(context)
    else
      null

  private val json = Json { ignoreUnknownKeys = true }

  suspend fun register(
    requestJson: String,
    preferImmediatelyAvailableCredentials: Boolean
  ): AuthsignalResponse<PasskeyRegistrationCredential> {
    if (context == null || credentialManager == null) {
      return PasskeySdkErrors.contextError()
    }

    if (Build.VERSION.SDK_INT <= 28) {
      return AuthsignalResponse(
        error = "Passkey registration requires API version 28 or higher.",
        errorCode = "sdk_error"
      )
    }

    val request = CreatePublicKeyCredentialRequest(
      requestJson = requestJson,
      preferImmediatelyAvailableCredentials = preferImmediatelyAvailableCredentials,
    )

    return try {
      val response = credentialManager.createCredential(
        context = context,
        request = request,
      ) as CreatePublicKeyCredentialResponse

      val responseJson = response.registrationResponseJson

      val data = json.decodeFromString<PasskeyRegistrationCredential>(responseJson)

      AuthsignalResponse(data = data)
    } catch (e: CreateCredentialCancellationException) {
      AuthsignalResponse(
        error = "The user canceled the passkey creation request.",
        errorCode = "user_canceled"
      )
    } catch (e: CreatePublicKeyCredentialDomException) {
      if (e.domError is InvalidStateError) {
        AuthsignalResponse(
          error = e.message,
          errorCode = "invalid_state_error"
        )
      } else {
        AuthsignalResponse(
          error = e.message,
          errorCode = "dom_error"
        )
      }
    } catch (e: CreateCredentialException) {
      Log.e(TAG, "createCredential failed: ${e.message}")

      AuthsignalResponse(error = "Unexpected exception: ${e.message}")
    }
  }

  suspend fun auth(requestJson: String, preferImmediatelyAvailableCredentials: Boolean): AuthsignalResponse<PasskeyAuthenticationCredential> {
    if (context == null || credentialManager == null) {
      return PasskeySdkErrors.contextError()
    }

    val request = GetCredentialRequest(
      credentialOptions = listOf(GetPublicKeyCredentialOption(requestJson = requestJson)),
      preferImmediatelyAvailableCredentials = preferImmediatelyAvailableCredentials
    )

    return try {
      val response = credentialManager.getCredential(
        context = context,
        request = request,
      )

      val publicKeyCredential = response.credential as PublicKeyCredential

      val responseJson = publicKeyCredential.authenticationResponseJson

      val data = json.decodeFromString<PasskeyAuthenticationCredential>(responseJson)

      AuthsignalResponse(data = data)
    } catch(e: GetCredentialCancellationException) {
      AuthsignalResponse(
        error = "The user canceled the passkey authentication request.",
        errorCode = "user_canceled"
      )
    } catch(e: NoCredentialException) {
      AuthsignalResponse(
        error = "No credential is available for the passkey authentication request.",
        errorCode = "no_credential"
      )
    } catch (e : GetCredentialException) {
      Log.e(TAG, "getCredential failed: ${e.message}")

      AuthsignalResponse(error = "Unexpected exception: ${e.message}")
    }
  }
}