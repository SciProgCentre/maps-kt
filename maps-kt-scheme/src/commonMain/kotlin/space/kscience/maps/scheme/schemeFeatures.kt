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

/**
 * Adds a scalable background image feature to the feature builder.
 *
 * @param width The width of the background in scheme units.
 * @param height The height of the background in scheme units.
 * @param offset The top-left corner offset of the background in the scheme space. Defaults to (0f, 0f).
 * @param id Optional unique identifier for the background feature. If null, a unique identifier will be generated.
 * @param painter A composable lambda function that returns the painter for the background image.
 * @return A reference to the added ScalableImageFeature.
 */
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

/**
 * Adds a circular marker to the `FeatureBuilder` using the specified center coordinates.
 *
 * @param centerCoordinates The center position of the circle as a pair of numerical values (x, y).
 * @param size The diameter of the circle. Default value is 5.dp.
 * @param id The optional unique identifier for this circle. If not provided, a unique id is generated internally.
 * @return A reference to the created circle feature.
 */
public fun FeatureBuilder<XY>.circle(
    centerCoordinates: Pair<Number, Number>,
    size: Dp = 5.dp,
    id: String? = null,
): FeatureRef<XY, CircleFeature<XY>> = circle(centerCoordinates.toCoordinates(), size, id = id)

/**
 * Adds a drawable feature to the feature builder at the specified position.
 *
 * @param position a pair of numbers representing the x and y coordinates of the feature in the coordinate space.
 * @param id an optional unique identifier for the feature. If null, a unique id will be generated automatically.
 * @param draw a lambda defining drawing logic, executed within the [DrawScope].
 * @return a reference to the created [DrawFeature] within the feature builder.
 */
public fun FeatureBuilder<XY>.draw(
    position: Pair<Number, Number>,
    id: String? = null,
    draw: DrawScope.() -> Unit,
): FeatureRef<XY, DrawFeature<XY>> = draw(position.toCoordinates(), id = id, draw = draw)

/**
 * Creates a line feature between two specified coordinates within the `FeatureBuilder`.
 *
 * @param aCoordinates The starting coordinates of the line as a pair of numbers (x, y).
 * @param bCoordinates The ending coordinates of the line as a pair of numbers (x, y).
 * @param id The optional unique identifier for the line feature. If null, an ID is automatically generated.
 * @return A reference to the created line feature represented by `FeatureRef<XY, LineFeature<XY>>`.
 */
public fun FeatureBuilder<XY>.line(
    aCoordinates: Pair<Number, Number>,
    bCoordinates: Pair<Number, Number>,
    id: String? = null,
): FeatureRef<XY, LineFeature<XY>> = line(aCoordinates.toCoordinates(), bCoordinates.toCoordinates(), id = id)

/**
 * Adds an arc feature to the feature builder.
 *
 * @param center A pair representing the center of the arc in the coordinate space.
 * @param radius The radius of the arc.
 * @param startAngle The starting angle of the arc, measured in radians from the 3 o'clock position clockwise.
 * @param arcLength The length of the arc, measured in radians.
 * @param id An optional identifier for the arc. If null, a unique ID is generated.
 * @return A reference to the created arc feature.
 */
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

/**
 * Adds an image feature to the `FeatureBuilder`. The feature is represented as a fixed-size vector icon.
 *
 * @param position the position of the image as a pair of numbers representing X and Y coordinates.
 * @param image the vector image to display as the feature.
 * @param size the size of the image feature using Dp units, defaulting to the size specified in the `ImageVector`.
 * @param id an optional unique identifier for the feature. If null, an ID will be generated.
 * @return a reference to the created feature.
 */
public fun FeatureBuilder<XY>.image(
    position: Pair<Number, Number>,
    image: ImageVector,
    size: DpSize = DpSize(image.defaultWidth, image.defaultHeight),
    id: String? = null,
): FeatureRef<XY, VectorIconFeature<XY>> =
    icon(position.toCoordinates(), image, size = size, id = id)

/**
 * Adds a text feature to the feature builder at the specified position.
 *
 * @param position The position of the text as a pair of numbers, which will be converted to coordinates.
 * @param text The text content to be displayed.
 * @param id An optional identifier for the feature. If not provided, a unique ID will be generated.
 * @return A reference to the created text feature.
 */
public fun FeatureBuilder<XY>.text(
    position: Pair<Number, Number>,
    text: String,
    id: String? = null,
): FeatureRef<XY, TextFeature<XY>> = text(position.toCoordinates(), text, id = id)

/**
 * Creates a pixel map feature within the specified rectangular boundaries and dimensions,
 * using a builder function to define the color of each pixel.
 *
 * @param rectangle The rectangular boundary of the pixel map.
 * @param xSize The width of each pixel in the map.
 * @param ySize The height of each pixel in the map.
 * @param id An optional ID for the pixel map feature. If null, a unique ID will be generated.
 * @param builder A function that determines the color of each pixel based on its coordinates.
 * @return A reference to the created pixel map feature.
 */
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

/**
 * Creates a rectangular polygon defined by its bounding edges in a 2D coordinate space.
 *
 * @param left The x-coordinate of the left edge of the rectangle.
 * @param right The x-coordinate of the right edge of the rectangle.
 * @param bottom The y-coordinate of the bottom edge of the rectangle.
 * @param top The y-coordinate of the top edge of the rectangle.
 * @param attributes The attributes to associate with the polygon feature. Defaults to an empty set of attributes.
 * @param id An optional identifier for the feature. If null, a unique identifier will be generated.
 * @return A reference to the created polygon feature within the feature store.
 */
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

/**
 * Creates a polygon feature representing a rectangle in the coordinate space.
 *
 * @param rectangle The rectangle defined by two opposing corners.
 * @param attributes Attributes to associate with the polygon feature. Defaults to an empty attribute set.
 * @param id An optional unique identifier for the feature. If null, a unique identifier will be generated.
 * @return A reference to the created polygon feature.
 */
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


