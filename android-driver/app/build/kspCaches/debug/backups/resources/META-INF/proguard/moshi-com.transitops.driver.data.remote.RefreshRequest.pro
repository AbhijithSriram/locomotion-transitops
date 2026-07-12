-if class com.transitops.driver.data.remote.RefreshRequest
-keepnames class com.transitops.driver.data.remote.RefreshRequest
-if class com.transitops.driver.data.remote.RefreshRequest
-keep class com.transitops.driver.data.remote.RefreshRequestJsonAdapter {
    public <init>(com.squareup.moshi.Moshi);
}
