package center.sciprog.maps.coordinates

/**
 * A section of the map between two parallels and two meridians. The figure represents a square in a Mercator projection.
 * Params are two opposing "corners" of  quasi-square.
 *
 * Note that this is a rectangle only on a Mercator projection.
 */
public data class GmcRectangle(
    public val a: GeodeticMapCoordinates,
    public val b: GeodeticMapCoordinates,
    public val ellipsoid: GeoEllipsoid = GeoEllipsoid.WGS84,
) {
    public companion object {

        /**
         * A quasi-square section. Note that latitudinal distance could be imprecise for large distances
         */
        public fun square(
            center: GeodeticMapCoordinates,
            width: Distance,
            height: Distance,
            ellipsoid: GeoEllipsoid = GeoEllipsoid.WGS84,
        ): GmcRectangle {
            val reducedRadius = ellipsoid.reducedRadius(center.latitude)
            val a = GeodeticMapCoordinates(
                center.latitude - (height / ellipsoid.polarRadius / 2).radians,
                center.longitude - (width / reducedRadius / 2).radians
            )
            val b = GeodeticMapCoordinates(
                center.latitude + (height / ellipsoid.polarRadius / 2).radians,
                center.longitude + (width / reducedRadius / 2).radians
            )
            return GmcRectangle(a, b, ellipsoid)
        }
    }
}

public val GmcRectangle.center: GeodeticMapCoordinates
    get() = GeodeticMapCoordinates(
        (a.latitude + b.latitude) / 2,
        (a.longitude + b.longitude) / 2
    )

/**
 * Minimum longitude
 */
public val GmcRectangle.left: Angle get() = minOf(a.longitude, b.longitude)

/**
 * maximum longitude
 */
public val GmcRectangle.right: Angle get() = maxOf(a.longitude, b.longitude)

/**
 * Maximum latitude
 */
public val GmcRectangle.top: Angle get() = maxOf(a.latitude, b.latitude)

/**
 * Minimum latitude
 */
public val GmcRectangle.bottom: Angle get() = minOf(a.latitude, b.latitude)

public val GmcRectangle.longitudeDelta: Angle get() = abs(a.longitude - b.longitude)
public val GmcRectangle.latitudeDelta: Angle get() = abs(a.latitude - b.latitude)

public val GmcRectangle.topLeft: GeodeticMapCoordinates get() = GeodeticMapCoordinates(top, left)
public val GmcRectangle.bottomRight: GeodeticMapCoordinates get() = GeodeticMapCoordinates(bottom, right)

/**
 * Compute a minimal bounding box including all given boxes. Return null if collection is empty
 */
public fun Collection<GmcRectangle>.wrapAll(): GmcRectangle? {
    if (isEmpty()) return null
    //TODO optimize computation
    val minLat = minOf { it.bottom }
    val maxLat = maxOf { it.top }
    val minLong = minOf { it.left }
    val maxLong = maxOf { it.right }
    return GmcRectangle(GeodeticMapCoordinates(minLat, minLong), GeodeticMapCoordinates(maxLat, maxLong))
}