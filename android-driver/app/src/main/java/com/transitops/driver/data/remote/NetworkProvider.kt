package com.transitops.driver.data.remote

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

object NetworkProvider {

    // Target the user's laptop IP Address where Spring Boot is running
    private const val BASE_URL = "http://192.168.1.8:8080/"

    // Moshi for JSON parsing
    private val moshi: Moshi by lazy {
        Moshi.Builder()
            .add(KotlinJsonAdapterFactory())
            .build()
    }

    // OkHttpClient with our FakeApiInterceptor for mocking incomplete endpoints
    private val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .addInterceptor(FakeApiInterceptor())
            .addInterceptor(AuthInterceptor { com.transitops.driver.data.auth.TokenProvider.token })
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .build()
    }

    // Retrofit Instance
    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
    }

    val api: TransitOpsApi by lazy {
        retrofit.create(TransitOpsApi::class.java)
    }
}
