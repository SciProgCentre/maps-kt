package center.sciprog.maps.compose

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.*
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import center.sciprog.maps.coordinates.GeodeticMapCoordinates
import center.sciprog.maps.coordinates.Gmc
import center.sciprog.maps.coordinates.radians
import center.sciprog.maps.coordinates.toFloat
import center.sciprog.maps.features.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import mu.KotlinLogging
import org.jetbrains.skia.Font
import org.jetbrains.skia.Paint
import kotlin.math.*


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
    mapTileProvider: MapTileProvider,
    initialViewPoint: MapViewPoint,
    featuresState: FeaturesState<Gmc>,
    config: ViewConfig<Gmc>,
    modifier: Modifier,
): Unit = key(initialViewPoint) {

    val state = rememberMapState(
        config,
        defaultCanvasSize,
        initialViewPoint,
        mapTileProvider.tileSize
    )
    val canvasModifier = modifier.mapControls(state).fillMaxSize()

    with(state) {

        val mapTiles = remember(mapTileProvider) { mutableStateListOf<MapTile>() }

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

        val painterCache = key(featuresState) {
            featuresState.features.values.filterIsInstance<VectorImageFeature<Gmc>>().associateWith { it.painter() }
        }

        Canvas(canvasModifier) {
            fun GeodeticMapCoordinates.toOffset(): Offset = toOffset(this@Canvas)

            fun DrawScope.drawFeature(zoom: Int, feature: MapFeature) {
                when (feature) {
                    is FeatureSelector -> drawFeature(zoom, feature.selector(zoom))
                    is CircleFeature -> drawCircle(
                        feature.color,
                        feature.size.toPx(),
                        center = feature.center.toOffset()
                    )

                    is RectangleFeature -> drawRect(
                        feature.color,
                        topLeft = feature.center.toOffset() - Offset(
                            feature.size.width.toPx() / 2,
                            feature.size.height.toPx() / 2
                        ),
                        size = feature.size.toSize()
                    )

                    is LineFeature -> drawLine(feature.color, feature.a.toOffset(), feature.b.toOffset())
                    is ArcFeature -> {
                        val topLeft = feature.oval.topLeft.toOffset()
                        val bottomRight = feature.oval.bottomRight.toOffset()

                        val size = Size(abs(topLeft.x - bottomRight.x), abs(topLeft.y - bottomRight.y))

                        drawArc(
                            color = feature.color,
                            startAngle = feature.startAngle.radians.degrees.toFloat(),
                            sweepAngle = feature.arcLength.radians.degrees.toFloat(),
                            useCenter = false,
                            topLeft = topLeft,
                            size = size,
                            style = Stroke()
                        )

                    }

                    is BitmapImageFeature -> drawImage(feature.image, feature.position.toOffset())

                    is VectorImageFeature -> {
                        val offset = feature.position.toOffset()
                        val size = feature.size.toSize()
                        translate(offset.x - size.width / 2, offset.y - size.height / 2) {
                            with(painterCache[feature]!!) {
                                draw(size)
                            }
                        }
                    }

                    is TextFeature -> drawIntoCanvas { canvas ->
                        val offset = feature.position.toOffset()
                        canvas.nativeCanvas.drawString(
                            feature.text,
                            offset.x + 5,
                            offset.y - 5,
                            Font().apply(feature.fontConfig),
                            feature.color.toPaint()
                        )
                    }

                    is DrawFeature -> {
                        val offset = feature.position.toOffset()
                        translate(offset.x, offset.y) {
                            feature.drawFeature(this)
                        }
                    }

                    is FeatureGroup -> {
                        feature.children.values.forEach {
                            drawFeature(zoom, it)
                        }
                    }

                    is PathFeature -> {
                        TODO("MapPathFeature not implemented")
//                    val offset = feature.rectangle.center.toOffset() - feature.targetRect.center
//                    translate(offset.x, offset.y) {
//                        sca
//                        drawPath(feature.path, brush = feature.brush, style = feature.style)
//                    }
                    }

                    is PointsFeature -> {
                        val points = feature.points.map { it.toOffset() }
                        drawPoints(
                            points = points,
                            color = feature.color,
                            strokeWidth = feature.stroke,
                            pointMode = feature.pointMode
                        )
                    }

//                else -> {
//                    logger.error { "Unrecognized feature type: ${feature::class}" }
//                }
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
                        image = image.toComposeImageBitmap(),
                        dstOffset = offset,
                        dstSize = tileSize
                    )
                }

                featuresState.features.values.filter { viewPoint.zoom in it.zoomRange }.forEach { feature ->
                    drawFeature(zoom, feature)
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
}
