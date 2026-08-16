package com.stride.core.common

/** Collapsed from the WMO weather-code table Open-Meteo returns — see [fromWmoCode]. */
enum class WeatherCondition {
    CLEAR,
    PARTLY_CLOUDY,
    CLOUDY,
    FOG,
    RAIN,
    SNOW,
    THUNDERSTORM,
    ;

    companion object {
        /** https://open-meteo.com/en/docs — "WMO Weather interpretation codes" table. */
        fun fromWmoCode(code: Int): WeatherCondition = when (code) {
            0 -> CLEAR
            1, 2 -> PARTLY_CLOUDY
            3 -> CLOUDY
            45, 48 -> FOG
            in 51..67 -> RAIN
            in 71..77 -> SNOW
            in 80..82 -> RAIN
            85, 86 -> SNOW
            in 95..99 -> THUNDERSTORM
            else -> CLOUDY
        }
    }
}

data class WeatherSnapshot(
    val temperatureCelsius: Double,
    val humidityPercent: Int,
    val windSpeedKmh: Double,
    val condition: WeatherCondition,
    val isDay: Boolean,
) {
    val isHot: Boolean get() = temperatureCelsius >= 27.0
    val isCold: Boolean get() = temperatureCelsius <= 5.0
}
