package center.sciprog.maps.compose

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import center.sciprog.maps.coordinates.Gmc
import center.sciprog.maps.features.FeatureGroup
import center.sciprog.maps.features.PainterFeature
import center.sciprog.maps.features.drawFeature
import center.sciprog.maps.features.zoomRange
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import mu.KotlinLogging
import org.jetbrains.skia.Paint
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow


private fun Color.toPaint(): Paint = Paint().apply {
    isAntiAlias = true
    color = toArgb()
}

private fun IntRange.intersect(other: IntRange) = max(first, other.first)..min(last, other.last)

private val logger = KotlinLogging.logger("MapView")

/**
 * A component that renders map and provides basic map manipulation capabilities
 */
@Composable
public actual fun MapView(
    mapState: MapViewScope,
    featuresState: FeatureGroup<Gmc>,
    modifier: Modifier,
): Unit = with(mapState) {

    val mapTiles = remember(mapTileProvider) { mutableStateListOf<MapTile>() }

    // Load tiles asynchronously
    LaunchedEffect(viewPoint, canvasSize) {
        with(mapTileProvider) {
            val indexRange = 0 until 2.0.pow(intZoom).toInt()

            val left = centerCoordinates.x - canvasSize.width.value / 2 / tileScale
            val right = centerCoordinates.x + canvasSize.width.value / 2 / tileScale
            val horizontalIndices: IntRange = (toIndex(left)..toIndex(right)).intersect(indexRange)

            val top = (centerCoordinates.y + canvasSize.height.value / 2 / tileScale)
            val bottom = (centerCoordinates.y - canvasSize.height.value / 2 / tileScale)
            val verticalIndices: IntRange = (toIndex(bottom)..toIndex(top)).intersect(indexRange)

            mapTiles.clear()

            for (j in verticalIndices) {
                for (i in horizontalIndices) {
                    val id = TileId(intZoom, i, j)
                    //ensure that failed tiles do not fail the application
                    supervisorScope {
                        //start all
                        val deferred = loadTileAsync(id)
                        //wait asynchronously for it to finish
                        launch {
                            try {
                                mapTiles += deferred.await()
                            } catch (ex: Exception) {
                                //displaying the error is maps responsibility
                                logger.error(ex) { "Failed to load tile with id=$id" }
                            }
                        }
                    }

                }

            }
        }
    }

    val painterCache: Map<PainterFeature<Gmc>, Painter> = key(featuresState) {
        featuresState.features.filterIsInstance<PainterFeature<Gmc>>().associateWith { it.getPainter() }
    }


    Canvas(modifier = modifier.mapControls(mapState, featuresState.features).fillMaxSize()) {

        if (canvasSize != size.toDpSize()) {
            logger.debug { "Recalculate canvas. Size: $size" }
            config.onCanvasSizeChange(canvasSize)
            canvasSize = size.toDpSize()
        }

        clipRect {
            val tileSize = IntSize(
                ceil((mapTileProvider.tileSize.dp * tileScale).toPx()).toInt(),
                ceil((mapTileProvider.tileSize.dp * tileScale).toPx()).toInt()
            )
            mapTiles.forEach { (id, image) ->
                //converting back from tile index to screen offset
                val offset = IntOffset(
                    (canvasSize.width / 2 + (mapTileProvider.toCoordinate(id.i).dp - centerCoordinates.x.dp) * tileScale).roundToPx(),
                    (canvasSize.height / 2 + (mapTileProvider.toCoordinate(id.j).dp - centerCoordinates.y.dp) * tileScale).roundToPx()
                )
                drawImage(
                    image = image.toComposeImageBitmap(),
                    dstOffset = offset,
                    dstSize = tileSize
                )
            }

            featuresState.features
                .filter { viewPoint.zoom in it.zoomRange }
                .forEach { feature ->
                    drawFeature(mapState, painterCache, feature)
                }
        }

        selectRect?.let { dpRect ->
            val rect = dpRect.toRect()
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

