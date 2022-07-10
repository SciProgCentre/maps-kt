package centre.sciprog.maps.compose

import androidx.compose.ui.graphics.Color

//TODO replace zoom range with zoom-based representation change
sealed class MapFeature(val zoomRange: ClosedFloatingPointRange<Double>)

private val defaultRange = 0.0..18.0

private fun Pair<Double, Double>.toCoordinates() = GeodeticMapCoordinates.ofDegrees(first, second)

class MapCircleFeature(
    val center: GeodeticMapCoordinates,
    zoomRange: ClosedFloatingPointRange<Double> = defaultRange,
    val size: Float = 5f,
    val color: Color = Color.Red,
) : MapFeature(zoomRange)

fun MapCircleFeature(
    centerCoordinates: Pair<Double, Double>,
    zoomRange: ClosedFloatingPointRange<Double> = defaultRange,
    size: Float = 5f,
    color: Color = Color.Red,
) = MapCircleFeature(
    centerCoordinates.toCoordinates(),
    zoomRange,
    size,
    color
)


class MapLineFeature(
    val a: GeodeticMapCoordinates,
    val b: GeodeticMapCoordinates,
    zoomRange: ClosedFloatingPointRange<Double> = defaultRange,
    val color: Color = Color.Red,
) : MapFeature(zoomRange)

fun MapLineFeature(
    aCoordinates: Pair<Double, Double>,
    bCoordinates: Pair<Double, Double>,
    zoomRange: ClosedFloatingPointRange<Double> = defaultRange,
    color: Color = Color.Red,
) = MapLineFeature(aCoordinates.toCoordinates(), bCoordinates.toCoordinates(), zoomRange, color)