package center.sciprog.maps.compose

import androidx.compose.ui.graphics.PointMode
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import center.sciprog.maps.coordinates.*
import center.sciprog.maps.features.*


internal fun FeatureGroup<Gmc>.coordinatesOf(pair: Pair<Number, Number>) =
    GeodeticMapCoordinates.ofDegrees(pair.first.toDouble(), pair.second.toDouble())

public typealias MapFeature = Feature<Gmc>

public fun FeatureGroup<Gmc>.circle(
    centerCoordinates: Pair<Number, Number>,
    size: Dp = 5.dp,
    id: String? = null,
): FeatureId<CircleFeature<Gmc>> = feature(
    id, CircleFeature(space, coordinatesOf(centerCoordinates), size)
)

public fun FeatureGroup<Gmc>.rectangle(
    centerCoordinates: Pair<Number, Number>,
    size: DpSize = DpSize(5.dp, 5.dp),
    id: String? = null,
): FeatureId<RectangleFeature<Gmc>> = feature(
    id, RectangleFeature(space, coordinatesOf(centerCoordinates),  size)
)


public fun FeatureGroup<Gmc>.draw(
    position: Pair<Number, Number>,
    id: String? = null,
    draw: DrawScope.() -> Unit,
): FeatureId<DrawFeature<Gmc>> = feature(
    id,
    DrawFeature(space, coordinatesOf(position), drawFeature = draw)
)


public fun FeatureGroup<Gmc>.line(
    curve: GmcCurve,
    id: String? = null,
): FeatureId<LineFeature<Gmc>> = feature(
    id,
    LineFeature(space, curve.forward.coordinates, curve.backward.coordinates)
)


public fun FeatureGroup<Gmc>.line(
    aCoordinates: Pair<Double, Double>,
    bCoordinates: Pair<Double, Double>,
    id: String? = null,
): FeatureId<LineFeature<Gmc>> = feature(
    id,
    LineFeature(space, coordinatesOf(aCoordinates), coordinatesOf(bCoordinates))
)


public fun FeatureGroup<Gmc>.arc(
    center: Pair<Double, Double>,
    radius: Distance,
    startAngle: Angle,
    arcLength: Angle,
    id: String? = null,
): FeatureId<ArcFeature<Gmc>> = feature(
    id,
    ArcFeature(
        space,
        oval = space.Rectangle(coordinatesOf(center), radius, radius),
        startAngle = startAngle.radians.toFloat(),
        arcLength = arcLength.radians.toFloat(),
    )
)

public fun FeatureGroup<Gmc>.points(
    points: List<Pair<Double, Double>>,
    stroke: Float = 2f,
    pointMode: PointMode = PointMode.Points,
    id: String? = null,
): FeatureId<PointsFeature<Gmc>> =
    feature(id, PointsFeature(space, points.map(::coordinatesOf),  stroke,  pointMode))

public fun FeatureGroup<Gmc>.image(
    position: Pair<Double, Double>,
    image: ImageVector,
    size: DpSize = DpSize(20.dp, 20.dp),
    id: String? = null,
): FeatureId<VectorImageFeature<Gmc>> = feature(
    id,
    VectorImageFeature(
        space,
        coordinatesOf(position),
        size,
        image,
    )
)

public fun FeatureGroup<Gmc>.text(
    position: Pair<Double, Double>,
    text: String,
    font: FeatureFont.() -> Unit = { size = 16f },
    id: String? = null,
): FeatureId<TextFeature<Gmc>> = feature(
    id,
    TextFeature(space, coordinatesOf(position), text, fontConfig = font)
)
