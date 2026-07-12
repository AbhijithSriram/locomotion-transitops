-if class com.transitops.driver.data.remote.VehicleDto
-keepnames class com.transitops.driver.data.remote.VehicleDto
-if class com.transitops.driver.data.remote.VehicleDto
-keep class com.transitops.driver.data.remote.VehicleDtoJsonAdapter {
    public <init>(com.squareup.moshi.Moshi);
}
