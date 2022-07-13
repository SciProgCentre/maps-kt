package centre.sciprog.maps.compose

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
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
import androidx.compose.ui.unit.*
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
    features: Map<FeatureId, MapFeature>,
    onClick: (GeodeticMapCoordinates) -> Unit,
    config: MapViewConfig,
    modifier: Modifier,
) {

    var viewPoint by remember { mutableStateOf(initialViewPoint) }

    val zoom: Int by derivedStateOf { viewPoint.zoom.roundToInt() }

    val tileScale: Double by derivedStateOf { 2.0.pow(viewPoint.zoom - zoom) }

    val mapTiles = remember { mutableStateListOf<MapTile>() }

    //var mapRectangle by remember { mutableStateOf(initialRectangle) }
    var canvasSize by remember { mutableStateOf(DpSize(512.dp, 512.dp)) }

    val centerCoordinates by derivedStateOf { WebMercatorProjection.toMercator(viewPoint.focus, zoom) }

    fun DpOffset.toMercator(): WebMercatorCoordinates = WebMercatorCoordinates(
        zoom,
        (x - canvasSize.width / 2).value / tileScale + centerCoordinates.x,
        (y - canvasSize.height / 2).value / tileScale + centerCoordinates.y,
    )

    /*
     * Convert screen independent offset to GMC, adjusting for fractional zoom
     */
    fun DpOffset.toGeodetic() = WebMercatorProjection.toGeodetic(toMercator())

    @OptIn(ExperimentalComposeUiApi::class)
    val canvasModifier = modifier.onPointerEvent(PointerEventType.Press) {
        val (xPos, yPos) = it.changes.first().position
        onClick(DpOffset(xPos.toDp(), yPos.toDp()).toGeodetic())
    }.onPointerEvent(PointerEventType.Scroll) {
        val change = it.changes.first()
        val (xPos, yPos) = change.position
        //compute invariant point of translation
        val invariant = DpOffset(xPos.toDp(), yPos.toDp()).toGeodetic()
        viewPoint = viewPoint.zoom(-change.scrollDelta.y.toDouble() * config.zoomSpeed, invariant)
    }.pointerInput(Unit) {
        detectDragGestures { _: PointerInputChange, dragAmount: Offset ->
            viewPoint = viewPoint.move(-dragAmount.x.toDp().value / tileScale, +dragAmount.y.toDp().value / tileScale)
        }
    }.fillMaxSize()


    // Load tiles asynchronously
    LaunchedEffect(viewPoint, canvasSize) {
        val left = centerCoordinates.x - canvasSize.width.value / 2 / tileScale
        val right = centerCoordinates.x + canvasSize.width.value / 2 / tileScale
        val horizontalIndices = mapTileProvider.toIndex(left)..mapTileProvider.toIndex(right)

        val top = (centerCoordinates.y + canvasSize.height.value / 2 / tileScale)
        val bottom = (centerCoordinates.y - canvasSize.height.value / 2 / tileScale)
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

    // d
    Canvas(canvasModifier) {

        fun WebMercatorCoordinates.toOffset(): Offset = Offset(
            (canvasSize.width / 2 + (x.dp - centerCoordinates.x.dp) * tileScale.toFloat()).toPx(),
            (canvasSize.height / 2 + (y.dp - centerCoordinates.y.dp) * tileScale.toFloat()).toPx()
        )

        //Convert GMC to offset in pixels (not DP), adjusting for zoom
        fun GeodeticMapCoordinates.toOffset(): Offset = WebMercatorProjection.toMercator(this, zoom).toOffset()


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

        if (canvasSize != size.toDpSize()) {
            canvasSize = size.toDpSize()
            logger.debug { "Recalculate canvas. Size: $size" }
        }
        clipRect {
            val tileSize = IntSize(
                (mapTileProvider.tileSize.dp * tileScale.toFloat()).roundToPx(),
                (mapTileProvider.tileSize.dp * tileScale.toFloat()).roundToPx()
            )
            mapTiles.forEach { (id, image) ->
                //converting back from tile index to screen offset
                val offset = IntOffset(
                    (canvasSize.width / 2 + (mapTileProvider.toCoordinate(id.i).dp - centerCoordinates.x.dp) * tileScale.toFloat()).roundToPx(),
                    (canvasSize.height / 2 + (mapTileProvider.toCoordinate(id.j).dp - centerCoordinates.y.dp) * tileScale.toFloat()).roundToPx()
                )
                drawImage(
                    image = image,
                    dstOffset = offset,
                    dstSize = tileSize
                )
            }
            features.values.filter { zoom in it.zoomRange }.forEach { feature ->
                drawFeature(zoom, feature)
            }
        }
    }
}