package com.ananta.towers.data

import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.RequestBody
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*
import java.util.concurrent.TimeUnit

interface ApiService {
    @POST("register")
    suspend fun register(@Body body: RegisterRequest): Response<TokenResponse>

    @POST("login")
    suspend fun login(@Body request: LoginRequest): Response<TokenResponse>

    @GET("users")
    suspend fun getUsers(@Header("Authorization") token: String): Response<List<AppUser>>

    @POST("users")
    suspend fun createUser(@Header("Authorization") token: String, @Body body: CreateUserRequest): Response<Unit>

    @DELETE("users/{id}")
    suspend fun deleteUser(@Header("Authorization") token: String, @Path("id") id: String): Response<Unit>

    @GET("vehicles/{vehicleNumber}")
    suspend fun lookupVehicle(
        @Header("Authorization") token: String,
        @Path("vehicleNumber") vehicleNumber: String
    ): Response<Vehicle>

    @POST("vehicles")
    suspend fun registerVehicle(
        @Header("Authorization") token: String,
        @Body body: RequestBody
    ): Response<Vehicle>

    @POST("visitors")
    suspend fun createVisitor(@Header("Authorization") token: String, @Body visitor: Visitor): Response<Visitor>

    @POST("visitor-entries")
    suspend fun createVisitorEntry(
        @Header("Authorization") token: String,
        @Body body: RequestBody
    ): Response<VisitorEntry>

    @PATCH("visitor-entries/{id}/exit")
    suspend fun markExit(@Header("Authorization") token: String, @Path("id") id: String): Response<VisitorEntry>

    @GET("visitor-entries/active")
    suspend fun getActiveEntries(@Header("Authorization") token: String): Response<List<VisitorEntry>>

    @PATCH("vehicles/{id}")
    suspend fun updateVehicle(
        @Header("Authorization") token: String,
        @Path("id") id: String,
        @Body body: RequestBody
    ): Response<Vehicle>

    @DELETE("vehicles/{id}")
    suspend fun deleteVehicle(
        @Header("Authorization") token: String,
        @Path("id") id: String,
        @Query("rc_book_url") rcBookUrl: String? = null,
        @Query("rent_agreement_url") rentAgreementUrl: String? = null,
        @Query("vehicle_photo_url") vehiclePhotoUrl: String? = null
    ): Response<Unit>

    @GET("visitor-entries/vehicle/{vehicleNumber}")
    suspend fun getVehicleEntryHistory(
        @Header("Authorization") token: String,
        @Path("vehicleNumber") vehicleNumber: String
    ): Response<List<VisitorEntry>>

    @GET("history")
    suspend fun getHistory(@Header("Authorization") token: String): Response<List<VisitorEntry>>

    @GET("vehicles")
    suspend fun getVehicles(@Header("Authorization") token: String): Response<List<Vehicle>>
}

data class LoginRequest(val username: String, val pin: String)
data class RegisterRequest(val username: String, val pin: String, val name: String)
data class TokenResponse(val token: String, val role: String, val name: String)

object ApiClient {
    private const val BASE_URL = "https://ananta.healthymealspot.com/"

    private val logging = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val httpClient = OkHttpClient.Builder()
        .addInterceptor(logging)
        .addInterceptor { chain ->
            val request = chain.request().newBuilder()
                .header("User-Agent", "AnantaApp/1.0")
                .header("Accept", "application/json")
                .build()
            chain.proceed(request)
        }
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    val service: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(httpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
}
