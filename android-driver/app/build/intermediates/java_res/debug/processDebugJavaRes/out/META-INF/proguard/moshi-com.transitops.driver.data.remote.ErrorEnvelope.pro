-if class com.transitops.driver.data.remote.ErrorEnvelope
-keepnames class com.transitops.driver.data.remote.ErrorEnvelope
-if class com.transitops.driver.data.remote.ErrorEnvelope
-keep class com.transitops.driver.data.remote.ErrorEnvelopeJsonAdapter {
    public <init>(com.squareup.moshi.Moshi);
}
