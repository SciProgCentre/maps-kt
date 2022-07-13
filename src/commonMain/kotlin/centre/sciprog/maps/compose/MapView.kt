package centre.sciprog.maps.compose

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import centre.sciprog.maps.GeodeticMapCoordinates
import centre.sciprog.maps.MapViewPoint


data class MapViewConfig(
    val zoomSpeed: Double = 1.0 / 3.0,
)

@Composable
expect fun MapView(
    initialViewPoint: MapViewPoint,
    mapTileProvider: MapTileProvider,
    features: Map<FeatureId, MapFeature>,
    onClick: (GeodeticMapCoordinates) -> Unit = {},
    //TODO consider replacing by modifier
    config: MapViewConfig = MapViewConfig(),
    modifier: Modifier = Modifier.fillMaxSize(),
)

@Composable
fun MapView(
    initialViewPoint: MapViewPoint,
    mapTileProvider: MapTileProvider,
    onClick: (GeodeticMapCoordinates) -> Unit = {},
    config: MapViewConfig = MapViewConfig(),
    modifier: Modifier = Modifier.fillMaxSize(),
    addFeatures: @Composable() (FeatureBuilder.() -> Unit) = {},
) {
    val featuresBuilder = MapFeatureBuilder()
    featuresBuilder.addFeatures()
    MapView(initialViewPoint, mapTileProvider, featuresBuilder.build(), onClick, config, modifier)
}