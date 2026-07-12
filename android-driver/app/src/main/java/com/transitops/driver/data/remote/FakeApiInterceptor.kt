package com.transitops.driver.data.remote

import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import java.io.IOException

/**
 * An interceptor to mock backend responses during the hackathon.
 * Since the backend might not be ready, this allows us to test the entire Android UI and Sync flow!
 */
class FakeApiInterceptor : Interceptor {

    private val jsonMediaType = "application/json".toMediaType()

    @Throws(IOException::class)
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val path = request.url.encodedPath

        val responseJson = when {
            path.contains("/auth/login") -> {
                // Return a real network response by passing it down the OkHttp chain!
                // This means it will actually hit 192.168.1.8:8080/auth/login
                return chain.proceed(request)
            }
            path.contains("/driver/me/active-trip") -> {
                """
                {
                  "tripId": "trip-42",
                  "status": "DISPATCHED",
                  "source": { "name": "Chennai Depot", "lat": 13.0827, "lng": 80.2707 },
                  "destination": { "name": "Bangalore Hub", "lat": 12.9716, "lng": 77.5946 },
                  "cargoWeightKg": 1200,
                  "vehicle": { "id": "veh-5", "regNumber": "TN-01-AB-1234", "type": "VAN", "maxLoadKg": 2000, "odometer": 15000 },
                  "routePolyline": "gfo}EtohhU...",
                  "dispatchedAt": 1752300000000
                }
                """.trimIndent()
            }
            path.contains("/sync/actions") -> {
                // Return a real network response by passing it down the OkHttp chain!
                // This means it will actually hit 192.168.1.8:8080/sync/actions
                return chain.proceed(request)
            }
            else -> "{}"
        }

        return Response.Builder()
            .code(200)
            .message("OK")
            .request(request)
            .protocol(Protocol.HTTP_1_1)
            .body(responseJson.toResponseBody(jsonMediaType))
            .addHeader("content-type", "application/json")
            .build()
    }
}
