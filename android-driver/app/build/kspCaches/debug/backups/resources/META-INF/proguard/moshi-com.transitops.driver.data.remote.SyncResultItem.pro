-if class com.transitops.driver.data.remote.SyncResultItem
-keepnames class com.transitops.driver.data.remote.SyncResultItem
-if class com.transitops.driver.data.remote.SyncResultItem
-keep class com.transitops.driver.data.remote.SyncResultItemJsonAdapter {
    public <init>(com.squareup.moshi.Moshi);
}
