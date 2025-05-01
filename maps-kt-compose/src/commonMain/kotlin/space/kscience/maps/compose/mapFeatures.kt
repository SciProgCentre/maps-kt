package space.kscience.maps.compose

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import org.jetbrains.skia.Font
import space.kscience.kmath.geometry.Angle
import space.kscience.maps.coordinates.*
import space.kscience.maps.features.*
import kotlin.math.ceil


internal fun FeatureBuilder<Gmc>.coordinatesOf(pair: Pair<Number, Number>) =
    GeodeticMapCoordinates.ofDegrees(pair.first.toDouble(), pair.second.toDouble())

public typealias MapFeature = Feature<Gmc>

/**
 * Adds a circle feature to the feature builder.
 *
 * @param centerCoordinates The geodetic map coordinates (latitude, longitude) of the circle's center as a pair of numbers.
 * @param size The radius of the circle as a Dp value. Defaults to 5.dp.
 * @param id An optional unique identifier for the circle feature. If null, an ID is automatically generated.
 * @return A reference to the created circle feature within the feature store.
 */
public fun FeatureBuilder<Gmc>.circle(
    centerCoordinates: Pair<Number, Number>,
    size: Dp = 5.dp,
    id: String? = null,
): FeatureRef<Gmc, CircleFeature<Gmc>> = feature(
    id, CircleFeature(space, coordinatesOf(centerCoordinates), size)
)

/**
 * Adds a rectangular feature to the map with the specified center coordinates and size.
 *
 * @param centerCoordinates The center coordinates of the rectangle as a pair of latitude and longitude values.
 * @param size The size of the rectangle, with a default value of 5.dp x 5.dp.
 * @param id An optional identifier for the rectangle. If null, a unique ID is generated automatically.
 * @return A reference to the created rectangle feature.
 */
public fun FeatureBuilder<Gmc>.rectangle(
    centerCoordinates: Pair<Number, Number>,
    size: DpSize = DpSize(5.dp, 5.dp),
    id: String? = null,
): FeatureRef<Gmc, RectangleFeature<Gmc>> = feature(
    id, RectangleFeature(space, coordinatesOf(centerCoordinates), size)
)


/**
 * Draws a feature on the map at the specified position.
 *
 * @param position The geographical coordinates as a pair of numbers (latitude, longitude) where the feature will be drawn.
 * @param id Optional identifier for the feature. If null, a unique identifier will be generated.
 * @param draw The drawing logic defined within the scope of the [DrawScope].
 * @return A reference to the newly created draw feature.
 */
public fun FeatureBuilder<Gmc>.draw(
    position: Pair<Number, Number>,
    id: String? = null,
    draw: DrawScope.() -> Unit,
): FeatureRef<Gmc, DrawFeature<Gmc>> = feature(
    id,
    DrawFeature(space, coordinatesOf(position), drawFeature = draw)
)


/**
 * Adds a line feature to the feature builder using the specified curve and optional identifier.
 *
 * @param curve the geodetic curve defining the start and end points of the line
 * @param id the optional identifier for the line feature; if null, an identifier is automatically generated
 * @return a reference to the created line feature
 */
public fun FeatureBuilder<Gmc>.line(
    curve: GmcCurve,
    id: String? = null,
): FeatureRef<Gmc, LineFeature<Gmc>> = feature(
    id,
    LineFeature(space, curve.forward.coordinates, curve.backward.coordinates)
)

/**
 * A segmented geodetic curve
 */
public fun FeatureBuilder<Gmc>.geodeticLine(
    curve: GmcCurve,
    ellipsoid: GeoEllipsoid = GeoEllipsoid.WGS84,
    maxLineDistance: Distance = 100.kilometers,
    id: String? = null,
): FeatureRef<Gmc, Feature<Gmc>> = if (curve.distance < maxLineDistance) {
    feature(
        id,
        LineFeature(space, curve.forward.coordinates, curve.backward.coordinates)
    )
} else {
    val segments = ceil(curve.distance / maxLineDistance).toInt()
    val segmentSize = curve.distance / segments
    val points = buildList<GmcPose> {
        add(curve.forward)
        repeat(segments) {
            val segment = ellipsoid.curveInDirection(this.last(), segmentSize, 1e-2)
            add(segment.backward)
        }
    }
    multiLine(points.map { it.coordinates }, id = id)
}

/**
 * Creates a segmented geodetic line between two geodetic coordinates. The line is calculated
 * based on the given ellipsoid and segmented into smaller parts if its total length exceeds the
 * specified maximum line segment distance.
 *
 * @param from The starting geodetic coordinate for the line.
 * @param to The ending geodetic coordinate for the line.
 * @param ellipsoid The reference ellipsoid used for calculations. Defaults to `GeoEllipsoid.WGS84`.
 * @param maxLineDistance The maximum allowed distance for a single line segment. Defaults to 100 kilometers.
 * @param id An optional unique identifier for the resulting feature. If null, a unique ID is generated.
 * @return A reference to the created geodetic line feature.
 */
public fun FeatureBuilder<Gmc>.geodeticLine(
    from: Gmc,
    to: Gmc,
    ellipsoid: GeoEllipsoid = GeoEllipsoid.WGS84,
    maxLineDistance: Distance = 100.kilometers,
    id: String? = null,
): FeatureRef<Gmc, Feature<Gmc>> = geodeticLine(ellipsoid.curveBetween(from, to), ellipsoid, maxLineDistance, id)

/**
 * Creates a `LineFeature` with the specified start and end coordinates and adds it to the feature store.
 *
 * @param aCoordinates The coordinates of the starting point of the line as a pair of latitude and longitude.
 * @param bCoordinates The coordinates of the ending point of the line as a pair of latitude and longitude.
 * @param id An optional unique identifier for the line feature. If null, a unique id will be generated.
 * @return A reference to the created `LineFeature`.
 */
public fun FeatureBuilder<Gmc>.line(
    aCoordinates: Pair<Double, Double>,
    bCoordinates: Pair<Double, Double>,
    id: String? = null,
): FeatureRef<Gmc, LineFeature<Gmc>> = feature(
    id,
    LineFeature(space, coordinatesOf(aCoordinates), coordinatesOf(bCoordinates))
)

/**
 * Adds an arc-shaped feature to the feature builder.
 *
 * @param center the geographical center of the arc represented as a pair of latitude and longitude in degrees
 * @param radius the radius of the arc in kilometers
 * @param startAngle the starting angle of the arc in radians, measured from the 3 o'clock position downwards
 * @param arcLength the angular extent of the arc in radians
 * @param id an optional identifier for the arc feature; if null, a unique identifier is generated
 * @return a reference to the added arc feature
 */
public fun FeatureBuilder<Gmc>.arc(
    center: Pair<Double, Double>,
    radius: Distance,
    startAngle: Angle,
    arcLength: Angle,
    id: String? = null,
): FeatureRef<Gmc, ArcFeature<Gmc>> = feature(
    id,
    ArcFeature(
        space,
        oval = space.Rectangle(coordinatesOf(center), radius, radius),
        startAngle = startAngle,
        arcLength = arcLength,
    )
)

/**
 * Adds a points feature to the current feature builder.
 *
 * @param points a list of pairs where each pair represents the latitude and longitude of a point.
 * @param id an optional unique identifier for the feature. If null, a unique id is generated automatically.
 * @return a reference to the created points feature in the feature store.
 */
public fun FeatureBuilder<Gmc>.points(
    points: List<Pair<Double, Double>>,
    id: String? = null,
): FeatureRef<Gmc, PointsFeature<Gmc>> = feature(id, PointsFeature(space, points.map(::coordinatesOf)))

/**
 * Creates a MultiLineFeature composed of multiple line segments defined by the given points
 * and adds it to the feature store. If an ID is provided, the feature is associated with that ID;
 * otherwise, a unique ID is generated.
 *
 * @param points A list of pairs representing the points (latitude, longitude) that define the line segments.
 * @param id An optional string identifier for the feature. If null, a unique ID is automatically created.
 * @return A reference to the created MultiLineFeature.
 */
public fun FeatureBuilder<Gmc>.multiLine(
    points: List<Pair<Double, Double>>,
    id: String? = null,
): FeatureRef<Gmc, MultiLineFeature<Gmc>> = feature(id, MultiLineFeature(space, points.map(::coordinatesOf)))

/**
 * Adds a vector icon feature to the map with a specified position, size, and image.
 *
 * @param position The geographic coordinates (latitude, longitude) of the icon's center.
 * @param image The image to be displayed as the vector icon.
 * @param size The size of the icon, specified as a [DpSize]. Defaults to 20.dp x 20.dp.
 * @param id An optional unique identifier for the icon. If null, a unique ID is generated automatically.
 * @return A reference to the created vector icon feature as a [FeatureRef].
 */
public fun FeatureBuilder<Gmc>.icon(
    position: Pair<Double, Double>,
    image: ImageVector,
    size: DpSize = DpSize(20.dp, 20.dp),
    id: String? = null,
): FeatureRef<Gmc, VectorIconFeature<Gmc>> = feature(
    id,
    VectorIconFeature(
        space,
        coordinatesOf(position),
        size,
        image,
    )
)

/**
 * Adds a text feature to the FeatureBuilder with the specified position, content, and font configuration.
 *
 * @param position The geographic position of the text as a pair of latitude and longitude.
 * @param text The text content to be displayed.
 * @param font A lambda to configure text font properties. Default size is 16f.
 * @param id An optional identifier for the feature. If null, a unique ID is generated.
 * @return A reference to the added TextFeature within the FeatureBuilder.
 */
public fun FeatureBuilder<Gmc>.text(
    position: Pair<Double, Double>,
    text: String,
    font: Font.() -> Unit = { size = 16f },
    id: String? = null,
): FeatureRef<Gmc, TextFeature<Gmc>> = feature(
    id,
    TextFeature(space, coordinatesOf(position), text, fontConfig = font)
)

/**
 * Creates a pixel map feature within the specified geographic rectangle, using the provided latitude and longitude deltas to
 * define pixel granularity and a builder function to determine pixel colors.
 *
 * @param rectangle The geographic rectangle defining the bounds of the pixel map.
 * @param latitudeDelta The latitude increment between two rows of pixels.
 * @param longitudeDelta The longitude increment between two columns of pixels.
 * @param id The optional identifier for the feature. If null, a unique ID will be generated.
 * @param builder A function that takes [Gmc] coordinates and returns the color for the corresponding pixel, or null for transparency.
 * @return A reference to the pixel map feature, encapsulating its boundaries and content.
 */
public fun FeatureBuilder<Gmc>.pixelMap(
    rectangle: Rectangle<Gmc>,
    latitudeDelta: Angle,
    longitudeDelta: Angle,
    id: String? = null,
    builder: (Gmc) -> Color?,
): FeatureRef<Gmc, PixelMapFeature<Gmc>> = feature(
    id,
    PixelMapFeature(
        space,
        rectangle,
        Structure2D(
            ceil(rectangle.longitudeDelta / latitudeDelta).toInt(),
            ceil(rectangle.latitudeDelta / longitudeDelta).toInt()

        ) { (i, j) ->
            val longitude = rectangle.left + longitudeDelta * i
            val latitude = rectangle.bottom + latitudeDelta * j
            builder(
                Gmc(latitude, longitude)
            )
        }
    )
)
