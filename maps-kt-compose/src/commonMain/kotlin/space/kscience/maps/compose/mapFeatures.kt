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

public fun FeatureBuilder<Gmc>.circle(
    centerCoordinates: Pair<Number, Number>,
    size: Dp = 5.dp,
    id: String? = null,
): FeatureRef<Gmc, CircleFeature<Gmc>> = feature(
    id, CircleFeature(space, coordinatesOf(centerCoordinates), size)
)

public fun FeatureBuilder<Gmc>.rectangle(
    centerCoordinates: Pair<Number, Number>,
    size: DpSize = DpSize(5.dp, 5.dp),
    id: String? = null,
): FeatureRef<Gmc, RectangleFeature<Gmc>> = feature(
    id, RectangleFeature(space, coordinatesOf(centerCoordinates), size)
)


public fun FeatureBuilder<Gmc>.draw(
    position: Pair<Number, Number>,
    id: String? = null,
    draw: DrawScope.() -> Unit,
): FeatureRef<Gmc, DrawFeature<Gmc>> = feature(
    id,
    DrawFeature(space, coordinatesOf(position), drawFeature = draw)
)


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

public fun FeatureBuilder<Gmc>.geodeticLine(
    from: Gmc,
    to: Gmc,
    ellipsoid: GeoEllipsoid = GeoEllipsoid.WGS84,
    maxLineDistance: Distance = 100.kilometers,
    id: String? = null,
): FeatureRef<Gmc, Feature<Gmc>> = geodeticLine(ellipsoid.curveBetween(from, to), ellipsoid, maxLineDistance, id)

public fun FeatureBuilder<Gmc>.line(
    aCoordinates: Pair<Double, Double>,
    bCoordinates: Pair<Double, Double>,
    id: String? = null,
): FeatureRef<Gmc, LineFeature<Gmc>> = feature(
    id,
    LineFeature(space, coordinatesOf(aCoordinates), coordinatesOf(bCoordinates))
)

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

public fun FeatureBuilder<Gmc>.points(
    points: List<Pair<Double, Double>>,
    id: String? = null,
): FeatureRef<Gmc, PointsFeature<Gmc>> = feature(id, PointsFeature(space, points.map(::coordinatesOf)))

public fun FeatureBuilder<Gmc>.multiLine(
    points: List<Pair<Double, Double>>,
    id: String? = null,
): FeatureRef<Gmc, MultiLineFeature<Gmc>> = feature(id, MultiLineFeature(space, points.map(::coordinatesOf)))

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

public fun FeatureBuilder<Gmc>.text(
    position: Pair<Double, Double>,
    text: String,
    font: Font.() -> Unit = { size = 16f },
    id: String? = null,
): FeatureRef<Gmc, TextFeature<Gmc>> = feature(
    id,
    TextFeature(space, coordinatesOf(position), text, fontConfig = font)
)

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
