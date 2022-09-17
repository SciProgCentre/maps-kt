package center.sciprog.maps.scheme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import center.sciprog.maps.scheme.SchemeFeature.Companion.defaultScaleRange

typealias FeatureId = String


public class SchemeFeaturesState internal constructor(
    private val features: MutableMap<FeatureId, SchemeFeature>,
    private val attributes: MutableMap<FeatureId, SnapshotStateMap<Attribute<out Any?>, in Any?>>,
) {
    public interface Attribute<T>

    public fun features(): Map<FeatureId, SchemeFeature> = features


    private fun generateID(feature: SchemeFeature): FeatureId = "@feature[${feature.hashCode().toUInt()}]"

    public fun addFeature(id: FeatureId?, feature: SchemeFeature): FeatureId {
        val safeId = id ?: generateID(feature)
        features[id ?: generateID(feature)] = feature
        return safeId
    }

    public fun <T> setAttribute(id: FeatureId, key: Attribute<T>, value: T) {
        attributes.getOrPut(id) { mutableStateMapOf() }[key] = value
    }

    @Suppress("UNCHECKED_CAST")
    public fun <T> getAttribute(id: FeatureId, key: Attribute<T>): T? =
        attributes[id]?.get(key)?.let { it as T }

    @Suppress("UNCHECKED_CAST")
    public fun <T> findAllWithAttribute(key: Attribute<T>, condition: (T) -> Boolean): Set<FeatureId> {
        return attributes.filterValues {
            condition(it[key] as T)
        }.keys
    }

    public companion object {

        /**
         * Build, but do not remember map feature state
         */
        public fun build(
            builder: SchemeFeaturesState.() -> Unit = {},
        ): SchemeFeaturesState = SchemeFeaturesState(
            mutableStateMapOf(),
            mutableStateMapOf()
        ).apply(builder)

        /**
         * Build and remember map feature state
         */
        @Composable
        public fun remember(
            builder: SchemeFeaturesState.() -> Unit = {},
        ): SchemeFeaturesState = androidx.compose.runtime.remember(builder) {
            build(builder)
        }

    }
}

fun SchemeFeaturesState.background(
    box: SchemeRectangle,
    id: FeatureId? = null,
    painter: @Composable () -> Painter,
): FeatureId = addFeature(
    id,
    SchemeBackgroundFeature(box, painter = painter)
)

fun SchemeFeaturesState.background(
    width: Float,
    height: Float,
    offset: SchemeCoordinates = SchemeCoordinates(0f, 0f),
    id: FeatureId? = null,
    painter: @Composable () -> Painter,
): FeatureId {
    val box = SchemeRectangle(
        offset,
        SchemeCoordinates(width + offset.x, height + offset.y)
    )
    return background(box, id, painter = painter)
}

fun SchemeFeaturesState.circle(
    center: SchemeCoordinates,
    scaleRange: FloatRange = defaultScaleRange,
    size: Float = 5f,
    color: Color = Color.Red,
    id: FeatureId? = null,
) = addFeature(
    id, SchemeCircleFeature(center, scaleRange, size, color)
)

fun SchemeFeaturesState.circle(
    centerCoordinates: Pair<Number, Number>,
    scaleRange: FloatRange = defaultScaleRange,
    size: Float = 5f,
    color: Color = Color.Red,
    id: FeatureId? = null,
) = addFeature(
    id, SchemeCircleFeature(centerCoordinates.toCoordinates(), scaleRange, size, color)
)

fun SchemeFeaturesState.draw(
    position: Pair<Number, Number>,
    scaleRange: FloatRange = defaultScaleRange,
    id: FeatureId? = null,
    drawFeature: DrawScope.() -> Unit,
) = addFeature(id, SchemeDrawFeature(position.toCoordinates(), scaleRange, drawFeature))

fun SchemeFeaturesState.line(
    aCoordinates: Pair<Number, Number>,
    bCoordinates: Pair<Number, Number>,
    scaleRange: FloatRange = defaultScaleRange,
    color: Color = Color.Red,
    id: FeatureId? = null,
) = addFeature(id, SchemeLineFeature(aCoordinates.toCoordinates(), bCoordinates.toCoordinates(), scaleRange, color))

fun SchemeFeaturesState.text(
    position: SchemeCoordinates,
    text: String,
    scaleRange: FloatRange = defaultScaleRange,
    color: Color = Color.Red,
    id: FeatureId? = null,
) = addFeature(id, SchemeTextFeature(position, text, scaleRange, color))

fun SchemeFeaturesState.text(
    position: Pair<Number, Number>,
    text: String,
    scaleRange: FloatRange = defaultScaleRange,
    color: Color = Color.Red,
    id: FeatureId? = null,
) = addFeature(id, SchemeTextFeature(position.toCoordinates(), text, scaleRange, color))

fun SchemeFeaturesState.image(
    position: Pair<Number, Number>,
    image: ImageVector,
    size: DpSize = DpSize(20.dp, 20.dp),
    scaleRange: FloatRange = defaultScaleRange,
    id: FeatureId? = null,
) = addFeature(id, SchemeImageFeature(position.toCoordinates(), image, size, scaleRange))

fun SchemeFeaturesState.group(
    scaleRange: FloatRange = defaultScaleRange,
    id: FeatureId? = null,
    builder: SchemeFeaturesState.() -> Unit,
): FeatureId {
    val groupBuilder = SchemeFeaturesState.build(builder)
    val feature = SchemeFeatureGroup(groupBuilder.features(), scaleRange)
    return addFeature(id, feature)
}