package center.sciprog.maps.features

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

public class MapFeaturesState {

    @PublishedApi
    internal val featureMap: MutableMap<String, Feature> = mutableStateMapOf()

    //TODO use context receiver for that
    public fun FeatureId<DraggableFeature>.draggable(
        //TODO add constraints
        callback: DragHandle = DragHandle.BYPASS,
    ) {
        val handle = DragHandle.withPrimaryButton { event, start, end ->
            val feature = featureMap[id] as? DraggableFeature ?: return@withPrimaryButton true
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
    public fun <T : Feature> FeatureId<T>.updated(
        scope: CoroutineScope,
        update: suspend (T) -> T,
    ): Job = scope.launch {
        while (isActive) {
            feature(this@updated, update(getFeature(this@updated)))
        }
    }

    @Suppress("UNCHECKED_CAST")
    public fun <T : SelectableFeature> FeatureId<T>.selectable(
        onSelect: (FeatureId<T>, T) -> Unit,
    ) {
        setAttribute(this, SelectableAttribute) { id, feature -> onSelect(id as FeatureId<T>, feature as T) }
    }


    public val features: Map<FeatureId<*>, Feature>
        get() = featureMap.mapKeys { FeatureId<Feature>(it.key) }

    @Suppress("UNCHECKED_CAST")
    public fun <T : Feature> getFeature(id: FeatureId<T>): T = featureMap[id.id] as T


    private fun generateID(feature: Feature): String = "@feature[${feature.hashCode().toUInt()}]"

    public fun <T : Feature> feature(id: String?, feature: T): FeatureId<T> {
        val safeId = id ?: generateID(feature)
        featureMap[safeId] = feature
        return FeatureId(safeId)
    }

    public fun <T : Feature> feature(id: FeatureId<T>?, feature: T): FeatureId<T> = feature(id?.id, feature)

    public fun <T> setAttribute(id: FeatureId<Feature>, key: Feature.Attribute<T>, value: T?) {
        getFeature(id).attributes.setAttribute(key, value)
    }

    @Suppress("UNCHECKED_CAST")
    public fun <T> getAttribute(id: FeatureId<Feature>, key: Feature.Attribute<T>): T? =
        getFeature(id).attributes[key]


//    @Suppress("UNCHECKED_CAST")
//    public fun <T> findAllWithAttribute(key: Attribute<T>, condition: (T) -> Boolean): Set<FeatureId> {
//        return attributes.filterValues {
//            condition(it[key] as T)
//        }.keys
//    }

    public inline fun <T> forEachWithAttribute(
        key: Feature.Attribute<T>,
        block: (id: FeatureId<*>, attributeValue: T) -> Unit,
    ) {
        featureMap.forEach { (id, feature) ->
            feature.attributes[key]?.let {
                block(FeatureId<Feature>(id), it)
            }
        }
    }

    public companion object {

        /**
         * Build, but do not remember map feature state
         */
        public fun build(
            builder: MapFeaturesState.() -> Unit = {},
        ): MapFeaturesState = MapFeaturesState().apply(builder)

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
): FeatureId<CircleFeature> = feature(
    id, CircleFeature(center, zoomRange, size, color)
)

public fun MapFeaturesState.circle(
    centerCoordinates: Pair<Double, Double>,
    zoomRange: IntRange = defaultZoomRange,
    size: Float = 5f,
    color: Color = Color.Red,
    id: String? = null,
): FeatureId<CircleFeature> = feature(
    id, CircleFeature(centerCoordinates.toCoordinates(), zoomRange, size, color)
)

public fun MapFeaturesState.rectangle(
    centerCoordinates: Gmc,
    zoomRange: IntRange = defaultZoomRange,
    size: DpSize = DpSize(5.dp, 5.dp),
    color: Color = Color.Red,
    id: String? = null,
): FeatureId<RectangleFeature> = feature(
    id, RectangleFeature(centerCoordinates, zoomRange, size, color)
)

public fun MapFeaturesState.rectangle(
    centerCoordinates: Pair<Double, Double>,
    zoomRange: IntRange = defaultZoomRange,
    size: DpSize = DpSize(5.dp, 5.dp),
    color: Color = Color.Red,
    id: String? = null,
): FeatureId<RectangleFeature> = feature(
    id, RectangleFeature(centerCoordinates.toCoordinates(), zoomRange, size, color)
)

public fun MapFeaturesState.draw(
    position: Pair<Double, Double>,
    zoomRange: IntRange = defaultZoomRange,
    id: String? = null,
    draw: DrawScope.() -> Unit,
): FeatureId<DrawFeature> = feature(id, DrawFeature(position.toCoordinates(), zoomRange, drawFeature = draw))

public fun MapFeaturesState.line(
    aCoordinates: Gmc,
    bCoordinates: Gmc,
    zoomRange: IntRange = defaultZoomRange,
    color: Color = Color.Red,
    id: String? = null,
): FeatureId<LineFeature> = feature(
    id,
    LineFeature(aCoordinates, bCoordinates, zoomRange, color)
)

public fun MapFeaturesState.line(
    curve: GmcCurve,
    zoomRange: IntRange = defaultZoomRange,
    color: Color = Color.Red,
    id: String? = null,
): FeatureId<LineFeature> = feature(
    id,
    LineFeature(curve.forward.coordinates, curve.backward.coordinates, zoomRange, color)
)

public fun MapFeaturesState.line(
    aCoordinates: Pair<Double, Double>,
    bCoordinates: Pair<Double, Double>,
    zoomRange: IntRange = defaultZoomRange,
    color: Color = Color.Red,
    id: String? = null,
): FeatureId<LineFeature> = feature(
    id,
    LineFeature(aCoordinates.toCoordinates(), bCoordinates.toCoordinates(), zoomRange, color)
)

public fun MapFeaturesState.arc(
    oval: GmcRectangle,
    startAngle: Angle,
    arcLength: Angle,
    zoomRange: IntRange = defaultZoomRange,
    color: Color = Color.Red,
    id: String? = null,
): FeatureId<ArcFeature> = feature(
    id,
    ArcFeature(oval, startAngle, arcLength, zoomRange, color)
)

public fun MapFeaturesState.arc(
    center: Pair<Double, Double>,
    radius: Distance,
    startAngle: Angle,
    arcLength: Angle,
    zoomRange: IntRange = defaultZoomRange,
    color: Color = Color.Red,
    id: String? = null,
): FeatureId<ArcFeature> = feature(
    id,
    ArcFeature(
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
): FeatureId<PointsFeature> = feature(id, PointsFeature(points, zoomRange, stroke, color, pointMode))

@JvmName("pointsFromPairs")
public fun MapFeaturesState.points(
    points: List<Pair<Double, Double>>,
    zoomRange: IntRange = defaultZoomRange,
    stroke: Float = 2f,
    color: Color = Color.Red,
    pointMode: PointMode = PointMode.Points,
    id: String? = null,
): FeatureId<PointsFeature> =
    feature(id, PointsFeature(points.map { it.toCoordinates() }, zoomRange, stroke, color, pointMode))

public fun MapFeaturesState.image(
    position: Pair<Double, Double>,
    image: ImageVector,
    size: DpSize = DpSize(20.dp, 20.dp),
    zoomRange: IntRange = defaultZoomRange,
    id: String? = null,
): FeatureId<VectorImageFeature> =
    feature(id, VectorImageFeature(position.toCoordinates(), image, size, zoomRange))

public fun MapFeaturesState.group(
    zoomRange: IntRange = defaultZoomRange,
    id: String? = null,
    builder: MapFeaturesState.() -> Unit,
): FeatureId<FeatureGroup> {
    val map = MapFeaturesState().apply(builder).features
    val feature = FeatureGroup(map, zoomRange)
    return feature(id, feature)
}

public fun MapFeaturesState.text(
    position: GeodeticMapCoordinates,
    text: String,
    zoomRange: IntRange = defaultZoomRange,
    color: Color = Color.Red,
    font: FeatureFont.() -> Unit = { size = 16f },
    id: String? = null,
): FeatureId<TextFeature> = feature(
    id,
    TextFeature(position, text, zoomRange, color, fontConfig = font)
)

public fun MapFeaturesState.text(
    position: Pair<Double, Double>,
    text: String,
    zoomRange: IntRange = defaultZoomRange,
    color: Color = Color.Red,
    font: FeatureFont.() -> Unit = { size = 16f },
    id: String? = null,
): FeatureId<TextFeature> = feature(
    id,
    TextFeature(position.toCoordinates(), text, zoomRange, color, fontConfig = font)
)
