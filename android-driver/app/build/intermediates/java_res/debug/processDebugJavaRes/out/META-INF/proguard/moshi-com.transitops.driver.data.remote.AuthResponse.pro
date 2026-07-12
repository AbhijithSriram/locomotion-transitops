-if class com.transitops.driver.data.remote.AuthResponse
-keepnames class com.transitops.driver.data.remote.AuthResponse
-if class com.transitops.driver.data.remote.AuthResponse
-keep class com.transitops.driver.data.remote.AuthResponseJsonAdapter {
    public <init>(com.squareup.moshi.Moshi);
}
