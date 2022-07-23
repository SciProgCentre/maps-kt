package center.sciprog.maps.coordinates

import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min

public data class GmcBox(
    public val a: GeodeticMapCoordinates,
    public val b: GeodeticMapCoordinates,
) {
    public companion object {
        public fun withCenter(
            center: GeodeticMapCoordinates,
            width: Distance,
            height: Distance,
            ellipsoid: Ellipsoid = Ellipsoid.WGS84,
        ): GmcBox {
            val r = ellipsoid.equatorRadius * cos(center.latitude)
            val a = GeodeticMapCoordinates.ofRadians(
                center.latitude - height / ellipsoid.polarRadius / 2,
                center.longitude - width / r / 2
            )
            val b = GeodeticMapCoordinates.ofRadians(
                center.latitude + height / ellipsoid.polarRadius / 2,
                center.longitude + width / r / 2
            )
            return GmcBox(a, b)
        }
    }
}

public val GmcBox.center: GeodeticMapCoordinates
    get() = GeodeticMapCoordinates.ofRadians(
        (a.latitude + b.latitude) / 2,
        (a.longitude + b.longitude) / 2
    )

/**
 * Minimum longitude
 */
public val GmcBox.left: Double get() = min(a.longitude, b.longitude)

/**
 * maximum longitude
 */
public val GmcBox.right: Double get() = max(a.longitude, b.longitude)

/**
 * Maximum latitude
 */
public val GmcBox.top: Double get() = max(a.latitude, b.latitude)

/**
 * Minimum latitude
 */
public val GmcBox.bottom: Double get() = min(a.latitude, b.latitude)

//TODO take curvature into account
public val GmcBox.width: Double get() = abs(a.longitude - b.longitude)
public val GmcBox.height: Double get() = abs(a.latitude - b.latitude)

public val GmcBox.topLeft: GeodeticMapCoordinates get() = GeodeticMapCoordinates.ofRadians(top, left)
public val GmcBox.bottomRight: GeodeticMapCoordinates get() = GeodeticMapCoordinates.ofRadians(bottom, right)

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
    return GmcBox(GeodeticMapCoordinates.ofRadians(minLat, minLong), GeodeticMapCoordinates.ofRadians(maxLat, maxLong))
}