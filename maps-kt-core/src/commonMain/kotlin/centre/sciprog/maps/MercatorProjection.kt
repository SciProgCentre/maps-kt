/*
 * Copyright 2018-2021 KMath contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package centre.sciprog.maps

import kotlin.math.*

public data class MercatorCoordinates(val x: Double, val y: Double)

/**
 * @param baseLongitude the longitude offset in radians
 * @param radius the average radius of the Earth
 * @param correctedRadius optional radius correction to account for ellipsoid model
 */
public open class MercatorProjection(
    public val baseLongitude: Double = 0.0,
    protected val radius: Double = DEFAULT_EARTH_RADIUS,
    private val correctedRadius: ((GeodeticMapCoordinates) -> Double)? = null,
) {

    public fun toGeodetic(mc: MercatorCoordinates): GeodeticMapCoordinates {
        val res = GeodeticMapCoordinates.ofRadians(
            atan(sinh(mc.y / radius)),
            baseLongitude + mc.x / radius,
        )
        return if (correctedRadius != null) {
            val r = correctedRadius.invoke(res)
            GeodeticMapCoordinates.ofRadians(
                atan(sinh(mc.y / r)),
                baseLongitude + mc.x / r,
            )
        } else {
            res
        }
    }

    /**
     * https://en.wikipedia.org/wiki/Web_Mercator_projection#Formulas
     */
    public fun toMercator(gmc: GeodeticMapCoordinates): MercatorCoordinates {
        require(abs(gmc.latitude) <= MAXIMUM_LATITUDE) { "Latitude exceeds the maximum latitude for mercator coordinates" }
        val r = correctedRadius?.invoke(gmc) ?: radius
        return MercatorCoordinates(
            x = r * (gmc.longitude - baseLongitude),
            y = r * ln(tan(PI / 4 + gmc.latitude / 2))
        )
    }

    public companion object : MercatorProjection(0.0, 6378137.0) {
        public const val MAXIMUM_LATITUDE: Double = 85.05113
        public val DEFAULT_EARTH_RADIUS: Double = radius
    }
}