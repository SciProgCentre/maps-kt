package center.sciprog.maps.compose

import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import center.sciprog.maps.coordinates.*
import center.sciprog.maps.features.CoordinateSpace
import center.sciprog.maps.features.Rectangle
import center.sciprog.maps.features.ViewPoint
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.pow

public object WebMercatorSpace : CoordinateSpace<Gmc> {

    private fun intZoom(zoom: Float): Int = floor(zoom).toInt()
    private fun tileScale(zoom: Float): Float = 2f.pow(zoom - floor(zoom))


    override fun Rectangle(first: Gmc, second: Gmc): Rectangle<Gmc> = GmcRectangle(first, second)

    override fun Rectangle(center: Gmc, zoom: Float, size: DpSize): Rectangle<Gmc> {
        val scale = WebMercatorProjection.scaleFactor(zoom)
        return Rectangle(center, (size.width.value / scale).radians, (size.height.value / scale).radians)
    }

    override val defaultViewPoint: ViewPoint<Gmc> = MapViewPoint.globe

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
        ViewPoint(focus, (zoom + zoomDelta).coerceIn(2f, 18f))
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

    override fun Gmc.offsetTo(b: Gmc, zoom: Float): DpOffset {
        val intZoom = intZoom(zoom)
        val mercatorA = WebMercatorProjection.toMercator(this, intZoom)
        val mercatorB = WebMercatorProjection.toMercator(b, intZoom)
        val tileScale = tileScale(zoom)
        return DpOffset(
            (mercatorA.x - mercatorB.x).dp * tileScale,
            (mercatorA.y - mercatorB.y).dp * tileScale
        )
    }

    override fun Gmc.isInsidePolygon(points: List<Gmc>): Boolean = points.zipWithNext().count { (left, right) ->
        val dist = right.latitude - left.latitude
        val intersection = left.latitude * abs((right.longitude - longitude) / dist) +
                right.latitude * abs((longitude - left.longitude) / dist)
        longitude in left.longitude..right.longitude && intersection >= latitude
    } % 2 == 0
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