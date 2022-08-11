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
    val newCoordinates = GeodeticMapCoordinates.ofRadians(
        (focus.latitude + deltaY / scaleFactor).coerceIn(
            -MercatorProjection.MAXIMUM_LATITUDE,
            MercatorProjection.MAXIMUM_LATITUDE
        ),
        focus.longitude + deltaX / scaleFactor
    )
    return MapViewPoint(newCoordinates, zoom)
}

private val logger = KotlinLogging.logger("MapView")

/**
 * A component that renders map and provides basic map manipulation capabilities
 */

@Composable
public actual fun MapView(
    mapViewState: MapViewState,
    modifier: Modifier,
) {
    @OptIn(ExperimentalComposeUiApi::class)
    val canvasModifier = modifier.pointerInput(Unit) {
        forEachGesture {
            awaitPointerEventScope {
                fun Offset.toDpOffset() = DpOffset(x.toDp(), y.toDp())

                val event: PointerEvent = awaitPointerEvent()
                event.changes.forEach { change ->
                    if (event.buttons.isPrimaryPressed) {
                        //Evaluating selection frame
                        if (event.keyboardModifiers.isShiftPressed) {
                            mapViewState.selectRect = Rect(change.position, change.position)
                            drag(change.id) { dragChange ->
                                mapViewState.selectRect?.let { rect ->
                                    val offset = dragChange.position
                                    mapViewState.selectRect = Rect(
                                        min(offset.x, rect.left),
                                        min(offset.y, rect.top),
                                        max(offset.x, rect.right),
                                        max(offset.y, rect.bottom)
                                    )
                                }
                            }
                            mapViewState.selectRect?.let { rect ->
                                //Use selection override if it is defined
                                val gmcBox = with(mapViewState) {
                                    GmcBox(
                                        rect.topLeft.toDpOffset().toGeodetic(),
                                        rect.bottomRight.toDpOffset().toGeodetic()
                                    )
                                }
                                mapViewState.config.onSelect(gmcBox)
                                if (mapViewState.config.zoomOnSelect) {
                                    val newViewPoint = gmcBox.computeViewPoint(mapViewState.mapTileProvider)
                                        .invoke(mapViewState.canvasSize)

                                    mapViewState.config.onViewChange(newViewPoint)
                                    mapViewState.viewPointInternal = newViewPoint
                                }
                                mapViewState.selectRect = null
                            }
                        } else {
                            val dragStart = change.position
                            val dpPos = DpOffset(dragStart.x.toDp(), dragStart.y.toDp())
                            mapViewState.config.onClick(
                                MapViewPoint(
                                    with(mapViewState) { dpPos.toGeodetic() },
                                    mapViewState.viewPoint.zoom
                                )
                            )
                            drag(change.id) { dragChange ->
                                val dragAmount = dragChange.position - dragChange.previousPosition
                                val newViewPoint = mapViewState.viewPoint.move(
                                    -dragAmount.x.toDp().value / mapViewState.tileScale,
                                    +dragAmount.y.toDp().value / mapViewState.tileScale
                                )
                                mapViewState.config.onViewChange(newViewPoint)
                                mapViewState.viewPointInternal = newViewPoint
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
        val invariant = with(mapViewState) { DpOffset(xPos.toDp(), yPos.toDp()).toGeodetic() }
        val newViewPoint =
            mapViewState.viewPoint.zoom(-change.scrollDelta.y.toDouble() * mapViewState.config.zoomSpeed, invariant)
        mapViewState.config.onViewChange(newViewPoint)
        mapViewState.viewPointInternal = newViewPoint
    }.fillMaxSize()


    // Load tiles asynchronously
    LaunchedEffect(mapViewState.viewPoint, mapViewState.canvasSize) {
        with(mapViewState.mapTileProvider) {
            val indexRange = 0 until 2.0.pow(mapViewState.zoom).toInt()

            val left =
                mapViewState.centerCoordinates.x - mapViewState.canvasSize.width.value / 2 / mapViewState.tileScale
            val right =
                mapViewState.centerCoordinates.x + mapViewState.canvasSize.width.value / 2 / mapViewState.tileScale
            val horizontalIndices: IntRange = (toIndex(left)..toIndex(right)).intersect(indexRange)

            val top =
                (mapViewState.centerCoordinates.y + mapViewState.canvasSize.height.value / 2 / mapViewState.tileScale)
            val bottom =
                (mapViewState.centerCoordinates.y - mapViewState.canvasSize.height.value / 2 / mapViewState.tileScale)
            val verticalIndices: IntRange = (toIndex(bottom)..toIndex(top)).intersect(indexRange)

            mapViewState.mapTiles.clear()

            for (j in verticalIndices) {
                for (i in horizontalIndices) {
                    val id = TileId(mapViewState.zoom, i, j)
                    //start all
                    val deferred = loadTileAsync(id)
                    //wait asynchronously for it to finish
                    launch {
                        try {
                            mapViewState.mapTiles += deferred.await()
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
    }


    Canvas(canvasModifier) {
        if (mapViewState.canvasSize != size.toDpSize()) {
            mapViewState.canvasSize = size.toDpSize()
            logger.debug { "Recalculate canvas. Size: $size" }
        }
        clipRect {
            val tileSize = IntSize(
                ceil((mapViewState.mapTileProvider.tileSize.dp * mapViewState.tileScale.toFloat()).toPx()).toInt(),
                ceil((mapViewState.mapTileProvider.tileSize.dp * mapViewState.tileScale.toFloat()).toPx()).toInt()
            )
            mapViewState.mapTiles.forEach { (id, image) ->
                //converting back from tile index to screen offset
                val offset = IntOffset(
                    (mapViewState.canvasSize.width / 2 + (mapViewState.mapTileProvider.toCoordinate(id.i).dp - mapViewState.centerCoordinates.x.dp) * mapViewState.tileScale.toFloat()).roundToPx(),
                    (mapViewState.canvasSize.height / 2 + (mapViewState.mapTileProvider.toCoordinate(id.j).dp - mapViewState.centerCoordinates.y.dp) * mapViewState.tileScale.toFloat()).roundToPx()
                )
                drawImage(
                    image = image,
                    dstOffset = offset,
                    dstSize = tileSize
                )
            }
            mapViewState.features.values.filter { mapViewState.zoom in it.zoomRange }.forEach { feature ->
                drawFeature(mapViewState.zoom, feature)
            }
        }
        mapViewState.selectRect?.let { rect ->
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
