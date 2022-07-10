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
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.translate
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
import org.jetbrains.skia.Font
import org.jetbrains.skia.Image
import org.jetbrains.skia.Paint
import java.net.URL
import java.nio.file.Path
import kotlin.io.path.*
import kotlin.math.floor
import kotlin.math.pow


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

private fun Color.toPaint(): Paint = Paint().apply {
    isAntiAlias = true
    color = toArgb()
}

private fun Double.toIndex(): Int = floor(this / TILE_SIZE).toInt()
private fun Int.toCoordinate(): Double = (this * TILE_SIZE).toDouble()

private val logger = KotlinLogging.logger("MapView")

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun MapView(
    initialViewPoint: MapViewPoint,
    features: Collection<MapFeature> = emptyList(),
    modifier: Modifier = Modifier.fillMaxSize(),
    client: HttpClient = remember { HttpClient(CIO) },
    cacheDirectory: Path? = null,
) {
    var viewPoint by remember { mutableStateOf(initialViewPoint) }

    val scope = rememberCoroutineScope()
    val mapCache = remember { OsMapCache(scope, client, cacheDirectory) }
    val mapTiles = remember { mutableStateListOf<OsMapTile>() }

    //var mapRectangle by remember { mutableStateOf(initialRectangle) }
    var canvasSize by remember { mutableStateOf(Size(512f, 512f)) }

    val centerCoordinates by derivedStateOf { viewPoint.toMercator() }

    LaunchedEffect(viewPoint, canvasSize) {
        val z = viewPoint.zoom.toInt()
        val left = centerCoordinates.x - canvasSize.width / 2
        val right = centerCoordinates.x + canvasSize.width / 2
        val horizontalIndices = left.toIndex()..right.toIndex()

        val top = (centerCoordinates.y + canvasSize.height / 2)
        val bottom = (centerCoordinates.y - canvasSize.height / 2)
        val verticalIndices = bottom.toIndex()..top.toIndex()

        mapTiles.clear()

        val indexRange = 0 until 2.0.pow(z).toInt()

        for (j in verticalIndices) {
            for (i in horizontalIndices) {
                if (z == viewPoint.zoom.toInt() && i in indexRange && j in indexRange) {
                    val tileId = OsMapTileId(z, i, j)
                    val tile = mapCache.loadTile(tileId)
                    mapTiles.add(tile)
                }
            }
        }

    }

    fun Offset.toMercator(): WebMercatorCoordinates = WebMercatorCoordinates(
        viewPoint.zoom,
        x + centerCoordinates.x - canvasSize.width / 2,
        y + centerCoordinates.y - canvasSize.height / 2,
    )

    fun Offset.toGeodetic() = WebMercatorProjection.toGeodetic(toMercator())

    fun WebMercatorCoordinates.toOffset(): Offset = Offset(
        (canvasSize.width / 2 - centerCoordinates.x + x).toFloat(),
        (canvasSize.height / 2 - centerCoordinates.y + y).toFloat()
    )

    fun GeodeticMapCoordinates.toOffset(): Offset = WebMercatorProjection.toMercator(this, viewPoint.zoom).toOffset()

    var coordinates by remember { mutableStateOf<GeodeticMapCoordinates?>(null) }

    val canvasModifier = modifier.onPointerEvent(PointerEventType.Move) {
        val position = it.changes.first().position
        coordinates = position.toGeodetic()
    }.onPointerEvent(PointerEventType.Press) {
        println(coordinates)
    }.onPointerEvent(PointerEventType.Scroll) {
        viewPoint = viewPoint.zoom(-it.changes.first().scrollDelta.y.toDouble())
    }.pointerInput(Unit) {
        detectDragGestures { _: PointerInputChange, dragAmount: Offset ->
            viewPoint = viewPoint.move(-dragAmount.x, +dragAmount.y)
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
                        (canvasSize.width / 2 - centerCoordinates.x + id.i.toCoordinate()).toFloat(),
                        (canvasSize.height / 2 - centerCoordinates.y + id.j.toCoordinate()).toFloat()
                    )
                    drawImage(
                        image = image,
                        topLeft = offset
                    )
                }
                features.filter { viewPoint.zoom in it.zoomRange }.forEach { feature ->
                    when (feature) {
                        is MapCircleFeature -> drawCircle(
                            feature.color,
                            feature.size,
                            center = feature.center.toOffset()
                        )
                        is MapLineFeature -> drawLine(feature.color, feature.a.toOffset(), feature.b.toOffset())
                        is MapBitmapImageFeature -> drawImage(feature.image, feature.position.toOffset())
                        is MapVectorImageFeature -> {
                            val offset = feature.position.toOffset()
                            translate(offset.x - feature.size.width/2, offset.y - feature.size.height/2) {
                                with(feature.painter) {
                                    draw(feature.size)
                                }
                            }
                        }
                        is MapTextFeature -> drawIntoCanvas { canvas ->
                            val offset = feature.position.toOffset()
                            canvas.nativeCanvas.drawString(
                                feature.text,
                                offset.x + 5,
                                offset.y - 5,
                                Font().apply { size = 16f },
                                feature.color.toPaint()
                            )
                        }

                    }
                }
            }
        }
    }
}