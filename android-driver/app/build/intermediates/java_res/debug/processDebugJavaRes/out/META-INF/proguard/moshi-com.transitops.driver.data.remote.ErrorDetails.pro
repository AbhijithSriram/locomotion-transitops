-if class com.transitops.driver.data.remote.ErrorDetails
-keepnames class com.transitops.driver.data.remote.ErrorDetails
-if class com.transitops.driver.data.remote.ErrorDetails
-keep class com.transitops.driver.data.remote.ErrorDetailsJsonAdapter {
    public <init>(com.squareup.moshi.Moshi);
}
