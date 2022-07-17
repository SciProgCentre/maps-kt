package centre.sciprog.maps.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.vector.ImageVector
import centre.sciprog.maps.GeodeticMapCoordinates
import centre.sciprog.maps.GmcBox

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

fun FeatureBuilder.route(
    route: List<Pair<Double, Double>>,
    zoomRange: IntRange = defaultZoomRange,
    stroke: Float = 2f,
    color: Color = Color.Red,
    id: FeatureId? = null
) = addFeature(id, MapRouteFeature(route.map { it.toCoordinates() }, zoomRange, stroke, color))

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
    id: FeatureId? = null,
    customFeatureBuilder: DrawScope.(Offset) -> Unit,
) = addFeature(id, object : MapCustomFeature(position = position.toCoordinates()) {
    override fun drawFeature(drawScope: DrawScope, offset: Offset) {
        customFeatureBuilder(drawScope, offset)
    }

    override fun getBoundingBox(zoom: Int): GmcBox {
        return GmcBox(position.toCoordinates(), position.toCoordinates())
    }

})

fun FeatureBuilder.line(
    aCoordinates: Pair<Double, Double>,
    bCoordinates: Pair<Double, Double>,
    zoomRange: IntRange = defaultZoomRange,
    color: Color = Color.Red,
    id: FeatureId? = null,
) = addFeature(id, MapLineFeature(aCoordinates.toCoordinates(), bCoordinates.toCoordinates(), zoomRange, color))

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
    size: Size = Size(20f, 20f),
    zoomRange: IntRange = defaultZoomRange,
    id: FeatureId? = null,
) = addFeature(id, MapVectorImageFeature(position.toCoordinates(), image, size, zoomRange))