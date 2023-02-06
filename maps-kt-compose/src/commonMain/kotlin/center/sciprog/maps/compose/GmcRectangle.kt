package center.sciprog.maps.compose

import center.sciprog.maps.coordinates.GeodeticMapCoordinates
import center.sciprog.maps.coordinates.Gmc
import center.sciprog.maps.features.Rectangle
import space.kscience.kmath.geometry.Angle
import space.kscience.kmath.geometry.abs

/**
 * A section of the map between two parallels and two meridians. The figure represents a square in a Mercator projection.
 * Params are two opposing "corners" of  quasi-square.
 *
 * Note that this is a rectangle only on a Mercator projection.
 */
internal data class GmcRectangle(
    override val a: GeodeticMapCoordinates,
    override val b: GeodeticMapCoordinates,
) : Rectangle<Gmc> {

    override fun contains(point: Gmc): Boolean =
        point.latitude in a.latitude..b.latitude
                && point.longitude in a.longitude..b.longitude
}

public val Rectangle<Gmc>.center: GeodeticMapCoordinates
    get() = GeodeticMapCoordinates.normalized(
        (a.latitude + b.latitude) / 2,
        (a.longitude + b.longitude) / 2
    )

/**
 * Minimum longitude
 */
public val Rectangle<Gmc>.left: Angle get() = minOf(a.longitude, b.longitude)

/**
 * maximum longitude
 */
public val Rectangle<Gmc>.right: Angle get() = maxOf(a.longitude, b.longitude)

/**
 * Maximum latitude
 */
public val Rectangle<Gmc>.top: Angle get() = maxOf(a.latitude, b.latitude)

/**
 * Minimum latitude
 */
public val Rectangle<Gmc>.bottom: Angle get() = minOf(a.latitude, b.latitude)

public val Rectangle<Gmc>.longitudeDelta: Angle get() = abs(a.longitude - b.longitude)
public val Rectangle<Gmc>.latitudeDelta: Angle get() = abs(a.latitude - b.latitude)

public val Rectangle<Gmc>.topLeft: Gmc get() = Gmc.normalized(top, left)
public val Rectangle<Gmc>.bottomRight: Gmc get() = Gmc.normalized(bottom, right)

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