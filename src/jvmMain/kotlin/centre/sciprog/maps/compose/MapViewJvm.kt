package centre.sciprog.maps.compose

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.gestures.forEachGesture
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.*
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.unit.*
import centre.sciprog.maps.*
import mu.KotlinLogging
import org.jetbrains.skia.Font
import org.jetbrains.skia.Paint
import kotlin.math.*


private fun Color.toPaint(): Paint = Paint().apply {
    isAntiAlias = true
    color = toArgb()
}

private val logger = KotlinLogging.logger("MapView")

/**
 * A component that renders map and provides basic map manipulation capabilities
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
actual fun MapView(
    mapTileProvider: MapTileProvider,
    computeViewPoint: (canvasSize: DpSize) -> MapViewPoint,
    features: Map<FeatureId, MapFeature>,
    onClick: (GeodeticMapCoordinates) -> Unit,
    config: MapViewConfig,
    modifier: Modifier,
) {
    var canvasSize by remember { mutableStateOf(DpSize(512.dp, 512.dp)) }

    var viewPointOverride by remember {
        mutableStateOf<MapViewPoint?>(
            if (config.inferViewBoxFromFeatures) {
                features.values.computeBoundingBox(1)?.let { box ->
                    val zoom = log2(
                        min(
                            canvasSize.width.value / box.width,
                            canvasSize.height.value / box.height
                        ) * PI / mapTileProvider.tileSize
                    )
                    MapViewPoint(box.center, zoom)
                }
            } else {
                null
            }
        )
    }

    val viewPoint by derivedStateOf { viewPointOverride ?: computeViewPoint(canvasSize) }

    val zoom: Int by derivedStateOf { floor(viewPoint.zoom).toInt() }

    val tileScale: Double by derivedStateOf { 2.0.pow(viewPoint.zoom - zoom) }

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

    val canvasModifier = modifier.pointerInput(Unit) {
        forEachGesture {
            awaitPointerEventScope {
                val event: PointerEvent = awaitPointerEvent()
                event.changes.forEach { change ->
                    if (event.buttons.isPrimaryPressed) {
                        //Evaluating selection frame
                        if (event.keyboardModifiers.isShiftPressed) {
                            selectRect = Rect(change.position, change.position)
                            drag(change.id) { dragChange ->
                                selectRect?.let { rect ->
                                    val offset = dragChange.position
                                    selectRect = Rect(
                                        min(offset.x, rect.left),
                                        min(offset.y, rect.top),
                                        max(offset.x, rect.right),
                                        max(offset.y, rect.bottom)
                                    )
                                }
                            }
                            selectRect?.let { rect ->
                                val (centerX, centerY) = rect.center
                                val centerGmc = DpOffset(centerX.toDp(), centerY.toDp()).toGeodetic()

                                val horizontalZoom: Float = log2(canvasSize.width.toPx() / rect.width)
                                val verticalZoom: Float = log2(canvasSize.height.toPx() / rect.height)


                                viewPointOverride = MapViewPoint(
                                    centerGmc,
                                    viewPoint.zoom + min(verticalZoom, horizontalZoom)
                                )
                                selectRect = null
                            }
                        } else {
                            val dragStart = change.position
                            val dpPos = DpOffset(dragStart.x.toDp(), dragStart.y.toDp())
                            onClick(dpPos.toGeodetic())
                            drag(change.id) { dragChange ->
                                val dragAmount = dragChange.position - dragChange.previousPosition
                                viewPointOverride = viewPoint.move(
                                    -dragAmount.x.toDp().value / tileScale,
                                    +dragAmount.y.toDp().value / tileScale
                                )
                            }
                        }
                    }
                }
            }
        }
    }.onPointerEvent(PointerEventType.Scroll) {
        val change = it.changes.first()
        val (xPos, yPos) = change.position
        //compute invariant point of translation
        val invariant = DpOffset(xPos.toDp(), yPos.toDp()).toGeodetic()
        viewPointOverride = viewPoint.zoom(-change.scrollDelta.y.toDouble() * config.zoomSpeed, invariant)
    }.fillMaxSize()


    // Load tiles asynchronously
    LaunchedEffect(viewPoint, canvasSize) {
        val indexRange = 0 until 2.0.pow(zoom).toInt()

        val left = centerCoordinates.x - canvasSize.width.value / 2 / tileScale
        val right = centerCoordinates.x + canvasSize.width.value / 2 / tileScale
        val horizontalIndices = mapTileProvider.toIndex(left)
            .rangeTo(mapTileProvider.toIndex(right))
            .intersect(indexRange)

        val top = (centerCoordinates.y + canvasSize.height.value / 2 / tileScale)
        val bottom = (centerCoordinates.y - canvasSize.height.value / 2 / tileScale)
        val verticalIndices = mapTileProvider.toIndex(bottom)
            .rangeTo(mapTileProvider.toIndex(top))
            .intersect(indexRange)

        mapTiles.clear()

        val tileIds = verticalIndices
            .flatMap { j ->
                horizontalIndices
                    .asSequence()
                    .map { TileId(zoom, it, j) }
            }

        mapTileProvider.loadTileAsync(
            tileIds = tileIds,
            scope = this
        ) { mapTiles += it }

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
