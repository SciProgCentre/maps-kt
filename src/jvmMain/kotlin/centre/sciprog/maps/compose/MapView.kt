package centre.sciprog.maps.compose

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent
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
import kotlin.math.*


private const val TILE_SIZE = 256


private data class OsMapTileId(
    val zoom: Int,
    val i: Int,
    val j: Int,
)

private fun OsMapTileId.osmUrl() = URL("https://tile.openstreetmap.org/${zoom.toInt()}/${i}/${j}.png")

private data class OsMapTile(
    val id: OsMapTileId,
    val image: ImageBitmap,
)

private class OsMapCache(val scope: CoroutineScope, val client: HttpClient) {
    private val cache = HashMap<OsMapTileId, Deferred<ImageBitmap>>()

    public suspend fun loadTile(id: OsMapTileId): OsMapTile {
        val image = cache.getOrPut(id) {
            scope.async(Dispatchers.IO) {
                val url = id.osmUrl()
                val byteArray = client.get(url).readBytes()

                logger.debug { "Finished downloading map tile with id $id from $url" }

                Image.makeFromEncoded(byteArray).toComposeImageBitmap()
            }
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
    initialZoom: Double? = null,
) {
    val mapRectangle by remember { mutableStateOf(initialRectangle) }

    val scope = rememberCoroutineScope()
    val mapCache = remember { OsMapCache(scope, client) }

    val mapTiles = mutableStateListOf<OsMapTile>()

    var canvasSize by remember { mutableStateOf(Size(512f,512f)) }

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

    //val scaleFactor = WebMercatorProjection.scaleFactor(computedZoom)

    val topLeft by derivedStateOf { with(WebMercatorProjection) { mapRectangle.topLeft.toMercator(zoom) } }

    LaunchedEffect(canvasSize, zoom) {

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
    }.fillMaxSize()
//        .pointerInput(Unit) {
//        forEachGesture {
//            awaitPointerEventScope {
//                val down = awaitFirstDown()
//                drag(down.id) {
//                    println(currentEvent.mouseEvent?.button)
//                }
//            }
//        }
//    }

    Column {
        //Text(coordinates.toString())
        Canvas(canvasModifier) {
            if(canvasSize!= size) {
                canvasSize = size
                logger.debug { "Redraw canvas. Size: $size" }
            }

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