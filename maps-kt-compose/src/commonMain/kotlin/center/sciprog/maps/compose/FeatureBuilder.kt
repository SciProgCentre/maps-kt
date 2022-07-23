package center.sciprog.maps.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import center.sciprog.maps.coordinates.GeodeticMapCoordinates

typealias FeatureId = String

interface FeatureBuilder {
    fun addFeature(id: FeatureId?, feature: MapFeature): FeatureId

    fun build(): SnapshotStateMap<FeatureId, MapFeature>
}

internal class MapFeatureBuilder(initialFeatures: Map<FeatureId, MapFeature>) : FeatureBuilder {

    private val content: SnapshotStateMap<FeatureId, MapFeature> = mutableStateMapOf<FeatureId, MapFeature>().apply {
        putAll(initialFeatures)
    }

    private fun generateID(feature: MapFeature): FeatureId = "@feature[${feature.hashCode().toUInt()}]"

    override fun addFeature(id: FeatureId?, feature: MapFeature): FeatureId {
        val safeId = id ?: generateID(feature)
        content[id ?: generateID(feature)] = feature
        return safeId
    }

    override fun build(): SnapshotStateMap<FeatureId, MapFeature> = content
}

fun FeatureBuilder.circle(
    center: GeodeticMapCoordinates,
    zoomRange: IntRange = defaultZoomRange,
    size: Float = 5f,
    color: Color = Color.Red,
    id: FeatureId? = null,
) = addFeature(
    id, MapCircleFeature(center, zoomRange, size, color)
)

fun FeatureBuilder.circle(
    centerCoordinates: Pair<Double, Double>,
    zoomRange: IntRange = defaultZoomRange,
    size: Float = 5f,
    color: Color = Color.Red,
    id: FeatureId? = null,
) = addFeature(
    id, MapCircleFeature(centerCoordinates.toCoordinates(), zoomRange, size, color)
)

fun FeatureBuilder.custom(
    position: Pair<Double, Double>,
    zoomRange: IntRange = defaultZoomRange,
    id: FeatureId? = null,
    drawFeature: DrawScope.() -> Unit,
) = addFeature(id, MapDrawFeature(position.toCoordinates(), zoomRange, drawFeature))

fun FeatureBuilder.line(
    aCoordinates: Pair<Double, Double>,
    bCoordinates: Pair<Double, Double>,
    zoomRange: IntRange = defaultZoomRange,
    color: Color = Color.Red,
    id: FeatureId? = null,
) = addFeature(id, MapLineFeature(aCoordinates.toCoordinates(), bCoordinates.toCoordinates(), zoomRange, color))

fun FeatureBuilder.text(
    position: GeodeticMapCoordinates,
    text: String,
    zoomRange: IntRange = defaultZoomRange,
    color: Color = Color.Red,
    id: FeatureId? = null,
) = addFeature(id, MapTextFeature(position, text, zoomRange, color))

fun FeatureBuilder.text(
    position: Pair<Double, Double>,
    text: String,
    zoomRange: IntRange = defaultZoomRange,
    color: Color = Color.Red,
    id: FeatureId? = null,
) = addFeature(id, MapTextFeature(position.toCoordinates(), text, zoomRange, color))

@Composable
fun FeatureBuilder.image(
    position: Pair<Double, Double>,
    image: ImageVector,
    size: DpSize = DpSize(20.dp, 20.dp),
    zoomRange: IntRange = defaultZoomRange,
    id: FeatureId? = null,
) = addFeature(id, MapVectorImageFeature(position.toCoordinates(), image, size, zoomRange))

fun FeatureBuilder.group(
    zoomRange: IntRange = defaultZoomRange,
    id: FeatureId? = null,
    builder: FeatureBuilder.() -> Unit,
): FeatureId {
    val map = MapFeatureBuilder(emptyMap()).apply(builder).build()
    val feature = MapFeatureGroup(map, zoomRange)
    return addFeature(id, feature)
}