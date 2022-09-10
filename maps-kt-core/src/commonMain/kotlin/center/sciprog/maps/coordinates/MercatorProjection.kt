/*
 * Copyright 2018-2021 KMath contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package center.sciprog.maps.coordinates

import center.sciprog.maps.coordinates.Angle.Companion.pi
import kotlin.math.*

public data class ProjectionCoordinates(val x: Distance, val y: Distance)

/**
 * @param T the type of projection coordinates
 */
public interface MapProjection<T : Any> {
    public fun toGeodetic(pc: T): GeodeticMapCoordinates
    public fun toProjection(gmc: GeodeticMapCoordinates): T

    public companion object{
        public val epsg3857: MercatorProjection = MercatorProjection()
    }
}


/**
 * @param baseLongitude the longitude offset in radians
 * @param ellipsoid - a [GeoEllipsoid] to be used for conversion
 */
public open class MercatorProjection(
    public val baseLongitude: Angle = Angle.zero,
    public val ellipsoid: GeoEllipsoid = GeoEllipsoid.sphere,
) : MapProjection<ProjectionCoordinates> {

    override fun toGeodetic(pc: ProjectionCoordinates): GeodeticMapCoordinates {
        val res = GeodeticMapCoordinates.ofRadians(
            atan(sinh(pc.y / ellipsoid.equatorRadius)),
            baseLongitude.radians.value + (pc.x / ellipsoid.equatorRadius),
        )

        return if (ellipsoid === GeoEllipsoid.sphere) {
            res
        } else {
            TODO("Elliptical mercator projection not implemented")
//            GeodeticMapCoordinates(
//                atan(sinh(pc.y / ellipsoid.polarRadius)).radians,
//                res.longitude,
//            )
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
                x = ellipsoid.equatorRadius * (gmc.longitude - baseLongitude).radians.value,
                y = ellipsoid.equatorRadius * ln(tan(pi / 4 + gmc.latitude / 2))
            )
        } else {
            val sinPhi = sin(gmc.latitude)
            ProjectionCoordinates(
                x = ellipsoid.equatorRadius * (gmc.longitude - baseLongitude).radians.value,
                y = ellipsoid.equatorRadius * ln(
                    tan(pi / 4 + gmc.latitude / 2) * ((1 - e * sinPhi) / (1 + e * sinPhi)).pow(e / 2)
                )
            )
        }

    }

    public companion object {
        public val MAXIMUM_LATITUDE: Angle = 85.05113.degrees
    }
}