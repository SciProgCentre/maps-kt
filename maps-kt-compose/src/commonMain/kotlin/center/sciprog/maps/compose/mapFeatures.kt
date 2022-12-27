package center.sciprog.maps.compose

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PointMode
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import center.sciprog.maps.coordinates.*
import center.sciprog.maps.features.*


internal fun FeatureBuilder<Gmc>.coordinatesOf(pair: Pair<Number, Number>) =
    GeodeticMapCoordinates.ofDegrees(pair.first.toDouble(), pair.second.toDouble())

public typealias MapFeature = Feature<Gmc>

public fun FeatureBuilder<Gmc>.circle(
    centerCoordinates: Pair<Number, Number>,
    zoomRange: DoubleRange = defaultZoomRange,
    size: Dp = 5.dp,
    color: Color = defaultColor,
    id: String? = null,
): FeatureId<CircleFeature<Gmc>> = feature(
    id, CircleFeature(coordinateSpace, coordinatesOf(centerCoordinates), zoomRange, size, color)
)

public fun FeatureBuilder<Gmc>.rectangle(
    centerCoordinates: Pair<Number, Number>,
    zoomRange: DoubleRange = defaultZoomRange,
    size: DpSize = DpSize(5.dp, 5.dp),
    color: Color = defaultColor,
    id: String? = null,
): FeatureId<RectangleFeature<Gmc>> = feature(
    id, RectangleFeature(coordinateSpace, coordinatesOf(centerCoordinates), zoomRange, size, color)
)


public fun FeatureBuilder<Gmc>.draw(
    position: Pair<Number, Number>,
    zoomRange: DoubleRange = defaultZoomRange,
    id: String? = null,
    draw: DrawScope.() -> Unit,
): FeatureId<DrawFeature<Gmc>> = feature(
    id,
    DrawFeature(coordinateSpace, coordinatesOf(position), zoomRange, drawFeature = draw)
)


public fun FeatureBuilder<Gmc>.line(
    curve: GmcCurve,
    zoomRange: DoubleRange = defaultZoomRange,
    color: Color = defaultColor,
    id: String? = null,
): FeatureId<LineFeature<Gmc>> = feature(
    id,
    LineFeature(coordinateSpace, curve.forward.coordinates, curve.backward.coordinates, zoomRange, color)
)


public fun FeatureBuilder<Gmc>.line(
    aCoordinates: Pair<Double, Double>,
    bCoordinates: Pair<Double, Double>,
    zoomRange: DoubleRange = defaultZoomRange,
    color: Color = defaultColor,
    id: String? = null,
): FeatureId<LineFeature<Gmc>> = feature(
    id,
    LineFeature(coordinateSpace, coordinatesOf(aCoordinates), coordinatesOf(bCoordinates), zoomRange, color)
)


public fun FeatureBuilder<Gmc>.arc(
    center: Pair<Double, Double>,
    radius: Distance,
    startAngle: Angle,
    arcLength: Angle,
    zoomRange: DoubleRange = defaultZoomRange,
    color: Color = defaultColor,
    id: String? = null,
): FeatureId<ArcFeature<Gmc>> = feature(
    id,
    ArcFeature(
        coordinateSpace,
        oval = coordinateSpace.Rectangle(coordinatesOf(center), radius, radius),
        startAngle = startAngle.radians.toFloat(),
        arcLength = arcLength.radians.toFloat(),
        zoomRange = zoomRange,
        color = color
    )
)

public fun FeatureBuilder<Gmc>.points(
    points: List<Pair<Double, Double>>,
    zoomRange: DoubleRange = defaultZoomRange,
    stroke: Float = 2f,
    color: Color = defaultColor,
    pointMode: PointMode = PointMode.Points,
    id: String? = null,
): FeatureId<PointsFeature<Gmc>> =
    feature(id, PointsFeature(coordinateSpace, points.map(::coordinatesOf), zoomRange, stroke, color, pointMode))

public fun FeatureBuilder<Gmc>.image(
    position: Pair<Double, Double>,
    image: ImageVector,
    size: DpSize = DpSize(20.dp, 20.dp),
    zoomRange: DoubleRange = defaultZoomRange,
    id: String? = null,
): FeatureId<VectorImageFeature<Gmc>> = feature(
    id,
    VectorImageFeature(
        coordinateSpace,
        coordinatesOf(position),
        size,
        image,
        zoomRange
    )
)

public fun FeatureBuilder<Gmc>.text(
    position: Pair<Double, Double>,
    text: String,
    zoomRange: DoubleRange = defaultZoomRange,
    color: Color = defaultColor,
    font: FeatureFont.() -> Unit = { size = 16f },
    id: String? = null,
): FeatureId<TextFeature<Gmc>> = feature(
    id,
    TextFeature(coordinateSpace, coordinatesOf(position), text, zoomRange, color, fontConfig = font)
)
