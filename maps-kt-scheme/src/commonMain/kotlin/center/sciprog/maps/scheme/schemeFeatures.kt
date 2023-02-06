package center.sciprog.maps.scheme

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import center.sciprog.attributes.Attributes
import center.sciprog.maps.features.*
import space.kscience.kmath.geometry.Angle

internal fun Pair<Number, Number>.toCoordinates(): XY = XY(first.toFloat(), second.toFloat())

fun FeatureGroup<XY>.background(
    width: Float,
    height: Float,
    offset: XY = XY(0f, 0f),
    id: String? = null,
    painter: @Composable () -> Painter,
): FeatureRef<XY, ScalableImageFeature<XY>> {
    val box = XYRectangle(
        offset,
        XY(width + offset.x, height + offset.y)
    )
    return feature(
        id,
        ScalableImageFeature(
            space,
            box,
            painter = painter,
            attributes = Attributes(ZAttribute, -100f)
        )
    )
}

fun FeatureGroup<XY>.circle(
    centerCoordinates: Pair<Number, Number>,
    size: Dp = 5.dp,
    id: String? = null,
): FeatureRef<XY, CircleFeature<XY>> = circle(centerCoordinates.toCoordinates(),  size,  id = id)

fun FeatureGroup<XY>.draw(
    position: Pair<Number, Number>,
    id: String? = null,
    draw: DrawScope.() -> Unit,
): FeatureRef<XY, DrawFeature<XY>> = draw(position.toCoordinates(), id = id, draw = draw)

fun FeatureGroup<XY>.line(
    aCoordinates: Pair<Number, Number>,
    bCoordinates: Pair<Number, Number>,
    id: String? = null,
): FeatureRef<XY, LineFeature<XY>> = line(aCoordinates.toCoordinates(), bCoordinates.toCoordinates(), id)


public fun FeatureGroup<XY>.arc(
    center: Pair<Double, Double>,
    radius: Float,
    startAngle: Angle,
    arcLength: Angle,
    id: String? = null,
): FeatureRef<XY, ArcFeature<XY>> = arc(
    oval = XYCoordinateSpace.Rectangle(center.toCoordinates(), radius, radius),
    startAngle = startAngle,
    arcLength = arcLength,
    id = id
)

fun FeatureGroup<XY>.image(
    position: Pair<Number, Number>,
    image: ImageVector,
    size: DpSize = DpSize(image.defaultWidth, image.defaultHeight),
    id: String? = null,
): FeatureRef<XY, VectorImageFeature<XY>> =
    image(position.toCoordinates(), image, size = size, id = id)

fun FeatureGroup<XY>.text(
    position: Pair<Number, Number>,
    text: String,
    id: String? = null,
): FeatureRef<XY, TextFeature<XY>> = text(position.toCoordinates(), text, id = id)

