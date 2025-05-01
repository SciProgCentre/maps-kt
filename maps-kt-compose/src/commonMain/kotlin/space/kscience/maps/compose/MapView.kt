package space.kscience.maps.compose

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.jetbrains.skia.Image
import space.kscience.attributes.Attributes
import space.kscience.maps.coordinates.Gmc
import space.kscience.maps.features.*
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.uuid.ExperimentalUuidApi


private fun IntRange.intersect(other: IntRange) = max(first, other.first)..min(last, other.last)

private val logger = KotlinLogging.logger("MapView")

//private fun FeatureDrawScope<Gmc>.drawTiles(
//    tileProvider: MapTileProvider
//) {
//
//}


/**
 * A component that renders map and provides basic map manipulation capabilities
 */
@OptIn(ExperimentalUuidApi::class)
@Composable
public fun MapView(
    mapState: MapCanvasState,
    featureStore: FeatureStore<Gmc>,
    modifier: Modifier,
): Unit = with(mapState) {


    val tileFeatures by featureStore.featureFlow
        .map { it.values.filterIsInstance<TileFeature>() }
        .collectAsState(emptyList())

    val allTiles: Map<TileFeature, SnapshotStateMap<TileId, Image>> = remember(tileFeatures) {
        tileFeatures.associateWith {
            mutableStateMapOf()
        }
    }

    LaunchedEffect(viewPoint, canvasSize, tileFeatures) {
        allTiles.forEach { (tileFeature, tiles) ->
            // Load tiles asynchronously
            with(tileFeature.tileProvider) {
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
                        launch {
                            try {
                                val tile = loadTileAsync(id).await()
                                tiles[tile.id] = tile.image
                            } catch (ex: Exception) {
                                //displaying the error is maps responsibility
                                if (ex !is CancellationException) {
                                    logger.error(ex) { "Failed to load tile with id=$id" }
                                }
                            }
                        }
                        tiles.keys.filter {
                            it.zoom != intZoom || it.j !in verticalIndices || it.i !in horizontalIndices
                        }.forEach {
                            tiles.remove(it)
                        }
                    }
                }
            }
        }

    }


    FeatureCanvas(mapState, featureStore.featureFlow, modifier = modifier.canvasControls(mapState, featureStore)) {
        // draw custom features
        val tileScale = mapState.tileScale

        allTiles.forEach { (feature, tiles) ->
            val tileProvider = feature.tileProvider
            clipRect {
                val tileSize = IntSize(
                    ceil((tileProvider.tileSize.dp * tileScale).toPx()).toInt(),
                    ceil((tileProvider.tileSize.dp * tileScale).toPx()).toInt()
                )
                tiles.forEach { (id, image) ->
                    //converting back from tile index to screen offset
                    val offset = IntOffset(
                        (mapState.canvasSize.width / 2 + (tileProvider.toCoordinate(id.i).dp - mapState.centerCoordinates.x.dp) * tileScale).roundToPx(),
                        (mapState.canvasSize.height / 2 + (tileProvider.toCoordinate(id.j).dp - mapState.centerCoordinates.y.dp) * tileScale).roundToPx()
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
}

@Composable
public fun MapView(
    mapState: MapCanvasState,
    mapTileProvider: MapTileProvider,
    featureStore: FeatureStore<Gmc>,
    modifier: Modifier,
) {
    //FIXME this function modifies arguments
    featureStore.feature("map", TileFeature(mapState.space, mapTileProvider, Attributes.EMPTY))
    MapView(mapState, featureStore, modifier)
}

/**
 * Create a [MapView] with given [featureStore] group.
 */
@Composable
public fun MapView(
    mapTileProvider: MapTileProvider,
    config: ViewConfig<Gmc>,
    featureStore: FeatureStore<Gmc>,
    initialViewPoint: ViewPoint<Gmc>? = null,
    initialRectangle: Rectangle<Gmc>? = null,
    modifier: Modifier = Modifier.fillMaxSize(),
) {
    val mapState = MapCanvasState.remember(config, initialViewPoint, initialRectangle)
    MapView(mapState, mapTileProvider, featureStore, modifier)
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
    buildFeatures: FeatureStore<Gmc>.() -> Unit = {},
) {
    val featureState = FeatureStore.remember(WebMercatorSpace, buildFeatures)
    val computedRectangle = initialRectangle ?: featureState.getBoundingBox()
    MapView(mapTileProvider, config, featureState, initialViewPoint, computedRectangle, modifier)
}