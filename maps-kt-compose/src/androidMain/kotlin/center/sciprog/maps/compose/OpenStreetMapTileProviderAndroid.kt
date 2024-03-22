package center.sciprog.maps.compose

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import android.util.LruCache
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import java.io.File
import java.net.URL
import kotlin.io.path.Path
import kotlin.io.path.createDirectories

public class AndroidTileImage(
    private val image: Bitmap,
) : TileImage {
    override fun toComposeImageBitmap(): ImageBitmap {
        return image.asImageBitmap()
    }
}

public class OpenStreetMapTileProviderAndroid(
    private val client: HttpClient,
    private val cacheDirectory: File,
    parallelism: Int = 4,
    cacheCapacity: Int = 200,
    private val osmBaseUrl: String = "https://tile.openstreetmap.org",
) : MapTileProvider {
    private val semaphore = Semaphore(parallelism)
    private val cache = LruCache<TileId, Deferred<Bitmap>>(cacheCapacity)
    private fun TileId.osmUrl() = URL("$osmBaseUrl/${zoom}/${i}/${j}.png")
    private fun TileId.cacheFilePath() = cacheDirectory.resolve("${zoom}/${i}/${j}.png")

    /**
     * Download and cache the tile image
     */
    private fun CoroutineScope.downloadImageAsync(id: TileId): Deferred<Bitmap> =
        async(Dispatchers.IO) {
            id.cacheFilePath().let { path ->
                if (path.exists()) {
                    try {
                        val blob = path.readBytes()
                        return@async BitmapFactory.decodeByteArray(blob, 0, blob.size)
                    } catch (ex: Exception) {
                        Log.d(TAG, "Failed to load image from $path")
                        path.delete()
                    }
                }
            }
            //semaphore works only for actual download
            semaphore.withPermit {
                val url = id.osmUrl()
                val byteArray = client.get(url).readBytes()
                Log.d(TAG, "Finished downloading map tile with id $id from $url")

                id.cacheFilePath().let { path ->
                    Log.d(TAG, "Caching map tile $id to $path")

                    path.parent?.let { Path(it).createDirectories() }
                    path.writeBytes(byteArray)
                }

                BitmapFactory.decodeByteArray(byteArray, 0, byteArray.size)
            }
        }

    override fun CoroutineScope.loadTileAsync(
        tileId: TileId,
    ): Deferred<MapTile> {

        //start image download
        val imageDeferred: Deferred<Bitmap> = cache.get(tileId) ?: run {
            val image = downloadImageAsync(tileId)
            cache.put(tileId, image) ?: image
        }

        //collect the result asynchronously
        return async {
            val image = runCatching { imageDeferred.await() }.onFailure {
                Log.e(TAG, "Failed to load tile image with id=$tileId", it)
                cache.remove(tileId).await()
            }.getOrThrow()

            MapTile(tileId, AndroidTileImage(image))
        }
    }

    private companion object {
        private const val TAG = "OSM_TILE_PROVIDER"
    }
}