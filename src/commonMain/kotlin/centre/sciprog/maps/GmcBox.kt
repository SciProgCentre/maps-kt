package centre.sciprog.maps

import kotlin.math.max
import kotlin.math.min

class GmcBox(val a: GeodeticMapCoordinates, val b: GeodeticMapCoordinates)

fun GmcBox(latitudes: ClosedFloatingPointRange<Double>, longitudes: ClosedFloatingPointRange<Double>) = GmcBox(
    Gmc.ofRadians(latitudes.start, longitudes.start),
    Gmc.ofRadians(latitudes.endInclusive, longitudes.endInclusive)
)

val GmcBox.center
    get() = GeodeticMapCoordinates.ofRadians(
        (a.latitude + b.latitude) / 2,
        (a.longitude + b.longitude) / 2
    )

val GmcBox.left get() = min(a.longitude, b.longitude)
val GmcBox.right get() = max(a.longitude, b.longitude)

val GmcBox.top get() = max(a.latitude, b.latitude)
val GmcBox.bottom get() = min(a.latitude, b.latitude)

/**
 * Compute a minimal bounding box including all given boxes
 */
fun Iterable<GmcBox>.wrapAll(): GmcBox {
    //TODO optimize computation
    val minLat = minOf { it.bottom }
    val maxLat = maxOf { it.top }
    val minLong = minOf { it.left }
    val maxLong = maxOf { it.right }
    return GmcBox(maxLat..maxLat, minLong..maxLong)
}