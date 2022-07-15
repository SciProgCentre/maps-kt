package centre.sciprog.maps.compose

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import centre.sciprog.maps.LruCache
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.sync.Semaphore
import mu.KotlinLogging
import org.jetbrains.skia.Image
import java.net.URL
import java.nio.file.Path
import kotlin.io.path.*

/**
 * A [MapTileProvider] based on Open Street Map API. With in-memory and file cache
 */
class OpenStreetMapTileProvider(
    private val client: HttpClient,
    private val cacheDirectory: Path,
    parallelism: Int = 1,
    cacheCapacity: Int = 200,
) : MapTileProvider {
    private val semaphore = Semaphore(parallelism)
    private val cache = LruCache<TileId, Deferred<ImageBitmap>>(cacheCapacity)

    private fun TileId.osmUrl() = URL("https://tile.openstreetmap.org/${zoom}/${i}/${j}.png")

    private fun TileId.cacheFilePath() = cacheDirectory.resolve("${zoom}/${i}/${j}.png")

    /**
     * Download and cache the tile image
     */
    private fun CoroutineScope.downloadImageAsync(id: TileId) = async(Dispatchers.IO) {
        id.cacheFilePath()?.let { path ->
            if (path.exists()) {
                try {
                    return@async Image.makeFromEncoded(path.readBytes()).toComposeImageBitmap()
                } catch (ex: Exception) {
                    logger.debug { "Failed to load image from $path" }
                    path.deleteIfExists()
                }
            }
        }

        val url = id.osmUrl()
        val byteArray = client.get(url).readBytes()

        logger.debug { "Finished downloading map tile with id $id from $url" }

        id.cacheFilePath()?.let { path ->
            logger.debug { "Caching map tile $id to $path" }

            path.parent.createDirectories()
            path.writeBytes(byteArray)
        }

        Image.makeFromEncoded(byteArray).toComposeImageBitmap()
    }

    override suspend fun loadTileAsync(id: TileId, scope: CoroutineScope) = scope.async {
        semaphore.acquire()
        try {
            val image = cache.getOrPut(id) { downloadImageAsync(id) }
            MapTile(id, image.await())
        } catch (e: Exception) {
            cache.remove(id)
            throw e
        } finally {
            semaphore.release()
        }
    }

    companion object {
        private val logger = KotlinLogging.logger("OpenStreetMapCache")
    }
}
