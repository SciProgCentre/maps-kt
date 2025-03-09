package space.kscience.maps.scheme

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import space.kscience.attributes.Attributes
import space.kscience.kmath.geometry.Angle
import space.kscience.maps.features.*
import kotlin.math.ceil

internal fun Pair<Number, Number>.toCoordinates(): XY = XY(first.toFloat(), second.toFloat())

public fun FeatureBuilder<XY>.background(
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

public fun FeatureBuilder<XY>.circle(
    centerCoordinates: Pair<Number, Number>,
    size: Dp = 5.dp,
    id: String? = null,
): FeatureRef<XY, CircleFeature<XY>> = circle(centerCoordinates.toCoordinates(), size, id = id)

public fun FeatureBuilder<XY>.draw(
    position: Pair<Number, Number>,
    id: String? = null,
    draw: DrawScope.() -> Unit,
): FeatureRef<XY, DrawFeature<XY>> = draw(position.toCoordinates(), id = id, draw = draw)

public fun FeatureBuilder<XY>.line(
    aCoordinates: Pair<Number, Number>,
    bCoordinates: Pair<Number, Number>,
    id: String? = null,
): FeatureRef<XY, LineFeature<XY>> = line(aCoordinates.toCoordinates(), bCoordinates.toCoordinates(), id = id)


public fun FeatureBuilder<XY>.arc(
    center: Pair<Double, Double>,
    radius: Float,
    startAngle: Angle,
    arcLength: Angle,
    id: String? = null,
): FeatureRef<XY, ArcFeature<XY>> = arc(
    oval = XYCoordinateSpace.Rectangle(center.toCoordinates(), 2 * radius, 2 * radius),
    startAngle = startAngle,
    arcLength = arcLength,
    id = id
)

public fun FeatureBuilder<XY>.image(
    position: Pair<Number, Number>,
    image: ImageVector,
    size: DpSize = DpSize(image.defaultWidth, image.defaultHeight),
    id: String? = null,
): FeatureRef<XY, VectorIconFeature<XY>> =
    icon(position.toCoordinates(), image, size = size, id = id)

public fun FeatureBuilder<XY>.text(
    position: Pair<Number, Number>,
    text: String,
    id: String? = null,
): FeatureRef<XY, TextFeature<XY>> = text(position.toCoordinates(), text, id = id)

public fun FeatureBuilder<XY>.pixelMap(
    rectangle: Rectangle<XY>,
    xSize: Float,
    ySize: Float,
    id: String? = null,
    builder: (XY) -> Color?,
): FeatureRef<XY, PixelMapFeature<XY>> = feature(
    id,
    PixelMapFeature(
        space,
        rectangle,
        Structure2D(
            ceil(rectangle.width / xSize).toInt(),
            ceil(rectangle.height / ySize).toInt()

        ) { (i, j) ->
            val longitude = rectangle.left + xSize * i
            val latitude = rectangle.bottom + ySize * j
            builder(
                XY(latitude, longitude)
            )
        }
    )
)

public fun FeatureBuilder<XY>.rectanglePolygon(
    left: Number, right: Number,
    bottom: Number, top: Number,
    attributes: Attributes = Attributes.EMPTY,
    id: String? = null,
): FeatureRef<XY, PolygonFeature<XY>> = polygon(
    listOf(
        XY(left.toFloat(), top.toFloat()),
        XY(right.toFloat(), top.toFloat()),
        XY(right.toFloat(), bottom.toFloat()),
        XY(left.toFloat(), bottom.toFloat())
    ),
    attributes, id
)

public fun FeatureBuilder<XY>.rectanglePolygon(
    rectangle: Rectangle<XY>,
    attributes: Attributes = Attributes.EMPTY,
    id: String? = null,
): FeatureRef<XY, PolygonFeature<XY>> = polygon(
    listOf(
        XY(rectangle.left, rectangle.top),
        XY(rectangle.right, rectangle.top),
        XY(rectangle.right, rectangle.bottom),
        XY(rectangle.left, rectangle.bottom)
    ),
    attributes, id
)


