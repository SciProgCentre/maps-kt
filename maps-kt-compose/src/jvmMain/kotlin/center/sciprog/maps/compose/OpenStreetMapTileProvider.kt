package center.sciprog.maps.compose

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.readBytes
import io.ktor.utils.io.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
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

        //semaphore works only for actual download
        semaphore.withPermit {
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
    }

    override fun CoroutineScope.loadTileAsync(
        tileId: TileId,
    ): Deferred<MapTile> {

        //start image download
        val imageDeferred = cache.getOrPut(tileId) {
            downloadImageAsync(tileId)
        }

        //collect the result asynchronously
        return async {
            val image = try {
                imageDeferred.await()
            } catch (ex: Exception) {
                cache.remove(tileId)
                if(ex !is CancellationException) {
                    logger.error(ex) { "Failed to load tile image with id=$tileId" }
                }
                throw ex
            }
            MapTile(tileId, image)
        }
    }


    companion object {
        private val logger = KotlinLogging.logger("OpenStreetMapCache")
    }
}
