package center.sciprog.maps.compose

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.gestures.forEachGesture
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.*
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.unit.*
import center.sciprog.maps.coordinates.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import mu.KotlinLogging
import org.jetbrains.skia.Font
import org.jetbrains.skia.Paint
import kotlin.math.*


private fun Color.toPaint(): Paint = Paint().apply {
    isAntiAlias = true
    color = toArgb()
}

private fun IntRange.intersect(other: IntRange) = max(first, other.first)..min(last, other.last)

internal fun MapViewPoint.move(deltaX: Double, deltaY: Double): MapViewPoint {
    val newCoordinates = GeodeticMapCoordinates(
        (focus.latitude + (deltaY / scaleFactor).radians).coerceIn(
            -MercatorProjection.MAXIMUM_LATITUDE,
            MercatorProjection.MAXIMUM_LATITUDE
        ),
        focus.longitude + (deltaX / scaleFactor).radians
    )
    return MapViewPoint(newCoordinates, zoom)
}

private val logger = KotlinLogging.logger("MapView")

/**
 * A component that renders map and provides basic map manipulation capabilities
 */

@Composable
public actual fun MapView(
    mapTileProvider: MapTileProvider,
    initialViewPoint: MapViewPoint,
    features: Map<FeatureId, MapFeature>,
    config: MapViewConfig,
    modifier: Modifier,
) {
    var canvasSize by remember { mutableStateOf(DpSize(512.dp, 512.dp)) }

    var viewPointOverride by remember { mutableStateOf(initialViewPoint) }
    var viewPoint by remember { mutableStateOf(initialViewPoint) }

    if (viewPointOverride != initialViewPoint) {
        viewPoint = initialViewPoint
        viewPointOverride = initialViewPoint
    }

    fun setViewPoint(newViewPoint: MapViewPoint) {
        config.onViewChange(newViewPoint)
        viewPoint = newViewPoint
    }

    val zoom: Int by derivedStateOf {
        require(viewPoint.zoom in 1.0..18.0) { "Zoom value of ${viewPoint.zoom} is not valid" }
        floor(viewPoint.zoom).toInt()
    }

    val tileScale: Double by derivedStateOf {
        2.0.pow(viewPoint.zoom - zoom)
    }

    val mapTiles = remember { mutableStateListOf<MapTile>() }

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

    // Selection rectangle. If null - no selection
    var selectRect by remember { mutableStateOf<Rect?>(null) }

    @OptIn(ExperimentalComposeUiApi::class)
    val canvasModifier = modifier.pointerInput(Unit) {
        forEachGesture {
            awaitPointerEventScope {
                fun Offset.toDpOffset() = DpOffset(x.toDp(), y.toDp())

                val event: PointerEvent = awaitPointerEvent()

                event.changes.forEach { change ->
                    val dragStart = change.position
                    val dpPos = DpOffset(dragStart.x.toDp(), dragStart.y.toDp())

                    //start selection
                    if (event.buttons.isPrimaryPressed && event.keyboardModifiers.isShiftPressed) {
                        selectRect = Rect(change.position, change.position)
                    }

                    drag(change.id) { dragChange ->
                        val dragAmount = dragChange.position - dragChange.previousPosition
                        val dpStart = DpOffset(
                            dragChange.previousPosition.x.toDp(),
                            dragChange.previousPosition.y.toDp()
                        )
                        val dpEnd = DpOffset(dragChange.position.x.toDp(), dragChange.position.y.toDp())

                        //apply drag handle and check if it prohibits the drag even propagation
                        if (
                            !config.dragHandle.handle(
                                event,
                                MapViewPoint(dpStart.toGeodetic(), viewPoint.zoom),
                                MapViewPoint(dpEnd.toGeodetic(), viewPoint.zoom)
                            )
                        ) {
                            //clear selection just in case
                            selectRect = null
                            return@drag
                        }

                        if (event.buttons.isPrimaryPressed) {
                            //Evaluating selection frame
                            selectRect?.let { rect ->
                                val offset = dragChange.position
                                selectRect = Rect(
                                    min(offset.x, rect.left),
                                    min(offset.y, rect.top),
                                    max(offset.x, rect.right),
                                    max(offset.y, rect.bottom)
                                )
                                return@drag
                            }

                            config.onClick(MapViewPoint(dpPos.toGeodetic(), viewPoint.zoom), event)

                            setViewPoint(
                                viewPoint.move(
                                    -dragAmount.x.toDp().value / tileScale,
                                    +dragAmount.y.toDp().value / tileScale
                                )
                            )
                        }
                    }

                    // evaluate selection
                    selectRect?.let { rect ->
                        //Use selection override if it is defined
                        val gmcBox = GmcRectangle(
                            rect.topLeft.toDpOffset().toGeodetic(),
                            rect.bottomRight.toDpOffset().toGeodetic()
                        )
                        config.onSelect(gmcBox)
                        if (config.zoomOnSelect) {
                            setViewPoint(gmcBox.computeViewPoint(mapTileProvider, canvasSize))
                        }
                        selectRect = null
                    }
                }
            }
        }
    }.onPointerEvent(PointerEventType.Scroll) {
        val change = it.changes.first()
        val (xPos, yPos) = change.position
        //compute invariant point of translation
        val invariant = DpOffset(xPos.toDp(), yPos.toDp()).toGeodetic()
        setViewPoint(viewPoint.zoom(-change.scrollDelta.y.toDouble() * config.zoomSpeed, invariant))
    }.fillMaxSize()


    // Load tiles asynchronously
    LaunchedEffect(viewPoint, canvasSize) {
        with(mapTileProvider) {
            val indexRange = 0 until 2.0.pow(zoom).toInt()

            val left = centerCoordinates.x - canvasSize.width.value / 2 / tileScale
            val right = centerCoordinates.x + canvasSize.width.value / 2 / tileScale
            val horizontalIndices: IntRange = (toIndex(left)..toIndex(right)).intersect(indexRange)

            val top = (centerCoordinates.y + canvasSize.height.value / 2 / tileScale)
            val bottom = (centerCoordinates.y - canvasSize.height.value / 2 / tileScale)
            val verticalIndices: IntRange = (toIndex(bottom)..toIndex(top)).intersect(indexRange)

            mapTiles.clear()

            for (j in verticalIndices) {
                for (i in horizontalIndices) {
                    val id = TileId(zoom, i, j)
                    try {
                        //start all
                        val deferred = loadTileAsync(id)
                        //wait asynchronously for it to finish
                        launch {
                            mapTiles += deferred.await()
                        }
                    } catch (ex: Exception) {
                        if (ex !is CancellationException) {
                            //displaying the error is maps responsibility
                            logger.error(ex) { "Failed to load tile with id=$id" }
                        }
                    }
                }

            }
        }
    }


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

                is MapRectangleFeature -> drawRect(
                    feature.color,
                    topLeft = feature.center.toOffset() - Offset(
                        feature.size.width.toPx() / 2,
                        feature.size.height.toPx() / 2
                    ),
                    size = feature.size.toSize()
                )

                is MapLineFeature -> drawLine(feature.color, feature.a.toOffset(), feature.b.toOffset())
                is MapArcFeature -> {
                    val topLeft = feature.oval.topLeft.toOffset()
                    val bottomRight = feature.oval.bottomRight.toOffset()

                    val path = Path().apply {
                        addArcRad(
                            Rect(topLeft, bottomRight),
                            feature.startAngle.radians.value.toFloat(),
                            feature.arcLength.radians.value.toFloat()
                        )
                    }

                    drawPath(path, color = feature.color, style = Stroke())

                }

                is MapBitmapImageFeature -> drawImage(feature.image, feature.position.toOffset())
                is MapVectorImageFeature -> {
                    val offset = feature.position.toOffset()
                    val size = feature.size.toSize()
                    translate(offset.x - size.width / 2, offset.y - size.height / 2) {
                        with(feature.painter) {
                            draw(size)
                        }
                    }
                }

                is MapTextFeature -> drawIntoCanvas { canvas ->
                    val offset = feature.position.toOffset()
                    canvas.nativeCanvas.drawString(
                        feature.text,
                        offset.x + 5,
                        offset.y - 5,
                        Font().apply(feature.fontConfig),
                        feature.color.toPaint()
                    )
                }

                is MapDrawFeature -> {
                    val offset = feature.position.toOffset()
                    translate(offset.x, offset.y) {
                        feature.drawFeature(this)
                    }
                }

                is MapFeatureGroup -> {
                    feature.children.values.forEach {
                        drawFeature(zoom, it)
                    }
                }

                is MapPointsFeature -> {
                    val points = feature.points.map { it.toOffset() }
                    drawPoints(
                        points = points,
                        color = feature.color,
                        strokeWidth = feature.stroke,
                        pointMode = feature.pointMode
                    )
                }

                else -> {
                    logger.error { "Unrecognized feature type: ${feature::class}" }
                }
            }
        }

        if (canvasSize != size.toDpSize()) {
            logger.debug { "Recalculate canvas. Size: $size" }
            config.onCanvasSizeChange(canvasSize)
            canvasSize = size.toDpSize()
        }

        clipRect {
            val tileSize = IntSize(
                ceil((mapTileProvider.tileSize.dp * tileScale.toFloat()).toPx()).toInt(),
                ceil((mapTileProvider.tileSize.dp * tileScale.toFloat()).toPx()).toInt()
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
        selectRect?.let { rect ->
            drawRect(
                color = Color.Blue,
                topLeft = rect.topLeft,
                size = rect.size,
                alpha = 0.5f,
                style = Stroke(
                    width = 2f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                )
            )
        }
    }
}
