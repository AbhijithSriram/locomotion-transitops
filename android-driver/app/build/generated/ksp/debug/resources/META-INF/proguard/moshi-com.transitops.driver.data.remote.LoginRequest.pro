-if class com.transitops.driver.data.remote.LoginRequest
-keepnames class com.transitops.driver.data.remote.LoginRequest
-if class com.transitops.driver.data.remote.LoginRequest
-keep class com.transitops.driver.data.remote.LoginRequestJsonAdapter {
    public <init>(com.squareup.moshi.Moshi);
}
