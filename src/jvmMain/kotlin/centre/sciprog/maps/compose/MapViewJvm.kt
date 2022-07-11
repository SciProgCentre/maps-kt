package centre.sciprog.maps.compose

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.input.pointer.pointerInput
import centre.sciprog.maps.*
import mu.KotlinLogging
import org.jetbrains.skia.Font
import org.jetbrains.skia.Paint
import kotlin.math.pow
import kotlin.math.roundToInt


private fun Color.toPaint(): Paint = Paint().apply {
    isAntiAlias = true
    color = toArgb()
}

private val logger = KotlinLogging.logger("MapView")

/**
 * A component that renders map and provides basic map manipulation capabilities
 */
@Composable
actual fun MapView(
    initialViewPoint: MapViewPoint,
    mapTileProvider: MapTileProvider,
    features: SnapshotStateMap<FeatureId, MapFeature>,
    onClick: (GeodeticMapCoordinates) -> Unit,
    modifier: Modifier,
) {
    var viewPoint by remember { mutableStateOf(initialViewPoint) }

    val zoom: Int by derivedStateOf { viewPoint.zoom.roundToInt() }

    val mapTiles = remember { mutableStateListOf<MapTile>() }

    //var mapRectangle by remember { mutableStateOf(initialRectangle) }
    var canvasSize by remember { mutableStateOf(Size(512f, 512f)) }

    val centerCoordinates by derivedStateOf { WebMercatorProjection.toMercator(viewPoint.focus, zoom) }

    // Load tiles asynchronously
    LaunchedEffect(viewPoint, canvasSize) {
        val left = centerCoordinates.x - canvasSize.width / 2
        val right = centerCoordinates.x + canvasSize.width / 2
        val horizontalIndices = mapTileProvider.toIndex(left)..mapTileProvider.toIndex(right)

        val top = (centerCoordinates.y + canvasSize.height / 2)
        val bottom = (centerCoordinates.y - canvasSize.height / 2)
        val verticalIndices = mapTileProvider.toIndex(bottom)..mapTileProvider.toIndex(top)

        mapTiles.clear()

        val indexRange = 0 until 2.0.pow(zoom).toInt()

        for (j in verticalIndices) {
            for (i in horizontalIndices) {
                if (i in indexRange && j in indexRange) {
                    val tileId = TileId(zoom, i, j)
                    try {
                        val tile = mapTileProvider.loadTile(tileId)
                        mapTiles.add(tile)
                    } catch (ex: Exception) {
                        logger.error(ex) { "Failed to load tile $tileId" }
                    }
                }
            }
        }

    }

    fun Offset.toMercator(): WebMercatorCoordinates = WebMercatorCoordinates(
        zoom,
        x + centerCoordinates.x - canvasSize.width / 2,
        y + centerCoordinates.y - canvasSize.height / 2,
    )

    fun Offset.toGeodetic() = WebMercatorProjection.toGeodetic(toMercator())

    fun WebMercatorCoordinates.toOffset(): Offset = Offset(
        (canvasSize.width / 2 - centerCoordinates.x + x).toFloat(),
        (canvasSize.height / 2 - centerCoordinates.y + y).toFloat()
    )

    fun GeodeticMapCoordinates.toOffset(): Offset = WebMercatorProjection.toMercator(this, zoom).toOffset()

    @OptIn(ExperimentalComposeUiApi::class)
    val canvasModifier = modifier.onPointerEvent(PointerEventType.Press) {
        onClick(it.changes.first().position.toGeodetic())
    }.onPointerEvent(PointerEventType.Scroll) {
        viewPoint = viewPoint.zoom(-it.changes.first().scrollDelta.y.toDouble())
    }.pointerInput(Unit) {
        detectDragGestures { _: PointerInputChange, dragAmount: Offset ->
            viewPoint = viewPoint.move(-dragAmount.x, +dragAmount.y)
        }
    }.fillMaxSize()

    fun DrawScope.drawFeature(zoom: Int, feature: MapFeature) {
        when (feature) {
            is MapFeatureSelector -> drawFeature(zoom, feature.selector(zoom))
            is MapCircleFeature -> drawCircle(
                feature.color,
                feature.size,
                center = feature.center.toOffset()
            )
            is MapLineFeature -> drawLine(feature.color, feature.a.toOffset(), feature.b.toOffset())
            is MapBitmapImageFeature -> drawImage(feature.image, feature.position.toOffset())
            is MapVectorImageFeature -> {
                val offset = feature.position.toOffset()
                translate(offset.x - feature.size.width / 2, offset.y - feature.size.height / 2) {
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


    Canvas(canvasModifier) {
        if (canvasSize != size) {
            canvasSize = size
            logger.debug { "Redraw canvas. Size: $size" }
        }
        clipRect {
            mapTiles.forEach { (id, image) ->
                //converting back from tile index to screen offset
                val offset = Offset(
                    (canvasSize.width / 2 - centerCoordinates.x + mapTileProvider.toCoordinate(id.i)).toFloat(),
                    (canvasSize.height / 2 - centerCoordinates.y + mapTileProvider.toCoordinate(id.j)).toFloat()
                )
                drawImage(
                    image = image,
                    topLeft = offset
                )
            }
            features.values.filter { zoom in it.zoomRange }.forEach { feature ->
                drawFeature(zoom, feature)
            }
        }
    }
}