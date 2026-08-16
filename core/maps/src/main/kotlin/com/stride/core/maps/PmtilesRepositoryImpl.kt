package com.stride.core.maps

import com.stride.core.common.DispatcherProvider
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject

/**
 * Config for where region extracts actually live. [baseUrl] is a placeholder — see
 * docs/foundation.md "Route mapping" hosting decision (Cloudflare R2 + the PMTiles Worker) —
 * because that infrastructure doesn't exist yet: no bucket has been created, no extracts have
 * been built or uploaded. Swapping this one value is the entire code-side change once it does;
 * everything else in this file is host-agnostic.
 */
data class PmtilesConfig(
    val baseUrl: String = "https://pmtiles.stride.app.example/regions",
)

class PmtilesRepositoryImpl @Inject constructor(
    private val cache: PmtilesCache,
    private val config: PmtilesConfig,
    private val dispatchers: DispatcherProvider,
) : PmtilesRepository {

    override fun regionFor(point: GeoPoint): MapRegion = com.stride.core.maps.regionFor(point)

    override fun isCached(region: MapRegion): Boolean = cache.isCached(region)

    override fun cachedFileFor(region: MapRegion): File? =
        cache.fileFor(region).takeIf { it.exists() && it.length() > 0 }

    override suspend fun downloadRegion(region: MapRegion): Result<File> = withContext(dispatchers.io) {
        runCatching {
            val url = "${config.baseUrl}/${region.fileName}"
            val connection = (URL(url).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 15_000
                readTimeout = 30_000
            }
            check(connection.responseCode == HttpURLConnection.HTTP_OK) {
                "Unexpected response ${connection.responseCode} fetching $url"
            }
            val target = cache.fileFor(region)
            // Download to a temp file first — a crash or connection drop mid-write must never
            // leave a truncated .pmtiles file behind masquerading as a valid cached region.
            val temp = File(target.parentFile, "${target.name}.part")
            connection.inputStream.use { input ->
                temp.outputStream().use { output -> input.copyTo(output) }
            }
            connection.disconnect()
            check(temp.length() > 0) { "Downloaded region file for ${region.id} was empty" }
            temp.renameTo(target)
            target
        }
    }
}
