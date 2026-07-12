-if class com.transitops.driver.data.remote.SyncResultResponse
-keepnames class com.transitops.driver.data.remote.SyncResultResponse
-if class com.transitops.driver.data.remote.SyncResultResponse
-keep class com.transitops.driver.data.remote.SyncResultResponseJsonAdapter {
    public <init>(com.squareup.moshi.Moshi);
}
