package centre.sciprog.maps.compose

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.input.pointer.pointerInput
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
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
import kotlin.math.*


private const val TILE_SIZE = 256


private data class OsMapTileId(
    val zoom: Int,
    val i: Int,
    val j: Int,
)

private data class OsMapTile(
    val id: OsMapTileId,
    val image: ImageBitmap,
)

private class OsMapCache(val scope: CoroutineScope, val client: HttpClient, private val cacheDirectory: Path? = null) {
    private val cache = HashMap<OsMapTileId, Deferred<ImageBitmap>>()

    private fun OsMapTileId.osmUrl() = URL("https://tile.openstreetmap.org/${zoom}/${i}/${j}.png")

    private fun OsMapTileId.cacheFilePath() = cacheDirectory?.resolve("${zoom}/${i}/${j}.png")

    private fun CoroutineScope.downloadImageAsync(id: OsMapTileId) = scope.async(Dispatchers.IO) {
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

    public suspend fun loadTile(id: OsMapTileId): OsMapTile {
        val image = cache.getOrPut(id) {
            scope.downloadImageAsync(id)
        }.await()

        return OsMapTile(id, image)
    }
}


private val logger = KotlinLogging.logger("MapView")

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun MapView(
    initialRectangle: MapRectangle,
    modifier: Modifier,
    client: HttpClient = remember { HttpClient(CIO) },
    cacheDirectory: Path? = null,
    initialZoom: Double? = null,
) {
    val scope = rememberCoroutineScope()
    val mapCache = remember { OsMapCache(scope, client, cacheDirectory) }
    val mapTiles = remember {  mutableStateListOf<OsMapTile>()}

    var mapRectangle by remember { mutableStateOf(initialRectangle) }
    var canvasSize by remember { mutableStateOf(Size(512f, 512f)) }

    //TODO provide override for tiling
    val numTilesHorizontal by derivedStateOf {
        ceil(canvasSize.width / TILE_SIZE).toInt()

    }
    val numTilesVertical by derivedStateOf {
        ceil(canvasSize.height / TILE_SIZE).toInt()
    }

    val zoom by derivedStateOf {
        val xOffsetUnscaled = mapRectangle.bottomRight.longitude - mapRectangle.topLeft.longitude
        val yOffsetUnscaled = ln(
            tan(PI / 4 + mapRectangle.topLeft.latitude / 2) / tan(PI / 4 + mapRectangle.bottomRight.latitude / 2)
        )

        initialZoom ?: ceil(
            log2(
                PI / max(
                    abs(xOffsetUnscaled / numTilesHorizontal),
                    abs(yOffsetUnscaled / numTilesVertical)
                )
            )
        )
    }

    val scaleFactor by derivedStateOf { WebMercatorProjection.scaleFactor(zoom) }

    val topLeft by derivedStateOf { with(WebMercatorProjection) { mapRectangle.topLeft.toMercator(zoom) } }

    LaunchedEffect(mapRectangle, canvasSize, zoom) {

        val startIndexHorizontal = (topLeft.x / TILE_SIZE).toInt()
        val startIndexVertical = (topLeft.y / TILE_SIZE).toInt()

        mapTiles.clear()

        for (j in 0 until numTilesVertical) {
            for (i in 0 until numTilesHorizontal) {
                val tileId = OsMapTileId(zoom.toInt(), startIndexHorizontal + i, startIndexVertical + j)
                val tile = mapCache.loadTile(tileId)
                mapTiles.add(tile)
            }
        }

    }

    var coordinates by remember { mutableStateOf<GeodeticMapCoordinates?>(null) }

    val canvasModifier = modifier.onPointerEvent(PointerEventType.Move) {
        val position = it.changes.first().position
        val screenCoordinates = TileWebMercatorCoordinates(
            zoom,
            position.x.toDouble() + topLeft.x,
            position.y.toDouble() + topLeft.y
        )
        coordinates = with(WebMercatorProjection) {
            screenCoordinates.toGeodetic()
        }
    }.onPointerEvent(PointerEventType.Press) {
        println(coordinates)
    }.pointerInput(Unit) {
        detectDragGestures { change: PointerInputChange, dragAmount: Offset ->
            mapRectangle = mapRectangle.move(dragAmount.y / scaleFactor, -dragAmount.x / scaleFactor)
        }
    }.fillMaxSize()


    Column {
        //Text(coordinates.toString())
        Canvas(canvasModifier) {
            if (canvasSize != size) {
                canvasSize = size
                logger.debug { "Redraw canvas. Size: $size" }
            }
            clipRect {
                mapTiles.forEach { (id, image) ->
                    //converting back from tile index to screen offset
                    logger.debug { "Drawing tile $id" }
                    val offset = Offset(
                        id.i.toFloat() * TILE_SIZE - topLeft.x.toFloat(),
                        id.j.toFloat() * TILE_SIZE - topLeft.y.toFloat()
                    )
                    drawImage(
                        image = image,
                        topLeft = offset
                    )
                }
            }
        }
    }
}