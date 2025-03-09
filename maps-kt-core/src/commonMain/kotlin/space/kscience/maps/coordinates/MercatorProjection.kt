/*
 * Copyright 2018-2021 KMath contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package space.kscience.maps.coordinates

import kotlinx.serialization.Serializable
import space.kscience.kmath.geometry.*
import kotlin.math.*

public data class ProjectionCoordinates(val x: Distance, val y: Distance)

/**
 * @param T the type of projection coordinates
 */
public interface MapProjection<T : Any> {
    public fun toGeodetic(pc: T): GeodeticMapCoordinates
    public fun toProjection(gmc: GeodeticMapCoordinates): T

    public companion object {
        public val epsg3857: MercatorProjection = MercatorProjection()
    }
}


/**
 * @param baseLongitude the longitude offset in radians
 * @param ellipsoid - a [GeoEllipsoid] to be used for conversion
 */
@Serializable
public open class MercatorProjection(
    public val baseLongitude: Angle = Angle.zero,
    public val ellipsoid: GeoEllipsoid = GeoEllipsoid.sphere,
) : MapProjection<ProjectionCoordinates> {


    /**
     * Taken from https://github.com/geotools/geotools/blob/main/modules/library/referencing/src/main/java/org/geotools/referencing/operation/projection/Mercator.java#L164
     */
    private fun cphi2(ts: Double): Double {
        val eccnth: Double = 0.5 * ellipsoid.eccentricity
        var phi: Double = PI / 2 - 2.0 * atan(ts)
        for (i in 0 until 15) {
            val con: Double = ellipsoid.eccentricity * sin(phi)
            val dphi: Double = PI / 2 - 2.0 * atan(ts * ((1 - con) / (1 + con)).pow(eccnth)) - phi
            phi += dphi
            if (abs(dphi) <= 1e-10) {
                return phi
            }
        }
        error("Inverse mercator projection transformation failed to converge")
    }


    override fun toGeodetic(pc: ProjectionCoordinates): GeodeticMapCoordinates {
        return if (ellipsoid === GeoEllipsoid.sphere) {
            GeodeticMapCoordinates.ofRadians(
                atan(sinh(pc.y / ellipsoid.equatorRadius)),
                baseLongitude.toRadians().value + (pc.x / ellipsoid.equatorRadius),
            )
        } else {
            GeodeticMapCoordinates.ofRadians(
                cphi2(exp(-(pc.y / ellipsoid.equatorRadius))),
                baseLongitude.toRadians().value + (pc.x / ellipsoid.equatorRadius)
            )
        }
    }

    /**
     * https://en.wikipedia.org/wiki/Web_Mercator_projection#Formulas
     */
    override fun toProjection(gmc: GeodeticMapCoordinates): ProjectionCoordinates {
        require(abs(gmc.latitude) <= MAXIMUM_LATITUDE) { "Latitude exceeds the maximum latitude for mercator coordinates" }
        val e = sqrt(ellipsoid.eSquared)

        return if (ellipsoid === GeoEllipsoid.sphere) {
            ProjectionCoordinates(
                x = ellipsoid.equatorRadius * (gmc.longitude - baseLongitude).toRadians().value,
                y = ellipsoid.equatorRadius * ln(tan(Angle.pi / 4 + gmc.latitude / 2))
            )
        } else {
            val sinPhi = sin(gmc.latitude)
            ProjectionCoordinates(
                x = ellipsoid.equatorRadius * (gmc.longitude - baseLongitude).toRadians().value,
                y = ellipsoid.equatorRadius * ln(
                    tan(Angle.pi / 4 + gmc.latitude / 2) * ((1 - e * sinPhi) / (1 + e * sinPhi)).pow(e / 2)
                )
            )
        }
    }

    public companion object {
        public val MAXIMUM_LATITUDE: Angle = 85.05113.degrees
    }
}