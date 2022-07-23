package center.sciprog.maps.scheme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import center.sciprog.maps.scheme.SchemeFeature.Companion.defaultScaleRange

typealias FeatureId = String

interface FeatureBuilder {
    fun addFeature(id: FeatureId?, feature: SchemeFeature): FeatureId

    fun build(): SnapshotStateMap<FeatureId, SchemeFeature>
}

internal class SchemeFeatureBuilder(initialFeatures: Map<FeatureId, SchemeFeature>) : FeatureBuilder {

    private val content: SnapshotStateMap<FeatureId, SchemeFeature> =
        mutableStateMapOf<FeatureId, SchemeFeature>().apply {
            putAll(initialFeatures)
        }

    private fun generateID(feature: SchemeFeature): FeatureId = "@feature[${feature.hashCode().toUInt()}]"

    override fun addFeature(id: FeatureId?, feature: SchemeFeature): FeatureId {
        val safeId = id ?: generateID(feature)
        content[id ?: generateID(feature)] = feature
        return safeId
    }

    override fun build(): SnapshotStateMap<FeatureId, SchemeFeature> = content
}

fun FeatureBuilder.background(
    painter: Painter,
    box: SchemeCoordinateBox,
    id: FeatureId? = null,
): FeatureId = addFeature(
    id,
    SchemeBackgroundFeature(box, painter)
)

fun FeatureBuilder.background(
    painter: Painter,
    size: Size = painter.intrinsicSize,
    offset: SchemeCoordinates = SchemeCoordinates(0f, 0f),
    id: FeatureId? = null,
): FeatureId {
    val box = SchemeCoordinateBox(
        offset,
        SchemeCoordinates(size.width + offset.x, size.height + offset.y)
    )
    return background(painter, box, id)
}

fun FeatureBuilder.circle(
    center: SchemeCoordinates,
    scaleRange: FloatRange = defaultScaleRange,
    size: Float = 5f,
    color: Color = Color.Red,
    id: FeatureId? = null,
) = addFeature(
    id, SchemeCircleFeature(center, scaleRange, size, color)
)

fun FeatureBuilder.circle(
    centerCoordinates: Pair<Number, Number>,
    scaleRange: FloatRange = defaultScaleRange,
    size: Float = 5f,
    color: Color = Color.Red,
    id: FeatureId? = null,
) = addFeature(
    id, SchemeCircleFeature(centerCoordinates.toCoordinates(), scaleRange, size, color)
)

fun FeatureBuilder.custom(
    position: Pair<Number, Number>,
    scaleRange: FloatRange = defaultScaleRange,
    id: FeatureId? = null,
    drawFeature: DrawScope.() -> Unit,
) = addFeature(id, SchemeDrawFeature(position.toCoordinates(), scaleRange, drawFeature))

fun FeatureBuilder.line(
    aCoordinates: Pair<Number, Number>,
    bCoordinates: Pair<Number, Number>,
    scaleRange: FloatRange = defaultScaleRange,
    color: Color = Color.Red,
    id: FeatureId? = null,
) = addFeature(id, SchemeLineFeature(aCoordinates.toCoordinates(), bCoordinates.toCoordinates(), scaleRange, color))

fun FeatureBuilder.text(
    position: SchemeCoordinates,
    text: String,
    scaleRange: FloatRange = defaultScaleRange,
    color: Color = Color.Red,
    id: FeatureId? = null,
) = addFeature(id, SchemeTextFeature(position, text, scaleRange, color))

fun FeatureBuilder.text(
    position: Pair<Number, Number>,
    text: String,
    scaleRange: FloatRange = defaultScaleRange,
    color: Color = Color.Red,
    id: FeatureId? = null,
) = addFeature(id, SchemeTextFeature(position.toCoordinates(), text, scaleRange, color))

@Composable
fun FeatureBuilder.image(
    position: Pair<Number, Number>,
    image: ImageVector,
    size: DpSize = DpSize(20.dp, 20.dp),
    scaleRange: FloatRange = defaultScaleRange,
    id: FeatureId? = null,
) = addFeature(id, SchemeVectorImageFeature(position.toCoordinates(), image, size, scaleRange))

fun FeatureBuilder.group(
    scaleRange: FloatRange = defaultScaleRange,
    id: FeatureId? = null,
    builder: FeatureBuilder.() -> Unit,
): FeatureId {
    val map = SchemeFeatureBuilder(emptyMap()).apply(builder).build()
    val feature = SchemeFeatureGroup(map, scaleRange)
    return addFeature(id, feature)
}