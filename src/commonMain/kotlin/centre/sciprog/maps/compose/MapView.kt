package centre.sciprog.maps.compose

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.DpSize
import centre.sciprog.maps.*
import kotlin.math.PI
import kotlin.math.log2
import kotlin.math.min


data class MapViewConfig(
    val zoomSpeed: Double = 1.0 / 3.0,
)

@Composable
expect fun MapView(
    mapTileProvider: MapTileProvider,
    computeViewPoint: (canvasSize: DpSize) -> MapViewPoint,
    features: Map<FeatureId, MapFeature>,
    onClick: (GeodeticMapCoordinates) -> Unit = {},
    //TODO consider replacing by modifier
    config: MapViewConfig = MapViewConfig(),
    modifier: Modifier = Modifier.fillMaxSize(),
)

@Composable
fun MapView(
    mapTileProvider: MapTileProvider,
    initialViewPoint: MapViewPoint,
    features: Map<FeatureId, MapFeature> = emptyMap(),
    onClick: (GeodeticMapCoordinates) -> Unit = {},
    config: MapViewConfig = MapViewConfig(),
    modifier: Modifier = Modifier.fillMaxSize(),
    buildFeatures: @Composable (FeatureBuilder.() -> Unit) = {},
) {
    val featuresBuilder = MapFeatureBuilder(features)
    featuresBuilder.buildFeatures()
    MapView(mapTileProvider, { initialViewPoint }, featuresBuilder.build(), onClick, config, modifier)
}

@Composable
fun MapView(
    mapTileProvider: MapTileProvider,
    box: GmcBox,
    features: Map<FeatureId, MapFeature> = emptyMap(),
    onClick: (GeodeticMapCoordinates) -> Unit = {},
    config: MapViewConfig = MapViewConfig(),
    modifier: Modifier = Modifier.fillMaxSize(),
    buildFeatures: @Composable (FeatureBuilder.() -> Unit) = {},
) {
    val featuresBuilder = MapFeatureBuilder(features)
    featuresBuilder.buildFeatures()
    val computeViewPoint: (canvasSize: DpSize) -> MapViewPoint = { canvasSize ->
        val zoom = log2(
            min(
                canvasSize.width.value / box.width,
                canvasSize.height.value / box.height
            ) * PI / mapTileProvider.tileSize
        )
        MapViewPoint(box.center, zoom)
    }
    MapView(mapTileProvider, computeViewPoint, featuresBuilder.build(), onClick, config, modifier)
}

/**
 * Create a MapView with initial [MapViewPoint] inferred from features
 *
 * @param defaultZoom the zoom, for which the bounding box is computed
 */
@Composable
fun MapViewWithFeatures(
    mapTileProvider: MapTileProvider,
    features: Map<FeatureId, MapFeature> = emptyMap(),
    onClick: (GeodeticMapCoordinates) -> Unit = {},
    config: MapViewConfig = MapViewConfig(),
    defaultZoom: Int = 1,
    modifier: Modifier = Modifier.fillMaxSize(),
    buildFeatures: @Composable (FeatureBuilder.() -> Unit),
) {
    val featuresBuilder = MapFeatureBuilder(features)
    featuresBuilder.buildFeatures()
    val featureSet = featuresBuilder.build()
    if(featureSet.isEmpty()) error("Can't create `MapViewWithFeatures` from empty feature set")
    val box: GmcBox = featureSet.values.map { it.getBoundingBox(defaultZoom) }.wrapAll()
    MapView(mapTileProvider, box, features, onClick, config, modifier, buildFeatures)
}