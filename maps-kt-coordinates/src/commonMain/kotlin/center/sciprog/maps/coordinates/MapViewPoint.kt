package center.sciprog.maps.coordinates

import kotlin.math.pow

/**
 * Observable position on the map. Includes observation coordinate and [zoom] factor
 */
public data class MapViewPoint(
    val focus: GeodeticMapCoordinates,
    val zoom: Double,
) {
    val scaleFactor: Double by lazy { WebMercatorProjection.scaleFactor(zoom) }
}

public fun MapViewPoint.move(delta: GeodeticMapCoordinates): MapViewPoint {
    val newCoordinates = GeodeticMapCoordinates.ofRadians(
        (focus.latitude + delta.latitude).coerceIn(
            -MercatorProjection.MAXIMUM_LATITUDE,
            MercatorProjection.MAXIMUM_LATITUDE
        ),
        focus.longitude + delta.longitude
    )
    return MapViewPoint(newCoordinates, zoom)
}

public fun MapViewPoint.zoom(
    zoomDelta: Double,
    invariant: GeodeticMapCoordinates = focus,
): MapViewPoint = if (invariant == focus) {
    copy(zoom = (zoom + zoomDelta).coerceIn(2.0, 18.0))
} else {
    val difScale = (1 - 2.0.pow(-zoomDelta))
    val newCenter = GeodeticMapCoordinates.ofRadians(
        focus.latitude + (invariant.latitude - focus.latitude) * difScale,
        focus.longitude + (invariant.longitude - focus.longitude) * difScale
    )
    MapViewPoint(newCenter, (zoom + zoomDelta).coerceIn(2.0, 18.0))
}