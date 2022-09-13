package center.sciprog.maps.compose

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.isPrimaryPressed
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
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
    val onCanvasSizeChange: (DpSize) -> Unit = {},
)

@Composable
public expect fun MapView(
    mapTileProvider: MapTileProvider,
    initialViewPoint: MapViewPoint,
    features: MapFeaturesState,
    config: MapViewConfig = MapViewConfig(),
    modifier: Modifier = Modifier.fillMaxSize(),
)


internal fun GmcRectangle.computeViewPoint(
    mapTileProvider: MapTileProvider,
    canvasSize: DpSize,
): MapViewPoint {
    val zoom = log2(
        min(
            canvasSize.width.value / longitudeDelta.radians.value,
            canvasSize.height.value / latitudeDelta.radians.value
        ) * PI / mapTileProvider.tileSize
    )
    return MapViewPoint(center, zoom)
}

private val defaultCanvasSize = DpSize(512.dp, 512.dp)

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
    initialRectangle: GmcRectangle? = null,
    config: MapViewConfig = MapViewConfig(),
    modifier: Modifier = Modifier.fillMaxSize(),
    buildFeatures: @Composable (MapFeaturesState.() -> Unit) = {},
): Unit = key(buildFeatures) {

    val featureState = rememberMapFeatureState(buildFeatures)

    val features = featureState.features()

    var cachedCanvasSize: DpSize by remember { mutableStateOf(defaultCanvasSize) }

    val viewPointOverride: MapViewPoint = remember(initialViewPoint, initialRectangle, cachedCanvasSize) {
        initialViewPoint
            ?: initialRectangle?.computeViewPoint(mapTileProvider, cachedCanvasSize)
            ?: features.values.computeBoundingBox(1.0)?.computeViewPoint(mapTileProvider, cachedCanvasSize)
            ?: MapViewPoint.globe
    }

    val featureDrag by derivedStateOf {
        DragHandle.withPrimaryButton { _, start, end ->
            val zoom = start.zoom
            featureState.findAllWithAttribute(DraggableAttribute) { it }.forEach { id ->
                val feature = features[id] as? DraggableMapFeature ?: return@forEach
                val boundingBox = feature.getBoundingBox(zoom) ?: return@forEach
                if (start.focus in boundingBox) {
                    featureState.addFeature(id, feature.withCoordinates(end.focus))
                    return@withPrimaryButton false
                }
            }
            return@withPrimaryButton true
        }
    }


    val newConfig by derivedStateOf {
        config.copy(
            dragHandle = DragHandle.combine(featureDrag, config.dragHandle),
            onCanvasSizeChange = { canvasSize ->
                config.onCanvasSizeChange(canvasSize)
                cachedCanvasSize = canvasSize
            }
        )
    }


    MapView(
        mapTileProvider = mapTileProvider,
        initialViewPoint = viewPointOverride,
        features = featureState,
        config = newConfig,
        modifier = modifier,
    )
}