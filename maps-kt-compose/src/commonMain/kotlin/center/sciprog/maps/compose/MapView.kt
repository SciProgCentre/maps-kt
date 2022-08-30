package center.sciprog.maps.compose

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.unit.DpSize
import center.sciprog.maps.coordinates.*
import kotlin.math.PI
import kotlin.math.log2
import kotlin.math.min


public fun interface DragHandle {
    /**
     * @param event - qualifiers of the event used for drag
     * @param start - is a point where drag begins, end is a point where drag ends
     * @param end - end point of the drag
     *
     * @return true if default event processors should be used after this one
     */
    public fun drag(event: PointerEvent, start: MapViewPoint, end: MapViewPoint): Boolean

    public companion object {
        public val BYPASS: DragHandle = DragHandle { _, _, _ -> true }
    }
}

//TODO consider replacing by modifier
/**
 */
public data class MapViewConfig(
    val zoomSpeed: Double = 1.0 / 3.0,
    val inferViewBoxFromFeatures: Boolean = false,
    val onClick: MapViewPoint.(PointerEvent) -> Unit = {},
    val dragHandle: DragHandle = DragHandle.BYPASS,
    val onViewChange: MapViewPoint.() -> Unit = {},
    val onSelect: (GmcRectangle) -> Unit = {},
    val zoomOnSelect: Boolean = true,
    val resetViewPoint: Boolean = false,
)

@Composable
public expect fun MapView(
    mapTileProvider: MapTileProvider,
    computeViewPoint: (canvasSize: DpSize) -> MapViewPoint,
    features: Map<FeatureId, MapFeature>,
    config: MapViewConfig = MapViewConfig(),
    modifier: Modifier = Modifier.fillMaxSize(),
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
        mapTileProvider,
        { initialViewPoint },
        featuresBuilder.build(),
        config,
        modifier
    )
}

internal fun GmcRectangle.computeViewPoint(mapTileProvider: MapTileProvider): (canvasSize: DpSize) -> MapViewPoint =
    { canvasSize ->
        val zoom = log2(
            min(
                canvasSize.width.value / longitudeDelta.radians.value,
                canvasSize.height.value / latitudeDelta.radians.value
            ) * PI / mapTileProvider.tileSize
        )
        MapViewPoint(center, zoom)
    }

@Composable
public fun MapView(
    mapTileProvider: MapTileProvider,
    box: GmcRectangle,
    features: Map<FeatureId, MapFeature> = emptyMap(),
    config: MapViewConfig = MapViewConfig(),
    modifier: Modifier = Modifier.fillMaxSize(),
    buildFeatures: @Composable (MapFeatureBuilder.() -> Unit) = {},
) {
    val featuresBuilder = MapFeatureBuilderImpl(features)
    featuresBuilder.buildFeatures()
    MapView(
        mapTileProvider,
        box.computeViewPoint(mapTileProvider),
        featuresBuilder.build(),
        config,
        modifier
    )
}