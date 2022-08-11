package center.sciprog.maps.compose

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.DpSize
import center.sciprog.maps.coordinates.*
import kotlin.math.PI
import kotlin.math.log2
import kotlin.math.min


//TODO consider replacing by modifier
/**
 * @param onDrag - returns true if you want to drag a map and false, if you want to make map stationary.
 *          start - is a point where drag begins, end is a point where drag ends
 */
public data class MapViewConfig(
    val zoomSpeed: Double = 1.0 / 3.0,
    val onClick: MapViewPoint.() -> Unit = {},
    val onDrag: Density.(start: MapViewPoint, end: MapViewPoint) -> Boolean = { _, _ -> true },
    val onViewChange: MapViewPoint.() -> Unit = {},
    val onSelect: (GmcBox) -> Unit = {},
    val zoomOnSelect: Boolean = true,
    val resetViewPoint: Boolean = false
)

@Composable
public expect fun MapView(
    modifier: Modifier = Modifier,
    mapViewState: MapViewState,
    mapViewConfig: MapViewConfig,
)

@Composable
public fun MapView(
    mapTileProvider: MapTileProvider,
    initialViewPoint: MapViewPoint,
    features: Map<FeatureId, MapFeature> = emptyMap(),
    config: MapViewConfig = MapViewConfig(),
    modifier: Modifier = Modifier.fillMaxSize(),
    buildFeatures: @Composable (MapFeatureBuilder.() -> Unit) = {},
) {
    val featuresBuilder = MapFeatureBuilderImpl(features)
    featuresBuilder.buildFeatures()
    MapView(
        mapViewState = MapViewState(
            mapTileProvider = mapTileProvider,
            initialViewPoint = { initialViewPoint },
            features = featuresBuilder.build(),
        ),
        mapViewConfig = config,
        modifier = modifier
    )
}

internal fun GmcBox.computeViewPoint(mapTileProvider: MapTileProvider): (canvasSize: DpSize) -> MapViewPoint =
    { canvasSize ->
        val zoom = log2(
            min(
                canvasSize.width.value / width,
                canvasSize.height.value / height
            ) * PI / mapTileProvider.tileSize
        )
        MapViewPoint(center, zoom)
    }

@Composable
public fun MapView(
    mapTileProvider: MapTileProvider,
    box: GmcBox,
    features: Map<FeatureId, MapFeature> = emptyMap(),
    config: MapViewConfig = MapViewConfig(),
    modifier: Modifier = Modifier.fillMaxSize(),
    buildFeatures: @Composable (MapFeatureBuilder.() -> Unit) = {},
) {
    val featuresBuilder = MapFeatureBuilderImpl(features)
    featuresBuilder.buildFeatures()
    MapView(
        mapViewState = MapViewState(
            mapTileProvider = mapTileProvider,
            features = featuresBuilder.build(),
            initialViewPoint = box.computeViewPoint(mapTileProvider),
        ),
        modifier = modifier,
        mapViewConfig = config,
    )
}