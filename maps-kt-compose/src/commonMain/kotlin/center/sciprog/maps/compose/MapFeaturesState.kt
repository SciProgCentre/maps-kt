package center.sciprog.maps.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PointMode
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import center.sciprog.maps.coordinates.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

@JvmInline
public value class FeatureId<out MapFeature>(public val id: String)

public class MapFeaturesState internal constructor(
    private val featureMap: MutableMap<String, MapFeature>,
    //@PublishedApi internal val attributeMap: MutableMap<String, SnapshotStateMap<Attribute<out Any?>, in Any?>>,
) {
    //TODO use context receiver for that
    public fun FeatureId<DraggableMapFeature>.draggable(
        //TODO add constraints
        callback: DragHandle = DragHandle.BYPASS,
    ) {
        val handle = DragHandle.withPrimaryButton { event, start, end ->
            val feature = featureMap[id] as? DraggableMapFeature ?: return@withPrimaryButton true
            val boundingBox = feature.getBoundingBox(start.zoom) ?: return@withPrimaryButton true
            if (start.focus in boundingBox) {
                feature(id, feature.withCoordinates(end.focus))
                callback.handle(event, start, end)
                false
            } else {
                true
            }
        }
        setAttribute(this, DraggableAttribute, handle)
    }

    /**
     * Cyclic update of a feature. Called infinitely until canceled.
     */
    public fun <T : MapFeature> FeatureId<T>.updated(
        scope: CoroutineScope,
        update: suspend (T) -> T,
    ): Job = scope.launch {
        while (isActive) {
            feature(this@updated, update(getFeature(this@updated)))
        }
    }

    @Suppress("UNCHECKED_CAST")
    public fun <T : SelectableMapFeature> FeatureId<T>.selectable(
        onSelect: (FeatureId<T>, T) -> Unit,
    ) {
        setAttribute(this, SelectableAttribute) { id, feature -> onSelect(id as FeatureId<T>, feature as T) }
    }


    public fun features(): Map<FeatureId<*>, MapFeature> = featureMap.mapKeys { FeatureId<MapFeature>(it.key) }

    @Suppress("UNCHECKED_CAST")
    public fun <T : MapFeature> getFeature(id: FeatureId<T>): T = featureMap[id.id] as T


    private fun generateID(feature: MapFeature): String = "@feature[${feature.hashCode().toUInt()}]"

    public fun <T : MapFeature> feature(id: String?, feature: T): FeatureId<T> {
        val safeId = id ?: generateID(feature)
        featureMap[safeId] = feature
        return FeatureId(safeId)
    }

    public fun <T : MapFeature> feature(id: FeatureId<T>?, feature: T): FeatureId<T> = feature(id?.id, feature)

    public fun <T> setAttribute(id: FeatureId<*>, key: MapFeature.Attribute<T>, value: T) {
        feature(id,getFeature(id).at)
        attributeMap.getOrPut(id.id) { mutableStateMapOf() }[key] = value
    }

    public fun removeAttribute(id: FeatureId<*>, key: MapFeature.Attribute<*>) {
        attributeMap[id.id]?.remove(key)
    }

    @Suppress("UNCHECKED_CAST")
    public fun <T> getAttribute(id: FeatureId<*>, key: MapFeature.Attribute<T>): T? =
        attributeMap[id.id]?.get(key)?.let { it as T }

//    @Suppress("UNCHECKED_CAST")
//    public fun <T> findAllWithAttribute(key: Attribute<T>, condition: (T) -> Boolean): Set<FeatureId> {
//        return attributes.filterValues {
//            condition(it[key] as T)
//        }.keys
//    }

    public inline fun <T> forEachWithAttribute(
        key: MapFeature.Attribute<T>,
        block: (id: FeatureId<*>, attributeValue: T) -> Unit,
    ) {
        attributeMap.forEach { (id, attributeMap) ->
            attributeMap[key]?.let {
                @Suppress("UNCHECKED_CAST")
                block(FeatureId<MapFeature>(id), it as T)
            }
        }
    }

    public companion object {

        /**
         * Build, but do not remember map feature state
         */
        public fun build(
            builder: MapFeaturesState.() -> Unit = {},
        ): MapFeaturesState = MapFeaturesState(
            mutableStateMapOf(),
            mutableStateMapOf()
        ).apply(builder)

        /**
         * Build and remember map feature state
         */
        @Composable
        public fun remember(
            builder: MapFeaturesState.() -> Unit = {},
        ): MapFeaturesState = remember(builder) {
            build(builder)
        }

    }
}

public fun MapFeaturesState.circle(
    center: GeodeticMapCoordinates,
    zoomRange: IntRange = defaultZoomRange,
    size: Float = 5f,
    color: Color = Color.Red,
    id: String? = null,
): FeatureId<MapCircleFeature> = feature(
    id, MapCircleFeature(center, zoomRange, size, color)
)

public fun MapFeaturesState.circle(
    centerCoordinates: Pair<Double, Double>,
    zoomRange: IntRange = defaultZoomRange,
    size: Float = 5f,
    color: Color = Color.Red,
    id: String? = null,
): FeatureId<MapCircleFeature> = feature(
    id, MapCircleFeature(centerCoordinates.toCoordinates(), zoomRange, size, color)
)

public fun MapFeaturesState.rectangle(
    centerCoordinates: Gmc,
    zoomRange: IntRange = defaultZoomRange,
    size: DpSize = DpSize(5.dp, 5.dp),
    color: Color = Color.Red,
    id: String? = null,
): FeatureId<MapRectangleFeature> = feature(
    id, MapRectangleFeature(centerCoordinates, zoomRange, size, color)
)

public fun MapFeaturesState.rectangle(
    centerCoordinates: Pair<Double, Double>,
    zoomRange: IntRange = defaultZoomRange,
    size: DpSize = DpSize(5.dp, 5.dp),
    color: Color = Color.Red,
    id: String? = null,
): FeatureId<MapRectangleFeature> = feature(
    id, MapRectangleFeature(centerCoordinates.toCoordinates(), zoomRange, size, color)
)

public fun MapFeaturesState.draw(
    position: Pair<Double, Double>,
    zoomRange: IntRange = defaultZoomRange,
    id: String? = null,
    drawFeature: DrawScope.() -> Unit,
): FeatureId<MapDrawFeature> = feature(id, MapDrawFeature(position.toCoordinates(), zoomRange, drawFeature))

public fun MapFeaturesState.line(
    aCoordinates: Gmc,
    bCoordinates: Gmc,
    zoomRange: IntRange = defaultZoomRange,
    color: Color = Color.Red,
    id: String? = null,
): FeatureId<MapLineFeature> = feature(
    id,
    MapLineFeature(aCoordinates, bCoordinates, zoomRange, color)
)

public fun MapFeaturesState.line(
    curve: GmcCurve,
    zoomRange: IntRange = defaultZoomRange,
    color: Color = Color.Red,
    id: String? = null,
): FeatureId<MapLineFeature> = feature(
    id,
    MapLineFeature(curve.forward.coordinates, curve.backward.coordinates, zoomRange, color)
)

public fun MapFeaturesState.line(
    aCoordinates: Pair<Double, Double>,
    bCoordinates: Pair<Double, Double>,
    zoomRange: IntRange = defaultZoomRange,
    color: Color = Color.Red,
    id: String? = null,
): FeatureId<MapLineFeature> = feature(
    id,
    MapLineFeature(aCoordinates.toCoordinates(), bCoordinates.toCoordinates(), zoomRange, color)
)

public fun MapFeaturesState.arc(
    oval: GmcRectangle,
    startAngle: Angle,
    arcLength: Angle,
    zoomRange: IntRange = defaultZoomRange,
    color: Color = Color.Red,
    id: String? = null,
): FeatureId<MapArcFeature> = feature(
    id,
    MapArcFeature(oval, startAngle, arcLength, zoomRange, color)
)

public fun MapFeaturesState.arc(
    center: Pair<Double, Double>,
    radius: Distance,
    startAngle: Angle,
    arcLength: Angle,
    zoomRange: IntRange = defaultZoomRange,
    color: Color = Color.Red,
    id: String? = null,
): FeatureId<MapArcFeature> = feature(
    id,
    MapArcFeature(
        oval = GmcRectangle.square(center.toCoordinates(), radius, radius),
        startAngle = startAngle,
        arcLength = arcLength,
        zoomRange = zoomRange,
        color = color
    )
)

public fun MapFeaturesState.points(
    points: List<Gmc>,
    zoomRange: IntRange = defaultZoomRange,
    stroke: Float = 2f,
    color: Color = Color.Red,
    pointMode: PointMode = PointMode.Points,
    id: String? = null,
): FeatureId<MapPointsFeature> = feature(id, MapPointsFeature(points, zoomRange, stroke, color, pointMode))

@JvmName("pointsFromPairs")
public fun MapFeaturesState.points(
    points: List<Pair<Double, Double>>,
    zoomRange: IntRange = defaultZoomRange,
    stroke: Float = 2f,
    color: Color = Color.Red,
    pointMode: PointMode = PointMode.Points,
    id: String? = null,
): FeatureId<MapPointsFeature> =
    feature(id, MapPointsFeature(points.map { it.toCoordinates() }, zoomRange, stroke, color, pointMode))

public fun MapFeaturesState.image(
    position: Pair<Double, Double>,
    image: ImageVector,
    size: DpSize = DpSize(20.dp, 20.dp),
    zoomRange: IntRange = defaultZoomRange,
    id: String? = null,
): FeatureId<MapVectorImageFeature> =
    feature(id, MapVectorImageFeature(position.toCoordinates(), image, size, zoomRange))

public fun MapFeaturesState.group(
    zoomRange: IntRange = defaultZoomRange,
    id: String? = null,
    builder: MapFeaturesState.() -> Unit,
): FeatureId<MapFeatureGroup> {
    val map = MapFeaturesState(
        mutableStateMapOf(),
        mutableStateMapOf()
    ).apply(builder).features()
    val feature = MapFeatureGroup(map, zoomRange)
    return feature(id, feature)
}

public fun MapFeaturesState.text(
    position: GeodeticMapCoordinates,
    text: String,
    zoomRange: IntRange = defaultZoomRange,
    color: Color = Color.Red,
    font: MapTextFeatureFont.() -> Unit = { size = 16f },
    id: String? = null,
): FeatureId<MapTextFeature> = feature(id, MapTextFeature(position, text, zoomRange, color, font))

public fun MapFeaturesState.text(
    position: Pair<Double, Double>,
    text: String,
    zoomRange: IntRange = defaultZoomRange,
    color: Color = Color.Red,
    font: MapTextFeatureFont.() -> Unit = { size = 16f },
    id: String? = null,
): FeatureId<MapTextFeature> = feature(id, MapTextFeature(position.toCoordinates(), text, zoomRange, color, font))
