package center.sciprog.maps.compose

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import center.sciprog.maps.coordinates.Gmc
import center.sciprog.maps.features.*
import kotlin.math.PI
import kotlin.math.log2
import kotlin.math.min


@Composable
public expect fun MapView(
    mapTileProvider: MapTileProvider,
    initialViewPoint: MapViewPoint,
    featuresState: FeaturesState<Gmc>,
    config: ViewConfig<Gmc> = ViewConfig(),
    modifier: Modifier = Modifier.fillMaxSize(),
)

internal val defaultCanvasSize = DpSize(512.dp, 512.dp)

public fun Rectangle<Gmc>.computeViewPoint(
    mapTileProvider: MapTileProvider,
    canvasSize: DpSize = defaultCanvasSize,
): MapViewPoint {
    val zoom = log2(
        min(
            canvasSize.width.value / longitudeDelta.radians.value,
            canvasSize.height.value / latitudeDelta.radians.value
        ) * PI / mapTileProvider.tileSize
    )
    return MapViewPoint(center, zoom.toFloat())
}

/**
 * A builder for a Map with static features.
 */
@Composable
public fun MapView(
    mapTileProvider: MapTileProvider,
    initialViewPoint: MapViewPoint? = null,
    initialRectangle: Rectangle<Gmc>? = null,
    featureMap: Map<FeatureId<*>, MapFeature>,
    config: ViewConfig<Gmc> = ViewConfig(),
    modifier: Modifier = Modifier.fillMaxSize(),
) {
    val featuresState = key(featureMap) {
        FeaturesState.build(GmcCoordinateSpace) {
            featureMap.forEach { feature(it.key.id, it.value) }
        }
    }

    val viewPointOverride: MapViewPoint = remember(initialViewPoint, initialRectangle) {
        initialViewPoint
            ?: initialRectangle?.computeViewPoint(mapTileProvider)
            ?: featureMap.values.computeBoundingBox(GmcCoordinateSpace, 1f)?.computeViewPoint(mapTileProvider)
            ?: MapViewPoint.globe
    }

    MapView(mapTileProvider, viewPointOverride, featuresState, config, modifier)
}

/**
 * Draw a map using convenient parameters. If neither [initialViewPoint], noe [initialRectangle] is defined,
 * use map features to infer view region.
 * @param initialViewPoint The view point of the map using center and zoom. Is used if provided
 * @param initialRectangle The rectangle to be used for view point computation. Used if [initialViewPoint] is not defined.
 * @param buildFeatures - a builder for features
 */
@Composable
public fun MapView(
    mapTileProvider: MapTileProvider,
    initialViewPoint: MapViewPoint? = null,
    initialRectangle: Rectangle<Gmc>? = null,
    config: ViewConfig<Gmc> = ViewConfig(),
    modifier: Modifier = Modifier.fillMaxSize(),
    buildFeatures: FeaturesState<Gmc>.() -> Unit = {},
) {
    val featureState = FeaturesState.remember(GmcCoordinateSpace, buildFeatures)

    val viewPointOverride: MapViewPoint = remember(initialViewPoint, initialRectangle) {
        initialViewPoint
            ?: initialRectangle?.computeViewPoint(mapTileProvider)
            ?: featureState.features.values.computeBoundingBox(GmcCoordinateSpace,1f)?.computeViewPoint(mapTileProvider)
            ?: MapViewPoint.globe
    }

    val featureDrag: DragHandle<Gmc> = DragHandle.withPrimaryButton { event, start: ViewPoint<Gmc>, end: ViewPoint<Gmc> ->
        featureState.forEachWithAttribute(DraggableAttribute) { _, handle ->
            //TODO add safety
            handle as DragHandle<Gmc>
            if (!handle.handle(event, start, end)) return@withPrimaryButton false
        }
        true
    }


    val newConfig = config.copy(
        dragHandle = DragHandle.combine(featureDrag, config.dragHandle)
    )

    MapView(
        mapTileProvider = mapTileProvider,
        initialViewPoint = viewPointOverride,
        featuresState = featureState,
        config = newConfig,
        modifier = modifier,
    )
}