package centre.sciprog.maps.compose

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import centre.sciprog.maps.GeodeticMapCoordinates
import centre.sciprog.maps.MapViewPoint

@Composable
expect fun MapView(
    initialViewPoint: MapViewPoint,
    mapTileProvider: MapTileProvider,
    features: Map<FeatureId, MapFeature>,
    onClick: (GeodeticMapCoordinates) -> Unit = {},
    modifier: Modifier = Modifier.fillMaxSize(),
)

@Composable
fun MapView(
    initialViewPoint: MapViewPoint,
    mapTileProvider: MapTileProvider,
    onClick: (GeodeticMapCoordinates) -> Unit = {},
    modifier: Modifier = Modifier.fillMaxSize(),
    addFeatures: @Composable() (FeatureBuilder.() -> Unit) = {},
) {
    val featuresBuilder = MapFeatureBuilder()
    featuresBuilder.addFeatures()
    MapView(initialViewPoint, mapTileProvider, featuresBuilder.build(), onClick, modifier)
}