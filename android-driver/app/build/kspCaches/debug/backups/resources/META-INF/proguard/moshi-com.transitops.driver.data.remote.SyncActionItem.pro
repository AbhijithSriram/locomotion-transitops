-if class com.transitops.driver.data.remote.SyncActionItem
-keepnames class com.transitops.driver.data.remote.SyncActionItem
-if class com.transitops.driver.data.remote.SyncActionItem
-keep class com.transitops.driver.data.remote.SyncActionItemJsonAdapter {
    public <init>(com.squareup.moshi.Moshi);
}
