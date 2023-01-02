package center.sciprog.maps.compose

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import center.sciprog.maps.coordinates.Gmc
import center.sciprog.maps.features.*


@Composable
public expect fun MapView(
    mapState: MapViewScope,
    featuresState: FeatureGroup<Gmc>,
    modifier: Modifier = Modifier.fillMaxSize(),
)

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

    val featureState = remember(featureMap) {
        FeatureGroup.build(WebMercatorSpace) {
            featureMap.forEach { feature(it.key.id, it.value) }
        }
    }

    val mapState: MapViewScope = rememberMapState(
        mapTileProvider,
        config,
        initialViewPoint = initialViewPoint,
        initialRectangle = initialRectangle ?: featureState.features.computeBoundingBox(WebMercatorSpace, Float.MAX_VALUE),
    )

    MapView(mapState, featureState, modifier)
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
    buildFeatures: FeatureGroup<Gmc>.() -> Unit = {},
) {

    val featureState = FeatureGroup.remember(WebMercatorSpace, buildFeatures)

    val mapState: MapViewScope = rememberMapState(
        mapTileProvider,
        config,
        initialViewPoint = initialViewPoint,
        initialRectangle = initialRectangle ?: featureState.features.computeBoundingBox(WebMercatorSpace, Float.MAX_VALUE),
    )

    MapView(mapState, featureState, modifier)
}