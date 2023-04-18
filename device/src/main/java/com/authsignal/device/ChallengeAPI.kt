package com.authsignal.device

import com.authsignal.device.models.Credential
import okhttp3.OkHttpClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST
import retrofit2.Retrofit


interface API {
  @Headers("Content-Type: application/json")
  @POST("users")
  fun getCredential(@Body body: CredentialRequest): Call<CredentialResponse>
}

class ChallengeAPI(baseURL: String) {
  private val client = OkHttpClient.Builder().build()

  private val retrofit = Retrofit.Builder()
    .baseUrl(baseURL)
    .addConverterFactory(GsonConverterFactory.create())
    .client(client)
    .build()

  fun getCredential(publicKey: String, onResult: (Credential?) -> Unit){
    val service = retrofit.create(API::class.java)
    val body = CredentialRequest(publicKey)

    service.getCredential(body).enqueue(
      object : Callback<CredentialResponse> {
        override fun onFailure(call: Call<CredentialResponse>, t: Throwable) {
          onResult(null)
        }
        override fun onResponse( call: Call<UserInfo>, response: Response<UserInfo>) {
          val addedUser = response.body()
          onResult(addedUser)
        }
      }
    )
  }
}

data class CredentialRequest(val publicKey: String)

data class CredentialResponse(
  val userAuthenticatorId: String,
  val verifiedAt: String,
  val lastVerifiedAt: String)