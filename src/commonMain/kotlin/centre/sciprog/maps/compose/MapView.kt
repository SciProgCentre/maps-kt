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
    features: Collection<MapFeature> = emptyList(),
    modifier: Modifier = Modifier.fillMaxSize(),
    onClick: (GeodeticMapCoordinates) -> Unit = {},
)