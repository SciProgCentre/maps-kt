package center.sciprog.maps.compose

import androidx.compose.ui.unit.DpSize
import center.sciprog.maps.coordinates.*
import center.sciprog.maps.features.CoordinateSpace
import center.sciprog.maps.features.Rectangle
import center.sciprog.maps.features.ViewPoint
import kotlin.math.pow

public object GmcCoordinateSpace : CoordinateSpace<Gmc> {
    override fun Rectangle(first: Gmc, second: Gmc): Rectangle<Gmc> = GmcRectangle(first, second)

    override fun Rectangle(center: Gmc, zoom: Float, size: DpSize): Rectangle<Gmc> {
        val scale = WebMercatorProjection.scaleFactor(zoom)
        return Rectangle(center, (size.width.value / scale).radians, (size.height.value / scale).radians)
    }

    override fun ViewPoint(center: Gmc, zoom: Float): ViewPoint<Gmc> = MapViewPoint(center, zoom)

    override fun ViewPoint<Gmc>.moveBy(delta: Gmc): ViewPoint<Gmc> {
        val newCoordinates = GeodeticMapCoordinates(
            (focus.latitude + delta.latitude).coerceIn(
                -MercatorProjection.MAXIMUM_LATITUDE,
                MercatorProjection.MAXIMUM_LATITUDE
            ),
            focus.longitude + delta.longitude
        )
        return MapViewPoint(newCoordinates, zoom)
    }

    override fun ViewPoint<Gmc>.zoomBy(zoomDelta: Float, invariant: Gmc): ViewPoint<Gmc> = if (invariant == focus) {
        ViewPoint(focus, (zoom + zoomDelta).coerceIn(2f, 18f) )
    } else {
        val difScale = (1 - 2f.pow(-zoomDelta))
        val newCenter = GeodeticMapCoordinates(
            focus.latitude + (invariant.latitude - focus.latitude) * difScale,
            focus.longitude + (invariant.longitude - focus.longitude) * difScale
        )
        MapViewPoint(newCenter, (zoom + zoomDelta).coerceIn(2f, 18f))
    }

    override fun Rectangle<Gmc>.withCenter(center: Gmc): Rectangle<Gmc> =
        Rectangle(center, height = latitudeDelta, width = longitudeDelta)

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
public fun CoordinateSpace<Gmc>.Rectangle(
    center: Gmc,
    height: Distance,
    width: Distance,
    ellipsoid: GeoEllipsoid = GeoEllipsoid.WGS84,
): Rectangle<Gmc> {
    val reducedRadius = ellipsoid.reducedRadius(center.latitude)
    return Rectangle(center, (height / ellipsoid.polarRadius).radians, (width / reducedRadius).radians)
}

/**
 * A quasi-square section.
 */
public fun CoordinateSpace<Gmc>.Rectangle(
    center: GeodeticMapCoordinates,
    height: Angle,
    width: Angle,
): Rectangle<Gmc> {
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