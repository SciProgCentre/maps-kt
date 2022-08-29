package center.sciprog.maps.coordinates

import kotlin.math.acos
import kotlin.math.pow
import kotlin.math.sqrt


public class GeoEllipsoid(public val equatorRadius: Distance, public val polarRadius: Distance) {

    /**
     * Flattening https://en.wikipedia.org/wiki/Flattening
     */
    public val f: Double = (equatorRadius.kilometers - polarRadius.kilometers) / equatorRadius.kilometers

    /**
     * Inverse flattening
     */
    public val inverseF: Double = equatorRadius.kilometers / (equatorRadius.kilometers - polarRadius.kilometers)

    public val eSquared: Double = 2 * f - f * f

    public companion object {

        public val WGS84: GeoEllipsoid = GeoEllipsoid(
            equatorRadius = Distance(6378.137),
            polarRadius = Distance(6356.752314245)
        )

        public val GRS80: GeoEllipsoid = GeoEllipsoid(
            equatorRadius = Distance(6378.137),
            polarRadius = Distance(6356.752314140)
        )

        public val sphere: GeoEllipsoid = GeoEllipsoid(
            equatorRadius = Distance(6378.137),
            polarRadius = Distance(6378.137)
        )

//        /**
//         * https://en.wikipedia.org/wiki/Great-circle_distance
//         */
//        public fun greatCircleAngleBetween(r1: GMC, r2: GMC): Radians = acos(
//            sin(r1.latitude) * sin(r2.latitude) + cos(r1.latitude) * cos(r2.latitude) * cos(r1.longitude - r2.longitude)
//        ).radians
    }
}

/**
 * A radius of circle normal to the axis of the ellipsoid at given latitude
 */
internal fun GeoEllipsoid.reducedRadius(latitude: Angle): Distance {
    val reducedLatitudeTan = (1 - f) * tan(latitude)
    return equatorRadius / sqrt(1.0 + reducedLatitudeTan.pow(2))
}
//
//
///**
// * Compute distance between two map points using giv
// * https://en.wikipedia.org/wiki/Geographical_distance#Lambert's_formula_for_long_lines
// */
//public fun GeoEllipsoid.lambertDistanceBetween(r1: GMC, r2: GMC): Distance {
//    val s = greatCircleAngleBetween(r1, r2)
//
//    val b1: Double = (1 - f) * tan(r1.latitude)
//    val b2: Double = (1 - f) * tan(r2.latitude)
//    val p = (b1 + b2) / 2
//    val q = (b2 - b1) / 2
//
//    val x = (s.value - sin(s)) * sin(p).pow(2) * cos(q).pow(2) / cos(s / 2).pow(2)
//    val y = (s.value + sin(s)) * cos(p).pow(2) * sin(q).pow(2) / sin(s / 2).pow(2)
//
//    return equatorRadius * (s.value - f / 2 * (x + y))
//}