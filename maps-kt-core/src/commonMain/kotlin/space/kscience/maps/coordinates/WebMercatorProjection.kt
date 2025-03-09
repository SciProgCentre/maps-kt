/*
 * Copyright 2018-2021 KMath contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package space.kscience.maps.coordinates

import space.kscience.kmath.geometry.abs
import kotlin.math.*

public data class WebMercatorCoordinates(val zoom: Int, val x: Float, val y: Float)

public object WebMercatorProjection {

    /**
     * Compute radians to projection coordinates ratio for given [zoom] factor
     */
    public fun scaleFactor(zoom: Float): Float = (256.0 / 2 / PI * 2f.pow(zoom)).toFloat()

    public fun toGeodetic(mercator: WebMercatorCoordinates): GeodeticMapCoordinates {
        val scaleFactor = scaleFactor(mercator.zoom.toFloat())
        val longitude = mercator.x / scaleFactor - PI
        val latitude = (atan(exp(PI - mercator.y / scaleFactor)) - PI / 4) * 2
        return GeodeticMapCoordinates.ofRadians(latitude, longitude)
    }

    /**
     * https://en.wikipedia.org/wiki/Web_Mercator_projection#Formulas
     *
     * return null if gmc is outside of possible coordinate scope for WebMercator
     */
    public fun toMercator(gmc: GeodeticMapCoordinates, zoom: Int): WebMercatorCoordinates? {
        if (abs(gmc.latitude) > MercatorProjection.MAXIMUM_LATITUDE) return null

        val scaleFactor = scaleFactor(zoom.toFloat())
        return WebMercatorCoordinates(
            zoom = zoom,
            x = scaleFactor * (gmc.longitude.toRadians().value + PI).toFloat(),
            y = scaleFactor * (PI - ln(tan(PI / 4 + gmc.latitude.toRadians().value / 2))).toFloat()
        )
    }

//    /**
//     * Compute and offset of [target] coordinate relative to [base] coordinate. If [zoom] is null, then optimal zoom
//     * will be computed to put the resulting x and y coordinates between -127.0 and 128.0
//     */
//    public fun computeOffset(
//        base: GeodeticMapCoordinates,
//        target: GeodeticMapCoordinates,
//        zoom: Double? = null,
//    ): TileWebMercatorCoordinates {
//        val xOffsetUnscaled = target.longitude - base.longitude
//        val yOffsetUnscaled = ln(
//            tan(PI / 4 + target.latitude / 2) / tan(PI / 4 + base.latitude / 2)
//        )
//
//        val computedZoom = zoom ?: ceil(log2(PI / max(abs(xOffsetUnscaled), abs(yOffsetUnscaled))))
//        val scaleFactor = scaleFactor(computedZoom)
//        return TileWebMercatorCoordinates(
//            computedZoom,
//            x = scaleFactor * xOffsetUnscaled,
//            y = scaleFactor * yOffsetUnscaled
//        )
//    }
}