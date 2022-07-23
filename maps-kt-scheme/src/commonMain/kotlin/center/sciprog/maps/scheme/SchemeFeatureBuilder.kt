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

interface SchemeFeatureBuilder {
    fun addFeature(id: FeatureId?, feature: SchemeFeature): FeatureId

    fun build(): SnapshotStateMap<FeatureId, SchemeFeature>
}

internal class SchemeFeatureBuilderImpl(
    initialFeatures: Map<FeatureId, SchemeFeature>,
) : SchemeFeatureBuilder {

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

fun SchemeFeatureBuilder.background(
    painter: Painter,
    box: SchemeCoordinateBox,
    id: FeatureId? = null,
): FeatureId = addFeature(
    id,
    SchemeBackgroundFeature(box, painter)
)

fun SchemeFeatureBuilder.background(
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

fun SchemeFeatureBuilder.circle(
    center: SchemeCoordinates,
    scaleRange: FloatRange = defaultScaleRange,
    size: Float = 5f,
    color: Color = Color.Red,
    id: FeatureId? = null,
) = addFeature(
    id, SchemeCircleFeature(center, scaleRange, size, color)
)

fun SchemeFeatureBuilder.circle(
    centerCoordinates: Pair<Number, Number>,
    scaleRange: FloatRange = defaultScaleRange,
    size: Float = 5f,
    color: Color = Color.Red,
    id: FeatureId? = null,
) = addFeature(
    id, SchemeCircleFeature(centerCoordinates.toCoordinates(), scaleRange, size, color)
)

fun SchemeFeatureBuilder.draw(
    position: Pair<Number, Number>,
    scaleRange: FloatRange = defaultScaleRange,
    id: FeatureId? = null,
    drawFeature: DrawScope.() -> Unit,
) = addFeature(id, SchemeDrawFeature(position.toCoordinates(), scaleRange, drawFeature))

fun SchemeFeatureBuilder.line(
    aCoordinates: Pair<Number, Number>,
    bCoordinates: Pair<Number, Number>,
    scaleRange: FloatRange = defaultScaleRange,
    color: Color = Color.Red,
    id: FeatureId? = null,
) = addFeature(id, SchemeLineFeature(aCoordinates.toCoordinates(), bCoordinates.toCoordinates(), scaleRange, color))

fun SchemeFeatureBuilder.text(
    position: SchemeCoordinates,
    text: String,
    scaleRange: FloatRange = defaultScaleRange,
    color: Color = Color.Red,
    id: FeatureId? = null,
) = addFeature(id, SchemeTextFeature(position, text, scaleRange, color))

fun SchemeFeatureBuilder.text(
    position: Pair<Number, Number>,
    text: String,
    scaleRange: FloatRange = defaultScaleRange,
    color: Color = Color.Red,
    id: FeatureId? = null,
) = addFeature(id, SchemeTextFeature(position.toCoordinates(), text, scaleRange, color))

@Composable
fun SchemeFeatureBuilder.image(
    position: Pair<Number, Number>,
    image: ImageVector,
    size: DpSize = DpSize(20.dp, 20.dp),
    scaleRange: FloatRange = defaultScaleRange,
    id: FeatureId? = null,
) = addFeature(id, SchemeVectorImageFeature(position.toCoordinates(), image, size, scaleRange))

fun SchemeFeatureBuilder.group(
    scaleRange: FloatRange = defaultScaleRange,
    id: FeatureId? = null,
    builder: SchemeFeatureBuilder.() -> Unit,
): FeatureId {
    val map = SchemeFeatureBuilderImpl(emptyMap()).apply(builder).build()
    val feature = SchemeFeatureGroup(map, scaleRange)
    return addFeature(id, feature)
}