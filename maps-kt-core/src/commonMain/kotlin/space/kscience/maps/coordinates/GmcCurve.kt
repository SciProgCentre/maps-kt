package space.kscience.maps.coordinates

import space.kscience.kmath.geometry.*
import kotlin.math.*

/**
 * A directed straight (geodetic) segment on a spheroid with given start, direction, end point and distance.
 * @param forward coordinate of a start point with the forward direction
 * @param backward coordinate of an end point with the backward direction
 */
public class GmcCurve internal constructor(
    public val forward: GmcPose,
    public val backward: GmcPose,
    public val distance: Distance,
){
    override fun toString(): String {
        return "GmcCurve(from: ${forward.coordinates}, to: ${backward.coordinates})"
    }
}

public operator fun  ClosedRange<Radians>.contains(angle: Angle): Boolean = contains(angle.toRadians())

/**
 * Reverse direction and order of ends
 */
public fun GmcCurve.reversed(): GmcCurve = GmcCurve(backward.reversed(), forward.reversed(), distance)

/**
 * Compute a curve alongside a meridian
 */
public fun GeoEllipsoid.meridianCurve(
    longitude: Angle,
    fromLatitude: Angle,
    toLatitude: Angle,
    step: Radians = 0.015.radians,
): GmcCurve {
    require(fromLatitude in (-Angle.piDiv2)..(Angle.piDiv2)) { "Latitude must be in (-90, 90) degrees range" }
    require(toLatitude in (-Angle.piDiv2)..(Angle.piDiv2)) { "Latitude must be in (-90, 90) degrees range" }

    fun smallDistance(from: Radians, to: Radians): Distance = equatorRadius *
            (1 - eSquared) *
            (1 - eSquared * sin(from).pow(2)).pow(-1.5) *
            abs((from - to).value)

    val up = toLatitude > fromLatitude

    val integrateFrom: Radians
    val integrateTo: Radians

    if (up) {
        integrateFrom = fromLatitude.toRadians()
        integrateTo = toLatitude.toRadians()
    } else {
        integrateTo = fromLatitude.toRadians()
        integrateFrom = toLatitude.toRadians()
    }

    var current: Radians = integrateFrom
    var s = Distance(0.0)
    while (current < integrateTo) {
        val next = minOf(current + step, integrateTo)
        s += smallDistance(current, next)
        current = next
    }

    return GmcCurve(
        forward = GmcPose(GeodeticMapCoordinates.normalized(fromLatitude, longitude), if (up) Angle.zero else Angle.pi),
        backward = GmcPose(GeodeticMapCoordinates.normalized(toLatitude, longitude), if (up) Angle.pi else Angle.zero),
        distance = s
    )
}

/**
 * Compute a curve alongside a parallel
 */
public fun GeoEllipsoid.parallelCurve(latitude: Angle, fromLongitude: Angle, toLongitude: Angle): GmcCurve {
    require(latitude in (-Angle.piDiv2)..(Angle.piDiv2)) { "Latitude must be in (-90, 90) degrees range" }
    val right = toLongitude > fromLongitude
    return GmcCurve(
        forward = GmcPose(GeodeticMapCoordinates.normalized(latitude, fromLongitude), if (right) Angle.piDiv2 else -Angle.piDiv2),
        backward = GmcPose(GeodeticMapCoordinates.normalized(latitude, toLongitude), if (right) -Angle.piDiv2 else Angle.piDiv2),
        distance = reducedRadius(latitude) * abs((fromLongitude - toLongitude).toRadians().value)
    )
}


/**
 * Taken from https://github.com/mgavaghan/geodesy
 * https://github.com/mgavaghan/geodesy/blob/ab1c6969dc964ff34929911f055b27525909ef3f/src/main/java/org/gavaghan/geodesy/GeodeticCalculator.java#L58
 *
 * Calculate the destination and final bearing after traveling a specified
 * distance, and a specified starting bearing, for an initial location. This
 * is the solution to the direct geodetic problem.
 *
 * @param start starting location
 * @return solution to the direct geodetic problem
 */
@Suppress("SpellCheckingInspection", "LocalVariableName")
public fun GeoEllipsoid.curveInDirection(
    start: GmcPose,
    distance: Distance,
    precision: Double = 1e-6,
): GmcCurve {
    val a: Distance = equatorRadius
    val b: Distance = polarRadius
    val aSquared: Double = a.kilometers.pow(2)
    val bSquared: Double = b.kilometers.pow(2)
    val phi1 = start.latitude
    val alpha1 = start.bearing
    val cosAlpha1: Double = cos(alpha1)
    val sinAlpha1: Double = sin(alpha1)
    val tanU1: Double = (1.0 - f) * tan(phi1)
    val cosU1: Double = 1.0 / sqrt(1.0 + tanU1 * tanU1)
    val sinU1 = tanU1 * cosU1

    // eq. 1
    val sigma1: Radians = atan2(tanU1, cosAlpha1).radians

    // eq. 2
    val sinAlpha: Double = cosU1 * sinAlpha1
    val sin2Alpha = sinAlpha * sinAlpha
    val cos2Alpha = 1 - sin2Alpha
    val uSquared = cos2Alpha * (aSquared - bSquared) / bSquared

    // eq. 3
    val A: Double = 1 + uSquared / 16384 * (4096 + uSquared * (-768 + uSquared * (320 - 175 * uSquared)))

    // eq. 4
    val B: Double = uSquared / 1024 * (256 + uSquared * (-128 + uSquared * (74 - 47 * uSquared)))

    // iterate until there is a negligible change in sigma
    val sOverbA: Radians = (distance / b / A).radians
    var sigma: Radians = sOverbA
    var sinSigma: Double
    var prevSigma: Radians = sOverbA
    var sigmaM2: Radians
    var cosSigmaM2: Double
    var cos2SigmaM2: Double
    while (!prevSigma.value.isNaN()) {
        // eq. 5
        sigmaM2 = sigma1 * 2.0 + sigma
        cosSigmaM2 = cos(sigmaM2)
        cos2SigmaM2 = cosSigmaM2 * cosSigmaM2
        sinSigma = sin(sigma)
//        val cosSigma: Double = cos(sigma)

        // eq. 6
        val deltaSigma = B * sinSigma *
                (cosSigmaM2 + B / 4.0 * (cos(sigma) * (-1 + 2 * cos2SigmaM2) -
                        B / 6.0 * cosSigmaM2 * (-3 + 4 * sinSigma * sinSigma) * (-3 + 4 * cos2SigmaM2)))

        // eq. 7
        sigma = sOverbA + deltaSigma.radians

        // break after converging to tolerance
        if (abs((sigma - prevSigma).value) < precision) break
        prevSigma = sigma
    }
    sigmaM2 = sigma1 * 2.0 + sigma
    cosSigmaM2 = cos(sigmaM2)
    cos2SigmaM2 = cosSigmaM2 * cosSigmaM2
    val cosSigma: Double = cos(sigma)
    sinSigma = sin(sigma)

    // eq. 8
    val phi2: Radians = atan2(
        sinU1 * cosSigma + cosU1 * sinSigma * cosAlpha1,
        (1.0 - f) * sqrt(
            sin2Alpha + (sinU1 * sinSigma - cosU1 * cosSigma * cosAlpha1).pow(2)
        )
    ).radians

    // eq. 9
    // This fixes the pole crossing defect spotted by Matt Feemster. When a
    // path passes a pole and essentially crosses a line of latitude twice -
    // once in each direction - the longitude calculation got messed up.
    //
    // Using atan2 instead of atan fixes the defect. The change is in the
    // next 3 lines.
    //
    // double tanLambda = sinSigma * sinAlpha1 / (cosU1 * cosSigma - sinU1 *
    // sinSigma * cosAlpha1);
    // double lambda = Math.atan(tanLambda);
    val lambda: Double = atan2(
        sinSigma * sinAlpha1,
        cosU1 * cosSigma - sinU1 * sinSigma * cosAlpha1
    )

    // eq. 10
    val C = f / 16 * cos2Alpha * (4 + f * (4 - 3 * cos2Alpha))

    // eq. 11
    val L = lambda - (1 - C) * f * sinAlpha *
            (sigma.value + C * sinSigma * (cosSigmaM2 + C * cosSigma * (-1 + 2 * cos2SigmaM2)))

    val endPoint = GeodeticMapCoordinates.normalized(phi2, start.longitude + L.radians)

    // eq. 12

    return GmcCurve(
        start,
        GmcPose(
            endPoint,
            atan2(sinAlpha, -sinU1 * sinSigma + cosU1 * cosSigma * cosAlpha1).radians
        ),
        distance
    )
}

/**
 * Taken from https://github.com/mgavaghan/geodesy
 *
 * Calculate the geodetic curve between two points on a specified reference
 * ellipsoid. This is the solution to the inverse geodetic problem.
 *
 * @receiver reference ellipsoid to use
 * @param start starting coordinates
 * @param end ending coordinates
 * @return solution to the inverse geodetic problem
 */
@Suppress("SpellCheckingInspection", "LocalVariableName")
public fun GeoEllipsoid.curveBetween(start: Gmc, end: Gmc, precision: Double = 1e-6): GmcCurve {
    //
    // All equation numbers refer back to Vincenty's publication:
    // See http://www.ngs.noaa.gov/PUBS_LIB/inverse.pdf
    //

    // get constants
    val a = equatorRadius
    val b = polarRadius

    if(start == end) error("Can't compute a curve because start and end coincide at $start")

    // get parameters as radians
    val phi1 = start.latitude
    val lambda1 = start.longitude
    val phi2 = end.latitude
    val lambda2 = end.longitude

    // calculations
    val a2 = a.kilometers * a.kilometers
    val b2 = b.kilometers * b.kilometers
    val a2b2b2 = (a2 - b2) / b2
    val omega: Angle = lambda2 - lambda1
    val tanphi1: Double = tan(phi1)
    val tanU1 = (1.0 - f) * tanphi1
    val U1: Double = atan(tanU1)
    val sinU1: Double = sin(U1)
    val cosU1: Double = cos(U1)
    val tanphi2: Double = tan(phi2)
    val tanU2 = (1.0 - f) * tanphi2
    val U2: Double = atan(tanU2)
    val sinU2: Double = sin(U2)
    val cosU2: Double = cos(U2)
    val sinU1sinU2 = sinU1 * sinU2
    val cosU1sinU2 = cosU1 * sinU2
    val sinU1cosU2 = sinU1 * cosU2
    val cosU1cosU2 = cosU1 * cosU2

    // eq. 13
    var lambda: Angle = omega

    // intermediates we'll need to compute 's'
    var A = 0.0

    var sigma = 0.0
    var deltasigma = 0.0
    var lambda0: Angle
    var converged = false
    for (i in 0..19) {
        lambda0 = lambda
        val sinlambda: Double = sin(lambda)
        val coslambda: Double = cos(lambda)

        // eq. 14
        val sin2sigma =
            cosU2 * sinlambda * cosU2 * sinlambda + (cosU1sinU2 - sinU1cosU2 * coslambda) * (cosU1sinU2 - sinU1cosU2 * coslambda)
        val sinsigma: Double = sqrt(sin2sigma)

        // eq. 15
        val cossigma = sinU1sinU2 + cosU1cosU2 * coslambda

        // eq. 16
        sigma = atan2(sinsigma, cossigma)

        // eq. 17 Careful! sin2sigma might be almost 0!
        val sinalpha = if (sin2sigma == 0.0) 0.0 else cosU1cosU2 * sinlambda / sinsigma
        val alpha: Double = asin(sinalpha)
        val cosalpha: Double = cos(alpha)
        val cos2alpha = cosalpha * cosalpha

        // eq. 18 Careful! cos2alpha might be almost 0!
        val cos2sigmam = if (cos2alpha == 0.0) 0.0 else cossigma - 2 * sinU1sinU2 / cos2alpha
        val u2 = cos2alpha * a2b2b2
        val cos2sigmam2 = cos2sigmam * cos2sigmam

        // eq. 3
        A = 1.0 + u2 / 16384 * (4096 + u2 * (-768 + u2 * (320 - 175 * u2)))

        // eq. 4
        val B = u2 / 1024 * (256 + u2 * (-128 + u2 * (74 - 47 * u2)))

        // eq. 6
        deltasigma =
            B * sinsigma * (cos2sigmam + B / 4 * (cossigma * (-1 + 2 * cos2sigmam2) - B / 6 * cos2sigmam * (-3 + 4 * sin2sigma) * (-3 + 4 * cos2sigmam2)))

        // eq. 10
        val C = f / 16 * cos2alpha * (4 + f * (4 - 3 * cos2alpha))

        // eq. 11 (modified)
        lambda = omega + (
                (1 - C) * f * sinalpha *
                        (sigma + C * sinsigma * (cos2sigmam + C * cossigma * (-1 + 2 * cos2sigmam2)))
                ).radians

        // see how much improvement we got
        val change: Double = abs((lambda - lambda0) / lambda)
        if (i > 1 && change < precision) {
            converged = true
            break
        }
    }

    // eq. 19
    val s: Distance = b * A * (sigma - deltasigma)
    val alpha1: Radians
    val alpha2: Radians

    // didn't converge? must be N/S
    if (!converged) {
        if (phi1 > phi2) {
            alpha1 = Angle.pi.toRadians()
            alpha2 = 0.0.radians
        } else if (phi1 < phi2) {
            alpha1 = 0.0.radians
            alpha2 = Angle.pi.toRadians()
        } else {
            error("Start and end point coinside.")
        }
    } else {
        // eq. 20
        alpha1 = atan2(
            cosU2 * sin(lambda),
            cosU1sinU2 - sinU1cosU2 * cos(lambda)
        ).radians

        // eq. 21
        alpha2 = atan2(
            cosU1 * sin(lambda),
            -sinU1cosU2 + cosU1sinU2 * cos(lambda)
        ).radians + Angle.pi
    }
    return GmcCurve(
        GmcPose(start, alpha1),
        GmcPose(end, alpha2),
        s
    )
}