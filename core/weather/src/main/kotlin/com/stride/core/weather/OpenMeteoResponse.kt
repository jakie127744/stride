package com.stride.core.weather

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class OpenMeteoResponse(
    @SerialName("current") val current: CurrentWeather,
) {
    @Serializable
    data class CurrentWeather(
        @SerialName("temperature_2m") val temperatureCelsius: Double,
        @SerialName("relative_humidity_2m") val relativeHumidityPercent: Int,
        @SerialName("weather_code") val weatherCode: Int,
        @SerialName("wind_speed_10m") val windSpeedKmh: Double,
        @SerialName("is_day") val isDay: Int,
    )
}
