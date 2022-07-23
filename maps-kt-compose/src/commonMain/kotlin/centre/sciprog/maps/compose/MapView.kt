package centre.sciprog.maps.compose

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.DpSize
import centre.sciprog.maps.*
import kotlin.math.PI
import kotlin.math.log2
import kotlin.math.min


//TODO consider replacing by modifier
data class MapViewConfig(
    val zoomSpeed: Double = 1.0 / 3.0,
    val inferViewBoxFromFeatures: Boolean = false,
    val onClick: MapViewPoint.() -> Unit = {},
    val onViewChange: MapViewPoint.() -> Unit = {},
    val onSelect: (GmcBox) -> Unit = {},
    val zoomOnSelect: Boolean = true
)

@Composable
expect fun MapView(
    mapTileProvider: MapTileProvider,
    computeViewPoint: (canvasSize: DpSize) -> MapViewPoint,
    features: Map<FeatureId, MapFeature>,
    config: MapViewConfig = MapViewConfig(),
    modifier: Modifier = Modifier.fillMaxSize(),
)

@Composable
fun MapView(
    mapTileProvider: MapTileProvider,
    initialViewPoint: MapViewPoint,
    features: Map<FeatureId, MapFeature> = emptyMap(),
    config: MapViewConfig = MapViewConfig(),
    modifier: Modifier = Modifier.fillMaxSize(),
    buildFeatures: @Composable (FeatureBuilder.() -> Unit) = {},
) {
    val featuresBuilder = MapFeatureBuilder(features)
    featuresBuilder.buildFeatures()
    MapView(
        mapTileProvider,
        { initialViewPoint },
        featuresBuilder.build(),
        config,
        modifier
    )
}

internal fun GmcBox.computeViewPoint(mapTileProvider: MapTileProvider): (canvasSize: DpSize) -> MapViewPoint = { canvasSize ->
    val zoom = log2(
        min(
            canvasSize.width.value / width,
            canvasSize.height.value / height
        ) * PI / mapTileProvider.tileSize
    )
    MapViewPoint(center, zoom)
}

@Composable
fun MapView(
    mapTileProvider: MapTileProvider,
    box: GmcBox,
    features: Map<FeatureId, MapFeature> = emptyMap(),
    config: MapViewConfig = MapViewConfig(),
    modifier: Modifier = Modifier.fillMaxSize(),
    buildFeatures: @Composable (FeatureBuilder.() -> Unit) = {},
) {
    val featuresBuilder = MapFeatureBuilder(features)
    featuresBuilder.buildFeatures()
    MapView(
        mapTileProvider,
        box.computeViewPoint(mapTileProvider),
        featuresBuilder.build(),
        config,
        modifier
    )
}