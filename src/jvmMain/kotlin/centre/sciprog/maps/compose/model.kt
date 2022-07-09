package centre.sciprog.maps.compose

import kotlin.math.abs

class MapRectangle(
    var topLeft: GeodeticMapCoordinates,
    var bottomRight: GeodeticMapCoordinates,
) {
    init {
        require(topLeft.latitude >= bottomRight.latitude)
        require(topLeft.longitude <= bottomRight.longitude)
    }
    val topRight: GeodeticMapCoordinates = GeodeticMapCoordinates.ofRadians(topLeft.latitude, bottomRight.longitude)
    val bottomLeft: GeodeticMapCoordinates = GeodeticMapCoordinates.ofRadians(bottomRight.latitude, topLeft.longitude)
}

sealed interface MapFeature

class MapLine(
    val from: GeodeticMapCoordinates,
    val to: GeodeticMapCoordinates,
) : MapFeature

class MapCircle(
    val center: GeodeticMapCoordinates,
)