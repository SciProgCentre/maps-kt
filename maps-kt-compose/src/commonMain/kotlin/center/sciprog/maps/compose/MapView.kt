package center.sciprog.maps.compose

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import center.sciprog.maps.coordinates.Gmc
import center.sciprog.maps.features.*
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import org.jetbrains.skia.Image
import kotlin.math.ceil
import kotlin.math.pow


private fun IntRange.intersect(other: IntRange) = kotlin.math.max(first, other.first)..kotlin.math.min(last, other.last)

private val logger = KotlinLogging.logger("MapView")

/**
 * A component that renders map and provides basic map manipulation capabilities
 */
@Composable
public fun MapView(
    mapState: MapCanvasState,
    mapTileProvider: MapTileProvider,
    features: FeatureGroup<Gmc>,
    modifier: Modifier,
) {
    val mapTiles = remember(mapTileProvider) {
        mutableStateMapOf<TileId, Image>()
    }

    with(mapState) {

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
                                    val tile = deferred.await()
                                    mapTiles[tile.id] = tile.image
                                } catch (ex: Exception) {
                                    //displaying the error is maps responsibility
                                    if (ex !is CancellationException) {
                                        logger.error(ex) { "Failed to load tile with id=$id" }
                                    }
                                }
                            }
                        }
                        mapTiles.keys.filter {
                            it.zoom != intZoom || it.j !in verticalIndices || it.i !in horizontalIndices
                        }.forEach {
                            mapTiles.remove(it)
                        }
                    }
                }
            }
        }
    }


    FeatureCanvas(mapState, features, modifier = modifier.canvasControls(mapState, features)) {
        val tileScale = mapState.tileScale

        clipRect {
            val tileSize = IntSize(
                ceil((mapTileProvider.tileSize.dp * tileScale).toPx()).toInt(),
                ceil((mapTileProvider.tileSize.dp * tileScale).toPx()).toInt()
            )
            mapTiles.forEach { (id, image) ->
                //converting back from tile index to screen offset
                val offset = IntOffset(
                    (mapState.canvasSize.width / 2 + (mapTileProvider.toCoordinate(id.i).dp - mapState.centerCoordinates.x.dp) * tileScale).roundToPx(),
                    (mapState.canvasSize.height / 2 + (mapTileProvider.toCoordinate(id.j).dp - mapState.centerCoordinates.y.dp) * tileScale).roundToPx()
                )
                drawImage(
                    image = image.toComposeImageBitmap(),
                    dstOffset = offset,
                    dstSize = tileSize
                )
            }
        }
    }
}

/**
 * Create a [MapView] with given [features] group.
 */
@Composable
public fun MapView(
    mapTileProvider: MapTileProvider,
    config: ViewConfig<Gmc>,
    features: FeatureGroup<Gmc>,
    initialViewPoint: ViewPoint<Gmc>? = null,
    initialRectangle: Rectangle<Gmc>? = null,
    modifier: Modifier,
) {
    val mapState = MapCanvasState.remember(mapTileProvider, config, initialViewPoint, initialRectangle)
    MapView(mapState, mapTileProvider, features, modifier)
}

/**
 * Draw a map using convenient parameters. If neither [initialViewPoint], noe [initialRectangle] is defined,
 * use map features to infer the view region.
 * @param initialViewPoint The view point of the map using center and zoom. Is used if provided
 * @param initialRectangle The rectangle to be used for view point computation. Used if [initialViewPoint] is not defined.
 * @param buildFeatures - a builder for features
 */
@Composable
public fun MapView(
    mapTileProvider: MapTileProvider,
    config: ViewConfig<Gmc> = ViewConfig(),
    initialViewPoint: ViewPoint<Gmc>? = null,
    initialRectangle: Rectangle<Gmc>? = null,
    modifier: Modifier = Modifier.fillMaxSize(),
    buildFeatures: FeatureGroup<Gmc>.() -> Unit = {},
) {
    val featureState = FeatureGroup.remember(WebMercatorSpace, buildFeatures)
    val computedRectangle = initialRectangle ?: featureState.getBoundingBox()
    MapView(mapTileProvider, config, featureState, initialViewPoint, computedRectangle, modifier)
}