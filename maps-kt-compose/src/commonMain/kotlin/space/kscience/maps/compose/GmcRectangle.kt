package space.kscience.maps.compose

import space.kscience.kmath.geometry.Angle
import space.kscience.kmath.geometry.abs
import space.kscience.maps.coordinates.GeodeticMapCoordinates
import space.kscience.maps.coordinates.Gmc
import space.kscience.maps.features.Rectangle

internal fun Angle.isBetween(a: Angle, b: Angle) = this in a..b || this in b..a

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
    override val center: GeodeticMapCoordinates
        get() = GeodeticMapCoordinates.normalized(
            (a.latitude + b.latitude) / 2,
            (a.longitude + b.longitude) / 2
        )


    override fun contains(point: Gmc): Boolean =
        point.latitude.isBetween(a.latitude, b.latitude) && point.longitude.isBetween(a.longitude, b.longitude)
}

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