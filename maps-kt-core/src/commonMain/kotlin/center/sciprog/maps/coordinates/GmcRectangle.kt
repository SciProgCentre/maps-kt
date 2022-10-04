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
) {
    public companion object {

        /**
         * A quasi-square section.
         */
        public fun square(
            center: GeodeticMapCoordinates,
            height: Angle,
            width: Angle,
        ): GmcRectangle {
            val a = GeodeticMapCoordinates(
                center.latitude - (height / 2),
                center.longitude - (width / 2)
            )
            val b = GeodeticMapCoordinates(
                center.latitude + (height / 2),
                center.longitude + (width / 2)
            )
            return GmcRectangle(a, b)
        }

        /**
         * A quasi-square section. Note that latitudinal distance could be imprecise for large distances
         */
        public fun square(
            center: GeodeticMapCoordinates,
            height: Distance,
            width: Distance,
            ellipsoid: GeoEllipsoid = GeoEllipsoid.WGS84,
        ): GmcRectangle {
            val reducedRadius = ellipsoid.reducedRadius(center.latitude)
            return square(center, (height / ellipsoid.polarRadius).radians, (width / reducedRadius).radians)
        }
    }
}

public fun GmcRectangle.moveTo(newCenter: Gmc): GmcRectangle = GmcRectangle.square(newCenter, height = latitudeDelta, width = longitudeDelta)

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

//public fun GmcRectangle.enlarge(
//    top: Distance,
//    bottom: Distance = top,
//    left: Distance = top,
//    right: Distance = left,
//): GmcRectangle {
//
//}
//
//public fun GmcRectangle.enlarge(
//    top: Angle,
//    bottom: Angle = top,
//    left: Angle = top,
//    right: Angle = left,
//): GmcRectangle {
//
//}

/**
 * Check if coordinate is inside the box
 */
public operator fun GmcRectangle.contains(coordinate: Gmc): Boolean =
    coordinate.latitude in (bottom..top) && coordinate.longitude in (left..right)

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