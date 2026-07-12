package com.transitops.driver.data.remote

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

/**
 * Retrofit interface representing the backend API contract.
 */
interface TransitOpsApi {

    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): Response<AuthResponse>

    @POST("auth/refresh")
    suspend fun refreshToken(@Body request: RefreshRequest): Response<AuthResponse>

    @GET("driver/me/active-trip")
    suspend fun getActiveTrip(): Response<ActiveTripResponse>

    @POST("sync/actions")
    suspend fun syncActions(@Body request: SyncActionRequest): Response<SyncResultResponse>
}
