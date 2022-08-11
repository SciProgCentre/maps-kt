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
import kotlin.math.*




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
    modifier: Modifier,
    mapViewState: MapViewState,
    mapViewConfig: MapViewConfig,
) {
    with(mapViewState) {
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
                                    //Use selection override if it is defined
                                    val gmcBox = GmcBox(
                                            rect.topLeft.toDpOffset().toGeodetic(),
                                            rect.bottomRight.toDpOffset().toGeodetic()
                                        )

                                    mapViewConfig.onSelect(gmcBox)
                                    if (mapViewConfig.zoomOnSelect) {
                                        val newViewPoint = gmcBox.computeViewPoint(mapTileProvider).invoke(canvasSize)

                                        mapViewConfig.onViewChange(newViewPoint)
                                        viewPointInternal = newViewPoint
                                    }
                                    selectRect = null
                                }
                            } else {
                                val dragStart = change.position
                                val dpPos = DpOffset(dragStart.x.toDp(), dragStart.y.toDp())
                                mapViewConfig.onClick(
                                    MapViewPoint(
                                         dpPos.toGeodetic() ,
                                        viewPoint.zoom
                                    )
                                )
                                drag(change.id) { dragChange ->
                                    val dragAmount = dragChange.position - dragChange.previousPosition
                                    val dpStart =
                                        DpOffset(
                                            dragChange.previousPosition.x.toDp(),
                                            dragChange.previousPosition.y.toDp()
                                        )
                                    val dpEnd = DpOffset(dragChange.position.x.toDp(), dragChange.position.y.toDp())
                                    if (!mapViewConfig.onDrag(
                                            this,
                                            MapViewPoint(dpStart.toGeodetic(), viewPoint.zoom),
                                            MapViewPoint(dpEnd.toGeodetic(), viewPoint.zoom)
                                        )
                                    ) return@drag
                                    val newViewPoint = viewPoint.move(
                                        -dragAmount.x.toDp().value / tileScale,
                                        +dragAmount.y.toDp().value / tileScale
                                    )
                                    mapViewConfig.onViewChange(newViewPoint)
                                    viewPointInternal = newViewPoint
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
            val newViewPoint = viewPoint.zoom(-change.scrollDelta.y.toDouble() * mapViewConfig.zoomSpeed, invariant)
            mapViewConfig.onViewChange(newViewPoint)
            viewPointInternal = newViewPoint
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
                        //start all
                        val deferred = loadTileAsync(id)
                        //wait asynchronously for it to finish
                        launch {
                            try {
                                mapTiles += deferred.await()
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
}