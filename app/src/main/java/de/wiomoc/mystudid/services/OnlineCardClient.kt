package de.wiomoc.mystudid.services

import com.google.gson.GsonBuilder
import com.google.gson.annotations.SerializedName
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.*
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*
import java.text.SimpleDateFormat
import java.util.*

object OnlineCardClient {

    val api: CardApi = Retrofit.Builder()
            .client(OkHttpClient.Builder().addInterceptor(HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            }).build())
            .baseUrl("https://cardservice-sws.cpwas.de/TL1/TLM/KASVC/")
            .addConverterFactory(GsonConverterFactory.create(
                    GsonBuilder()
                            .setDateFormat("dd.MM.yyyy HH:mm")
                            .create()
            ))
            .build()
            .create()

    var authToken: String? = null
    var cardNumber: String? = null

    val DATAFORMAT_JSON = "JSON"

    val dateQueryFormatter = SimpleDateFormat("dd.MM.yyyy")

    data class LoginCredentials(
        @SerializedName("BenutzerID") val cardNumber: String,
        @SerializedName("Passwort") val password: String
    )

    data class LoginResponse(@SerializedName("authToken") val authToken: String)

    data class Transaction(@SerializedName("transFullId") val id: String,
                           @SerializedName("zahlBetrag") val amount: Float,
                           @SerializedName("datum") val date: Date,
                           @SerializedName("ortName") val location: String)

    interface CardApi {
        @POST("LOGIN")
        @Headers("Authorization: Basic S0FTVkM6ekt2NXlFMUxaVW12VzI5SQ==")
        fun login(@Body loginCredentials: LoginCredentials,
                  @Query("karteNr") cardNumber: String = loginCredentials.cardNumber,
                  @Query("datenformat") dataFormat: String = DATAFORMAT_JSON,
                  @Query("format") format: String = DATAFORMAT_JSON): Call<Array<LoginResponse>>

        @GET("KARTE")
        @Headers("Authorization: Basic S0FTVkM6ekt2NXlFMUxaVW12VzI5SQ==")
        fun card(@Query("authToken") authToken: String = OnlineCardClient.authToken!!,
                 @Query("karteNr") cardNumber: String = OnlineCardClient.cardNumber!!,
                 @Query("format") format: String = DATAFORMAT_JSON): Call<Array<Transaction>>

        @GET("TRANS")
        @Headers("Authorization: Basic S0FTVkM6ekt2NXlFMUxaVW12VzI5SQ==")
        fun transactions(@Query("datumVon") dateFrom: String,
                         @Query("datumBis") dateUntil: String,
                         @Query("authToken") authToken: String = OnlineCardClient.authToken!!,
                         @Query("karteNr") cardNumber: String = OnlineCardClient.cardNumber!!,
                         @Query("_") timestamp: Long = System.currentTimeMillis(),
                         @Query("format") format: String = DATAFORMAT_JSON): Call<Array<Transaction>>
    }

    interface ResponseCallback<R> {
        fun onSuccess(response: R)
        fun onCredentialsRequired(cb: (loginCredentials: LoginCredentials) -> Unit)
        fun onFailure(t: Throwable)
    }

    fun login(responseCallback: ResponseCallback<LoginResponse>,
              loginCredentials: LoginCredentials? = PreferencesManager.loginCredentials) {
        if (loginCredentials == null) {
            responseCallback.onCredentialsRequired {
                login(responseCallback, it)
            }
        } else {
            api.login(loginCredentials).enqueue(object : Callback<Array<LoginResponse>> {
                override fun onFailure(call: Call<Array<LoginResponse>>, t: Throwable) {
                    responseCallback.onFailure(t)
                }

                override fun onResponse(call: Call<Array<LoginResponse>>, response: Response<Array<LoginResponse>>) {
                    when (response.code()) {
                        500 -> {
                            PreferencesManager.loginCredentials = null
                            responseCallback.onCredentialsRequired {
                                login(responseCallback, it)
                            }
                        }

                        else -> {
                            val loginResponse = response.body()!![0]
                            PreferencesManager.loginCredentials = loginCredentials
                            authToken = loginResponse.authToken
                            cardNumber = loginCredentials.cardNumber
                            responseCallback.onSuccess(loginResponse)
                        }

                    }
                }
            })
        }
    }

    fun transactions(
        responseCallback: ResponseCallback<Array<Transaction>>,
        dateFrom: Date, dateUntil: Date = Date()
    ) = ensureAuthToken(responseCallback) {
        api.transactions(dateQueryFormatter.format(dateFrom), dateQueryFormatter.format(dateUntil))
    }

    private fun <T> ensureAuthToken(responseCallback: ResponseCallback<T>,
                                    apiFunction: () -> Call<T>) {
        if (authToken == null) {
            login(object : ResponseCallback<LoginResponse> {
                override fun onSuccess(response: LoginResponse) {
                    ensureAuthToken(responseCallback, apiFunction)
                }

                override fun onCredentialsRequired(cb: (loginCredentials: LoginCredentials) -> Unit) {
                    responseCallback.onCredentialsRequired(cb)
                }

                override fun onFailure(t: Throwable) {
                    responseCallback.onFailure(t)
                }
            })
        } else {
            apiFunction().enqueue(object : Callback<T> {
                override fun onFailure(call: Call<T>, t: Throwable) {
                    responseCallback.onFailure(t)
                }

                override fun onResponse(call: Call<T>, response: Response<T>) {
                    when (response.code()) {
                        500 -> login(object : ResponseCallback<LoginResponse> {
                            override fun onSuccess(response: LoginResponse) {
                                ensureAuthToken(responseCallback, apiFunction)
                            }

                            override fun onCredentialsRequired(cb: (loginCredentials: LoginCredentials) -> Unit) {
                                responseCallback.onCredentialsRequired(cb)
                            }

                            override fun onFailure(t: Throwable) {
                                responseCallback.onFailure(t)
                            }

                        })
                        else -> responseCallback.onSuccess(response.body()!!)
                    }
                }
            })
        }
    }
}
