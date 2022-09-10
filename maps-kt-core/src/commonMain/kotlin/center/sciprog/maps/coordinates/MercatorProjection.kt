/*
 * Copyright 2018-2021 KMath contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package center.sciprog.maps.coordinates

import center.sciprog.maps.coordinates.Angle.Companion.pi
import kotlin.math.atan
import kotlin.math.ln
import kotlin.math.sinh

public data class ProjectionCoordinates(val x: Distance, val y: Distance)

/**
 * @param T the type of projection coordinates
 */
public interface MapProjection<T: Any>{
    public fun toGeodetic(pc: T): GeodeticMapCoordinates
    public fun toProjection(gmc: GeodeticMapCoordinates): T
}


/**
 * @param baseLongitude the longitude offset in radians
 * @param radius the average radius of the Earth
 * @param correctedRadius optional radius correction to account for ellipsoid model
 */
public open class MercatorProjection(
    public val baseLongitude: Angle = Angle.zero,
    protected val radius: Distance = DEFAULT_EARTH_RADIUS,
    private val correctedRadius: ((GeodeticMapCoordinates) -> Distance)? = null,
): MapProjection<ProjectionCoordinates> {

    override fun toGeodetic(pc: ProjectionCoordinates): GeodeticMapCoordinates {
        val res = GeodeticMapCoordinates.ofRadians(
            atan(sinh(pc.y / radius)),
            baseLongitude.radians.value + (pc.x / radius),
        )
        return if (correctedRadius != null) {
            val r = correctedRadius.invoke(res)
            GeodeticMapCoordinates.ofRadians(
                atan(sinh(pc.y / r)),
                baseLongitude.radians.value + pc.x / r,
            )
        } else {
            res
        }
    }

    /**
     * https://en.wikipedia.org/wiki/Web_Mercator_projection#Formulas
     */
    override fun toProjection(gmc: GeodeticMapCoordinates): ProjectionCoordinates {
        require(abs(gmc.latitude) <= MAXIMUM_LATITUDE) { "Latitude exceeds the maximum latitude for mercator coordinates" }
        val r: Distance = correctedRadius?.invoke(gmc) ?: radius
        return ProjectionCoordinates(
            x = r * (gmc.longitude - baseLongitude).radians.value,
            y = r * ln(tan(pi / 4 + gmc.latitude / 2))
        )
    }

    public companion object : MercatorProjection(Angle.zero, Distance(6378.137)) {
        public val MAXIMUM_LATITUDE: Angle = 85.05113.degrees
        public val DEFAULT_EARTH_RADIUS: Distance = radius
    }
}