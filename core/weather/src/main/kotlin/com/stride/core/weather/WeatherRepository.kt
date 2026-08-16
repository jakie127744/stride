package com.stride.core.weather

import com.stride.core.common.WeatherSnapshot

interface WeatherRepository {
    /**
     * Fetches current conditions for a coordinate via Open-Meteo — no API key, no account, no
     * per-request billing (see docs/foundation.md "Maps" for why that matters for a shipped
     * app). Returns null on any network/parse failure rather than throwing: a failed weather
     * fetch should never block a runner from starting their session.
     */
    suspend fun fetchCurrentConditions(latitude: Double, longitude: Double): WeatherSnapshot?
}
