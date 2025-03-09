package center.sciprog.maps.compose

import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.readBytes
import io.ktor.http.Url
import io.ktor.util.decodeBase64Bytes
import io.ktor.util.encodeBase64
import kotlinx.browser.window
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import org.jetbrains.skia.Image
import org.w3c.dom.Storage

/**
 * A [MapTileProvider] based on Open Street Map API. With in-memory and file cache
 */
public class OpenStreetMapTileProvider(
    private val client: HttpClient,
    private val storage: Storage = window.localStorage,
    parallelism: Int = 4,
    cacheCapacity: Int = 200,
    private val osmBaseUrl: String = "https://tile.openstreetmap.org",
) : MapTileProvider {
    private val semaphore = Semaphore(parallelism)
    private val cache = LruCache<TileId, Deferred<Image>>(cacheCapacity)

    private fun TileId.osmUrl() = Url("$osmBaseUrl/${zoom}/${i}/${j}.png")

    private fun TileId.imageName() = "${zoom}/${i}/${j}.png"

    private fun TileId.readImage() = storage.getItem(imageName())

    /**
     * Download and cache the tile image
     */
    private fun CoroutineScope.downloadImageAsync(id: TileId): Deferred<Image> = async {

        id.readImage()?.let { imageString ->
            try {
                return@async Image.makeFromEncoded(imageString.decodeBase64Bytes())
            } catch (ex: Exception) {
                logger.debug { "Failed to load image from $imageString" }
                storage.removeItem(id.imageName())
            }
        }

        //semaphore works only for actual download
        semaphore.withPermit {
            val url = id.osmUrl()
            val byteArray = client.get(url).readBytes()
            logger.debug { "Finished downloading map tile with id $id from $url" }
            val imageName = id.imageName()
            logger.debug { "Caching map tile $id to $imageName" }
            storage.setItem(imageName, byteArray.encodeBase64())
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
                if (it !is CancellationException) {
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