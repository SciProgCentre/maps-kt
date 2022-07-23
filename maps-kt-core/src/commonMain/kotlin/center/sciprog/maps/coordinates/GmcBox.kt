package center.sciprog.maps.coordinates

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

public data class GmcBox(
    public val a: GeodeticMapCoordinates,
    public val b: GeodeticMapCoordinates,
)

public fun GmcBox(
    latitudes: ClosedFloatingPointRange<Double>,
    longitudes: ClosedFloatingPointRange<Double>,
): GmcBox = GmcBox(
    GeodeticMapCoordinates.ofRadians(latitudes.start, longitudes.start),
    GeodeticMapCoordinates.ofRadians(latitudes.endInclusive, longitudes.endInclusive)
)

public val GmcBox.center: GeodeticMapCoordinates
    get() = GeodeticMapCoordinates.ofRadians(
        (a.latitude + b.latitude) / 2,
        (a.longitude + b.longitude) / 2
    )

public val GmcBox.left: Double get() = min(a.longitude, b.longitude)
public val GmcBox.right: Double get() = max(a.longitude, b.longitude)

public val GmcBox.top: Double get() = max(a.latitude, b.latitude)
public val GmcBox.bottom: Double get() = min(a.latitude, b.latitude)

//TODO take curvature into account
public val GmcBox.width: Double get() = abs(a.longitude - b.longitude)
public val GmcBox.height: Double get() = abs(a.latitude - b.latitude)

/**
 * Compute a minimal bounding box including all given boxes. Return null if collection is empty
 */
public fun Collection<GmcBox>.wrapAll(): GmcBox? {
    if (isEmpty()) return null
    //TODO optimize computation
    val minLat = minOf { it.bottom }
    val maxLat = maxOf { it.top }
    val minLong = minOf { it.left }
    val maxLong = maxOf { it.right }
    return GmcBox(minLat..maxLat, minLong..maxLong)
}