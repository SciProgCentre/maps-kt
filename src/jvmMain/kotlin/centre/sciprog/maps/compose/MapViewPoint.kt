package centre.sciprog.maps.compose

/**
 * Observable position on the map
 */
data class MapViewPoint(
    val focus: GeodeticMapCoordinates,
    val zoom: Double,
) {
    val scaleFactor by lazy { WebMercatorProjection.scaleFactor(zoom) }
}

fun MapViewPoint.move(deltaX: Float, deltaY: Float): MapViewPoint {
    val newCoordinates = GeodeticMapCoordinates.ofRadians(
        (focus.latitude + deltaY / scaleFactor).coerceIn(
            -MercatorProjection.MAXIMUM_LATITUDE,
            MercatorProjection.MAXIMUM_LATITUDE
        ),
        focus.longitude + deltaX / scaleFactor
    )
    return MapViewPoint(newCoordinates, zoom)
}

fun MapViewPoint.move(delta: GeodeticMapCoordinates): MapViewPoint {
    val newCoordinates = GeodeticMapCoordinates.ofRadians(
        (focus.latitude + delta.latitude).coerceIn(
            -MercatorProjection.MAXIMUM_LATITUDE,
            MercatorProjection.MAXIMUM_LATITUDE
        ),
        focus.longitude + delta.longitude
    )
    return MapViewPoint(newCoordinates, zoom)
}

fun MapViewPoint.zoom(zoomDelta: Double): MapViewPoint {
    return copy(zoom = (zoom + zoomDelta).coerceIn(1.0, 18.0))
}

fun MapViewPoint.toMercator() = WebMercatorProjection.toMercator(focus, zoom)