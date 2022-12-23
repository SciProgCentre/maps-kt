package center.sciprog.maps.compose

import androidx.compose.ui.unit.DpSize
import center.sciprog.maps.coordinates.*
import center.sciprog.maps.features.CoordinateSpace
import center.sciprog.maps.features.Rectangle

public object GmcCoordinateSpace : CoordinateSpace<Gmc> {
    override fun buildRectangle(first: Gmc, second: Gmc): GmcRectangle = GmcRectangle(first, second)

    override fun buildRectangle(center: Gmc, zoom: Double, size: DpSize): GmcRectangle{
        val scale = WebMercatorProjection.scaleFactor(zoom)
        return buildRectangle(center, (size.width.value/scale).radians, (size.height.value / scale).radians)
    }

    override fun Rectangle<Gmc>.withCenter(center: Gmc): GmcRectangle {
            return buildRectangle(center, height = latitudeDelta, width = longitudeDelta)
    }

    override fun Collection<Rectangle<Gmc>>.wrapRectangles(): Rectangle<Gmc>? {
        if (isEmpty()) return null
        //TODO optimize computation
        val minLat = minOf { it.bottom }
        val maxLat = maxOf { it.top }
        val minLong = minOf { it.left }
        val maxLong = maxOf { it.right }
        return GmcRectangle(GeodeticMapCoordinates(minLat, minLong), GeodeticMapCoordinates(maxLat, maxLong))
    }

    override fun Collection<Gmc>.wrapPoints(): Rectangle<Gmc>? {
        if (isEmpty()) return null
        //TODO optimize computation
        val minLat = minOf { it.latitude }
        val maxLat = maxOf { it.latitude }
        val minLong = minOf { it.longitude }
        val maxLong = maxOf { it.longitude }
        return GmcRectangle(GeodeticMapCoordinates(minLat, minLong), GeodeticMapCoordinates(maxLat, maxLong))
    }
}

/**
 * A quasi-square section. Note that latitudinal distance could be imprecise for large distances
 */
public fun CoordinateSpace<Gmc>.buildRectangle(
    center: Gmc,
    height: Distance,
    width: Distance,
    ellipsoid: GeoEllipsoid = GeoEllipsoid.WGS84
): GmcRectangle {
    val reducedRadius = ellipsoid.reducedRadius(center.latitude)
    return buildRectangle(center, (height / ellipsoid.polarRadius).radians, (width / reducedRadius).radians)
}

/**
 * A quasi-square section.
 */
public fun CoordinateSpace<Gmc>.buildRectangle(
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