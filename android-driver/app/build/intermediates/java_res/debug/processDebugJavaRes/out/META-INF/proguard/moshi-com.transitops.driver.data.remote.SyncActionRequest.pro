-if class com.transitops.driver.data.remote.SyncActionRequest
-keepnames class com.transitops.driver.data.remote.SyncActionRequest
-if class com.transitops.driver.data.remote.SyncActionRequest
-keep class com.transitops.driver.data.remote.SyncActionRequestJsonAdapter {
    public <init>(com.squareup.moshi.Moshi);
}
