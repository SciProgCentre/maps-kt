package center.sciprog.maps.scheme

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import center.sciprog.maps.features.*

internal fun Pair<Number, Number>.toCoordinates(): XY = XY(first.toFloat(), second.toFloat())

fun FeaturesState<XY>.background(
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
    return scalableImage(box, id = id, painter = painter)
}

fun FeaturesState<XY>.circle(
    centerCoordinates: Pair<Number, Number>,
    zoomRange: FloatRange = Feature.defaultZoomRange,
    size: Dp = 5.dp,
    color: Color = Color.Red,
    id: String? = null,
): FeatureId<CircleFeature<XY>> = circle(centerCoordinates.toCoordinates(), zoomRange, size, color, id = id)

fun FeaturesState<XY>.draw(
    position: Pair<Number, Number>,
    zoomRange: FloatRange = Feature.defaultZoomRange,
    id: String? = null,
    draw: DrawScope.() -> Unit,
): FeatureId<DrawFeature<XY>> = draw(position.toCoordinates(), zoomRange = zoomRange, id = id, draw = draw)

fun FeaturesState<XY>.line(
    aCoordinates: Pair<Number, Number>,
    bCoordinates: Pair<Number, Number>,
    scaleRange: FloatRange = Feature.defaultZoomRange,
    color: Color = Color.Red,
    id: String? = null,
): FeatureId<LineFeature<XY>> = line(aCoordinates.toCoordinates(), bCoordinates.toCoordinates(), scaleRange, color, id)


public fun FeaturesState<XY>.arc(
    center: Pair<Double, Double>,
    radius: Float,
    startAngle: Float,
    arcLength: Float,
    zoomRange: FloatRange = Feature.defaultZoomRange,
    color: Color = Color.Red,
    id: String? = null,
): FeatureId<ArcFeature<XY>> = arc(
    oval = XYCoordinateSpace.Rectangle(center.toCoordinates(), radius, radius),
    startAngle = startAngle,
    arcLength = arcLength,
    zoomRange = zoomRange,
    color = color
)

fun FeaturesState<XY>.image(
    position: Pair<Number, Number>,
    image: ImageVector,
    size: DpSize = DpSize(image.defaultWidth, image.defaultHeight),
    zoomRange: FloatRange = Feature.defaultZoomRange,
    id: String? = null,
): FeatureId<VectorImageFeature<XY>> =
    image(position.toCoordinates(), image, size = size, zoomRange = zoomRange, id = id)

fun FeaturesState<XY>.text(
    position: Pair<Number, Number>,
    text: String,
    zoomRange: FloatRange = Feature.defaultZoomRange,
    color: Color = Color.Red,
    id: String? = null,
): FeatureId<TextFeature<XY>> = text(position.toCoordinates(), text, zoomRange, color, id = id)

