package center.sciprog.maps.scheme

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import center.sciprog.attributes.Attributes
import center.sciprog.attributes.ZAttribute
import center.sciprog.maps.features.*

internal fun Pair<Number, Number>.toCoordinates(): XY = XY(first.toFloat(), second.toFloat())

fun FeatureGroup<XY>.background(
    width: Float,
    height: Float,
    offset: XY = XY(0f, 0f),
    id: String? = null,
    painter: @Composable () -> Painter,
): FeatureId<ScalableImageFeature<XY>> {
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
): FeatureId<CircleFeature<XY>> = circle(centerCoordinates.toCoordinates(),  size,  id = id)

fun FeatureGroup<XY>.draw(
    position: Pair<Number, Number>,
    id: String? = null,
    draw: DrawScope.() -> Unit,
): FeatureId<DrawFeature<XY>> = draw(position.toCoordinates(), id = id, draw = draw)

fun FeatureGroup<XY>.line(
    aCoordinates: Pair<Number, Number>,
    bCoordinates: Pair<Number, Number>,
    id: String? = null,
): FeatureId<LineFeature<XY>> = line(aCoordinates.toCoordinates(), bCoordinates.toCoordinates(), id)


public fun FeatureGroup<XY>.arc(
    center: Pair<Double, Double>,
    radius: Float,
    startAngle: Float,
    arcLength: Float,
    id: String? = null,
): FeatureId<ArcFeature<XY>> = arc(
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
): FeatureId<VectorImageFeature<XY>> =
    image(position.toCoordinates(), image, size = size, id = id)

fun FeatureGroup<XY>.text(
    position: Pair<Number, Number>,
    text: String,
    id: String? = null,
): FeatureId<TextFeature<XY>> = text(position.toCoordinates(), text, id = id)

