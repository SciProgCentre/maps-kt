package space.kscience.maps.compose

import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.readBytes
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import org.jetbrains.skia.Image
import java.net.URL
import java.nio.file.Path
import kotlin.io.path.*

/**
 * A [MapTileProvider] based on Open Street Map API. With in-memory and file cache
 */
public class OpenStreetMapTileProvider(
    private val client: HttpClient,
    private val cacheDirectory: Path,
    parallelism: Int = 4,
    cacheCapacity: Int = 200,
    private val osmBaseUrl: String = "https://tile.openstreetmap.org",
) : MapTileProvider {
    private val semaphore = Semaphore(parallelism)
    private val cache = LruCache<TileId, Deferred<Image>>(cacheCapacity)

    private fun TileId.osmUrl() = URL("$osmBaseUrl/${zoom}/${i}/${j}.png")

    private fun TileId.cacheFilePath() = cacheDirectory.resolve("${zoom}/${i}/${j}.png")

    /**
     * Download and cache the tile image
     */
    private fun CoroutineScope.downloadImageAsync(id: TileId): Deferred<Image> = async(Dispatchers.IO) {

        id.cacheFilePath()?.let { path ->
            if (path.exists()) {
                try {
                    return@async Image.makeFromEncoded(path.readBytes())
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

            Image.makeFromEncoded(byteArray)
        }
    }

    override fun CoroutineScope.loadTileAsync(
        tileId: TileId,
    ): Deferred<MapTile> {

        //start image download
        val imageDeferred: Deferred<Image> = cache.getOrPut(tileId) {
            downloadImageAsync(tileId)
        }

        //collect the result asynchronously
        return async {
            val image: Image = runCatching { imageDeferred.await() }.onFailure {
                if(it !is CancellationException) {
                    logger.error(it) { "Failed to load tile image with id=$tileId" }
                }
                cache.remove(tileId)
            }.getOrThrow()

            MapTile(tileId, image)
        }
    }


    public companion object {
        private val logger = KotlinLogging.logger("OpenStreetMapCache")
    }
}