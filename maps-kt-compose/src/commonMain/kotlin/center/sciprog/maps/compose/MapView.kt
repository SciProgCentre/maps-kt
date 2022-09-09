package center.sciprog.maps.compose

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.isPrimaryPressed
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
    public fun handle(event: PointerEvent, start: MapViewPoint, end: MapViewPoint): Boolean

    public companion object {
        public val BYPASS: DragHandle = DragHandle { _, _, _ -> true }

        /**
         * Process only events with primary button pressed
         */
        public fun withPrimaryButton(
            block: (event: PointerEvent, start: MapViewPoint, end: MapViewPoint) -> Boolean,
        ): DragHandle = DragHandle { event, start, end ->
            if (event.buttons.isPrimaryPressed) {
                block(event, start, end)
            } else {
                false
            }
        }

        /**
         * Combine several handles into one
         */
        public fun combine(vararg handles: DragHandle): DragHandle = DragHandle { event, start, end ->
            handles.forEach {
                if (!it.handle(event, start, end)) return@DragHandle false
            }
            return@DragHandle true
        }
    }
}

//TODO consider replacing by modifier
/**
 */
public data class MapViewConfig(
    val zoomSpeed: Double = 1.0 / 3.0,
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

private fun prepareConfig(initialConfig: MapViewConfig, featureBuilder: MapFeatureBuilder): MapViewConfig {
    val draggableFeatureIds: Set<FeatureId> = featureBuilder.attributes().filterValues {
        it[DraggableAttribute] ?: false
    }.keys

    val features = featureBuilder.features

    val featureDrag = DragHandle.withPrimaryButton { _, start, end ->
        val zoom = start.zoom
        draggableFeatureIds.forEach { id ->
            val feature = features[id] as? DraggableMapFeature ?: return@forEach
            //val border = WebMercatorProjection.scaleFactor(zoom)
            val boundingBox = feature.getBoundingBox(zoom) ?: return@forEach
            if (start.focus in boundingBox) {
                features[id] = feature.withCoordinates(end.focus)
                return@withPrimaryButton false
            }
        }
        return@withPrimaryButton true
    }
    return initialConfig.copy(
        dragHandle = DragHandle.combine(featureDrag, initialConfig.dragHandle),
    )
}


internal fun GmcRectangle.computeViewPoint(
    mapTileProvider: MapTileProvider,
): (canvasSize: DpSize) -> MapViewPoint = { canvasSize ->
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
    initialViewPoint: MapViewPoint? = null,
    config: MapViewConfig = MapViewConfig(),
    modifier: Modifier = Modifier.fillMaxSize(),
    buildFeatures: @Composable (MapFeatureBuilder.() -> Unit) = {},
) {
    val featuresBuilder = MapFeatureBuilderImpl(mutableStateMapOf())
    featuresBuilder.buildFeatures()
    val features = remember { featuresBuilder.features }

    val newConfig = remember(features) {
        prepareConfig(config, featuresBuilder)
    }

    MapView(
        mapTileProvider,
        { canvasSize ->
            initialViewPoint ?: features.values.computeBoundingBox(1.0)?.let { box ->
                val zoom = log2(
                    min(
                        canvasSize.width.value / box.longitudeDelta.radians.value,
                        canvasSize.height.value / box.latitudeDelta.radians.value
                    ) * PI / mapTileProvider.tileSize
                )
                MapViewPoint(box.center, zoom)
            } ?: MapViewPoint(GeodeticMapCoordinates(0.0.radians, 0.0.radians), 1.0)
        },
        features,
        newConfig,
        modifier
    )
}

//
//@Composable
//public fun MapView(
//    mapTileProvider: MapTileProvider,
//    box: GmcRectangle,
//    config: MapViewConfig = MapViewConfig(),
//    modifier: Modifier = Modifier.fillMaxSize(),
//    buildFeatures: @Composable (MapFeatureBuilder.() -> Unit) = {},
//) {
//    val builder by derivedStateOf { MapFeatureBuilderImpl().apply(buildFeatures) }
//
//    MapView(
//        mapTileProvider,
//        box.computeViewPoint(mapTileProvider),
//        builder.features,
//        prepareConfig(config, builder),
//        modifier
//    )
//}