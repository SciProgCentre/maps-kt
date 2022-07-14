package centre.sciprog.maps.compose

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.readBytes
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import mu.KotlinLogging
import org.jetbrains.skia.Image
import java.net.URL
import java.nio.file.Path
import kotlin.io.path.*
import kotlin.math.pow

/**
 * A [MapTileProvider] based on Open Street Map API. With in-memory and file cache
 */
public class OpenStreetMapTileProvider(
    private val scope: CoroutineScope,
    private val client: HttpClient,
    private val cacheDirectory: Path,
) : MapTileProvider {
    private val cache = HashMap<TileId, Deferred<ImageBitmap>>()

    private fun TileId.osmUrl() = URL("https://tile.openstreetmap.org/${zoom}/${i}/${j}.png")

    private fun TileId.cacheFilePath() = cacheDirectory.resolve("${zoom}/${i}/${j}.png")

    /**
     * Download and cache the tile image
     */
    private fun downloadImageAsync(id: TileId) = scope.async(Dispatchers.IO) {
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

    override fun loadTileAsync(id: TileId): Deferred<MapTile> {
        val indexRange = indexRange(id.zoom)
        if (id.i !in indexRange || id.j !in indexRange) {
            error("Indices (${id.i}, ${id.j}) are not in index range $indexRange for zoom ${id.zoom}")
        }

        val image = cache.getOrPut(id) {
            downloadImageAsync(id)
        }

        return scope.async {
            MapTile(id, image.await())
        }
    }

    companion object {
        private val logger = KotlinLogging.logger("OpenStreetMapCache")
        private fun indexRange(zoom: Int): IntRange = 0 until 2.0.pow(zoom).toInt()
    }
}