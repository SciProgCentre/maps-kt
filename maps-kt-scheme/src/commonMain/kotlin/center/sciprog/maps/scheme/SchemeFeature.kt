package center.sciprog.maps.scheme

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import center.sciprog.maps.scheme.SchemeFeature.Companion.defaultScaleRange

internal typealias FloatRange = ClosedFloatingPointRange<Float>

sealed class SchemeFeature(val scaleRange: FloatRange) {
    abstract fun getBoundingBox(scale: Float): SchemeCoordinateBox?

    companion object {
        val defaultScaleRange = 0f..Float.MAX_VALUE
    }
}


fun Iterable<SchemeFeature>.computeBoundingBox(scale: Float): SchemeCoordinateBox? =
    mapNotNull { it.getBoundingBox(scale) }.wrapAll()


internal fun Pair<Number, Number>.toCoordinates() = SchemeCoordinates(first.toFloat(), second.toFloat())

/**
 * A background image that is bound to scheme coordinates and is scaled together with them
 *
 * @param position the size of background in scheme size units. The screen units to scheme units ratio equals scale.
 */
class SchemeBackgroundFeature(
    val position: SchemeCoordinateBox,
    val painter: Painter,
    scaleRange: FloatRange = defaultScaleRange,
) : SchemeFeature(scaleRange) {
    override fun getBoundingBox(scale: Float): SchemeCoordinateBox = position
}

class SchemeFeatureSelector(val selector: (scale: Float) -> SchemeFeature) : SchemeFeature(defaultScaleRange) {
    override fun getBoundingBox(scale: Float): SchemeCoordinateBox? = selector(scale).getBoundingBox(scale)
}

class SchemeDrawFeature(
    val position: SchemeCoordinates,
    scaleRange: FloatRange = defaultScaleRange,
    val drawFeature: DrawScope.() -> Unit,
) : SchemeFeature(scaleRange) {
    override fun getBoundingBox(scale: Float): SchemeCoordinateBox = SchemeCoordinateBox(position, position)
}

class SchemeCircleFeature(
    val center: SchemeCoordinates,
    scaleRange: FloatRange = defaultScaleRange,
    val size: Float = 5f,
    val color: Color = Color.Red,
) : SchemeFeature(scaleRange) {
    override fun getBoundingBox(scale: Float): SchemeCoordinateBox = SchemeCoordinateBox(center, center)
}

class SchemeLineFeature(
    val a: SchemeCoordinates,
    val b: SchemeCoordinates,
    scaleRange: FloatRange = defaultScaleRange,
    val color: Color = Color.Red,
) : SchemeFeature(scaleRange) {
    override fun getBoundingBox(scale: Float): SchemeCoordinateBox = SchemeCoordinateBox(a, b)
}

class SchemeTextFeature(
    val position: SchemeCoordinates,
    val text: String,
    scaleRange: FloatRange = defaultScaleRange,
    val color: Color = Color.Red,
) : SchemeFeature(scaleRange) {
    override fun getBoundingBox(scale: Float): SchemeCoordinateBox = SchemeCoordinateBox(position, position)
}

class SchemeBitmapFeature(
    val position: SchemeCoordinates,
    val image: ImageBitmap,
    val size: IntSize = IntSize(15, 15),
    scaleRange: FloatRange = defaultScaleRange,
) : SchemeFeature(scaleRange) {
    override fun getBoundingBox(scale: Float): SchemeCoordinateBox = SchemeCoordinateBox(position, position)
}

class SchemeImageFeature(
    val position: SchemeCoordinates,
    val painter: Painter,
    val size: DpSize,
    scaleRange: FloatRange = defaultScaleRange,
) : SchemeFeature(scaleRange) {
    override fun getBoundingBox(scale: Float): SchemeCoordinateBox = SchemeCoordinateBox(position, position)
}

@Composable
fun SchemeVectorImageFeature(
    position: SchemeCoordinates,
    image: ImageVector,
    size: DpSize = DpSize(20.dp, 20.dp),
    scaleRange: FloatRange = defaultScaleRange,
): SchemeImageFeature = SchemeImageFeature(position, rememberVectorPainter(image), size, scaleRange)

/**
 * A group of other features
 */
class SchemeFeatureGroup(
    val children: Map<FeatureId, SchemeFeature>,
    scaleRange: FloatRange = defaultScaleRange,
) : SchemeFeature(scaleRange) {
    override fun getBoundingBox(scale: Float): SchemeCoordinateBox? =
        children.values.mapNotNull { it.getBoundingBox(scale) }.wrapAll()
}
