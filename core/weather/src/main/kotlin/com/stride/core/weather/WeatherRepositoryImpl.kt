package com.stride.core.weather

import com.stride.core.common.DispatcherProvider
import com.stride.core.common.WeatherCondition
import com.stride.core.common.WeatherSnapshot
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale
import javax.inject.Inject

class WeatherRepositoryImpl @Inject constructor(
    private val dispatchers: DispatcherProvider,
) : WeatherRepository {

    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun fetchCurrentConditions(latitude: Double, longitude: Double): WeatherSnapshot? =
        withContext(dispatchers.io) {
            runCatching {
                val url = buildString {
                    append("https://api.open-meteo.com/v1/forecast")
                    append("?latitude=").append(String.format(Locale.US, "%.4f", latitude))
                    append("&longitude=").append(String.format(Locale.US, "%.4f", longitude))
                    append("&current=temperature_2m,relative_humidity_2m,weather_code,wind_speed_10m,is_day")
                    append("&timezone=auto")
                }
                val connection = (URL(url).openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    connectTimeout = 8_000
                    readTimeout = 8_000
                }
                val body = connection.inputStream.bufferedReader().use { it.readText() }
                connection.disconnect()
                val response = json.decodeFromString<OpenMeteoResponse>(body)
                WeatherSnapshot(
                    temperatureCelsius = response.current.temperatureCelsius,
                    humidityPercent = response.current.relativeHumidityPercent,
                    windSpeedKmh = response.current.windSpeedKmh,
                    condition = WeatherCondition.fromWmoCode(response.current.weatherCode),
                    isDay = response.current.isDay == 1,
                )
            }.getOrNull() // network/parse failure should never block a runner from starting
        }
}
