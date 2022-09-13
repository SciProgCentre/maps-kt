package center.sciprog.maps.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PointMode
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import center.sciprog.maps.coordinates.*

public typealias FeatureId = String

public object DraggableAttribute : MapFeaturesState.Attribute<Boolean>

public class MapFeaturesState internal constructor(
    private val features: MutableMap<FeatureId, MapFeature>,
    private val attributes: MutableMap<FeatureId, SnapshotStateMap<Attribute<out Any?>, in Any?>>,
) {
    public interface Attribute<T>

    //TODO use context receiver for that
    public fun FeatureId.draggable(enabled: Boolean = true) {
        setAttribute(this, DraggableAttribute, enabled)
    }

    public fun features(): Map<FeatureId, MapFeature> = features


    private fun generateID(feature: MapFeature): FeatureId = "@feature[${feature.hashCode().toUInt()}]"

    public fun addFeature(id: FeatureId?, feature: MapFeature): FeatureId {
        val safeId = id ?: generateID(feature)
        features[id ?: generateID(feature)] = feature
        return safeId
    }

    public fun <T> setAttribute(id: FeatureId, key: MapFeaturesState.Attribute<T>, value: T) {
        attributes.getOrPut(id) { mutableStateMapOf() }[key] = value
    }

    @Suppress("UNCHECKED_CAST")
    public fun <T> getAttribute(id: FeatureId, key: MapFeaturesState.Attribute<T>): T? =
        attributes[id]?.get(key)?.let { it as T }

    @Suppress("UNCHECKED_CAST")
    public fun <T> findAllWithAttribute(key: MapFeaturesState.Attribute<T>, condition: (T) -> Boolean): Set<FeatureId> {
        return attributes.filterValues {
            condition(it[key] as T)
        }.keys
    }
}

@Composable
public fun rememberMapFeatureState(
    builder: @Composable MapFeaturesState.() -> Unit = {},
): MapFeaturesState = remember(builder) {
    MapFeaturesState(
        mutableStateMapOf(),
        mutableStateMapOf()
    )
}.apply { builder() }

public fun MapFeaturesState.circle(
    center: GeodeticMapCoordinates,
    zoomRange: IntRange = defaultZoomRange,
    size: Float = 5f,
    color: Color = Color.Red,
    id: FeatureId? = null,
): FeatureId = addFeature(
    id, MapCircleFeature(center, zoomRange, size, color)
)

public fun MapFeaturesState.circle(
    centerCoordinates: Pair<Double, Double>,
    zoomRange: IntRange = defaultZoomRange,
    size: Float = 5f,
    color: Color = Color.Red,
    id: FeatureId? = null,
): FeatureId = addFeature(
    id, MapCircleFeature(centerCoordinates.toCoordinates(), zoomRange, size, color)
)

public fun MapFeaturesState.rectangle(
    centerCoordinates: Pair<Double, Double>,
    zoomRange: IntRange = defaultZoomRange,
    size: DpSize = DpSize(5.dp, 5.dp),
    color: Color = Color.Red,
    id: FeatureId? = null,
): FeatureId = addFeature(
    id, MapRectangleFeature(centerCoordinates.toCoordinates(), zoomRange, size, color)
)

public fun MapFeaturesState.draw(
    position: Pair<Double, Double>,
    zoomRange: IntRange = defaultZoomRange,
    id: FeatureId? = null,
    drawFeature: DrawScope.() -> Unit,
): FeatureId = addFeature(id, MapDrawFeature(position.toCoordinates(), zoomRange, drawFeature))

public fun MapFeaturesState.line(
    aCoordinates: Gmc,
    bCoordinates: Gmc,
    zoomRange: IntRange = defaultZoomRange,
    color: Color = Color.Red,
    id: FeatureId? = null,
): FeatureId = addFeature(
    id,
    MapLineFeature(aCoordinates, bCoordinates, zoomRange, color)
)

public fun MapFeaturesState.line(
    curve: GmcCurve,
    zoomRange: IntRange = defaultZoomRange,
    color: Color = Color.Red,
    id: FeatureId? = null,
): FeatureId = addFeature(
    id,
    MapLineFeature(curve.forward.coordinates, curve.backward.coordinates, zoomRange, color)
)

public fun MapFeaturesState.line(
    aCoordinates: Pair<Double, Double>,
    bCoordinates: Pair<Double, Double>,
    zoomRange: IntRange = defaultZoomRange,
    color: Color = Color.Red,
    id: FeatureId? = null,
): FeatureId = addFeature(
    id,
    MapLineFeature(aCoordinates.toCoordinates(), bCoordinates.toCoordinates(), zoomRange, color)
)

public fun MapFeaturesState.arc(
    oval: GmcRectangle,
    startAngle: Angle,
    arcLength: Angle,
    zoomRange: IntRange = defaultZoomRange,
    color: Color = Color.Red,
    id: FeatureId? = null,
): FeatureId = addFeature(
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
    id: FeatureId? = null,
): FeatureId = addFeature(
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
    id: FeatureId? = null,
): FeatureId = addFeature(id, MapPointsFeature(points, zoomRange, stroke, color, pointMode))

@JvmName("pointsFromPairs")
public fun MapFeaturesState.points(
    points: List<Pair<Double, Double>>,
    zoomRange: IntRange = defaultZoomRange,
    stroke: Float = 2f,
    color: Color = Color.Red,
    pointMode: PointMode = PointMode.Points,
    id: FeatureId? = null,
): FeatureId = addFeature(id, MapPointsFeature(points.map { it.toCoordinates() }, zoomRange, stroke, color, pointMode))

@Composable
public fun MapFeaturesState.image(
    position: Pair<Double, Double>,
    image: ImageVector,
    size: DpSize = DpSize(20.dp, 20.dp),
    zoomRange: IntRange = defaultZoomRange,
    id: FeatureId? = null,
): FeatureId = addFeature(id, MapVectorImageFeature(position.toCoordinates(), image, size, zoomRange))

@Composable
public fun MapFeaturesState.group(
    zoomRange: IntRange = defaultZoomRange,
    id: FeatureId? = null,
    builder: @Composable MapFeaturesState.() -> Unit,
): FeatureId {
    val map = rememberMapFeatureState(builder).features()
    val feature = MapFeatureGroup(map, zoomRange)
    return addFeature(id, feature)
}

public fun MapFeaturesState.text(
    position: GeodeticMapCoordinates,
    text: String,
    zoomRange: IntRange = defaultZoomRange,
    color: Color = Color.Red,
    font: MapTextFeatureFont.() -> Unit = { size = 16f },
    id: FeatureId? = null,
): FeatureId = addFeature(id, MapTextFeature(position, text, zoomRange, color, font))

public fun MapFeaturesState.text(
    position: Pair<Double, Double>,
    text: String,
    zoomRange: IntRange = defaultZoomRange,
    color: Color = Color.Red,
    font: MapTextFeatureFont.() -> Unit = { size = 16f },
    id: FeatureId? = null,
): FeatureId = addFeature(id, MapTextFeature(position.toCoordinates(), text, zoomRange, color, font))
