-if class com.transitops.driver.data.remote.ActiveTripResponse
-keepnames class com.transitops.driver.data.remote.ActiveTripResponse
-if class com.transitops.driver.data.remote.ActiveTripResponse
-keep class com.transitops.driver.data.remote.ActiveTripResponseJsonAdapter {
    public <init>(com.squareup.moshi.Moshi);
}
