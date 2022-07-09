package centre.sciprog.maps.compose

import kotlin.math.max
import kotlin.math.min

class MapRectangle private constructor(
    var topLeft: GeodeticMapCoordinates,
    var bottomRight: GeodeticMapCoordinates,
) {
    init {
        require(topLeft.latitude >= bottomRight.latitude)
        require(topLeft.longitude <= bottomRight.longitude)
    }

    val topRight: GeodeticMapCoordinates get() = GeodeticMapCoordinates.ofRadians(topLeft.latitude, bottomRight.longitude)
    val bottomLeft: GeodeticMapCoordinates get() = GeodeticMapCoordinates.ofRadians(bottomRight.latitude, topLeft.longitude)

    val topLatitude get() = topLeft.latitude
    val bottomLatitude get() = bottomRight.latitude

    val leftLongitude get() = topLeft.longitude
    val rightLongitude get() = bottomRight.longitude

    companion object{
        fun of(
            a: GeodeticMapCoordinates,
            b: GeodeticMapCoordinates
        ) = MapRectangle(
            GeodeticMapCoordinates.ofRadians(max(a.latitude, b.latitude), min(a.longitude,b.longitude)),
            GeodeticMapCoordinates.ofRadians(min(a.latitude, b.latitude), max(a.longitude,b.longitude)),
        )
    }
}

internal fun MapRectangle.move(latitudeDelta: Double, longitudeDelta: Double): MapRectangle {
    val safeLatitudeDelta: Double = if(topLatitude + latitudeDelta > MercatorProjection.MAXIMUM_LATITUDE){
        0.0
    } else if(bottomLatitude + latitudeDelta < -MercatorProjection.MAXIMUM_LATITUDE){
        0.0
    } else {
        latitudeDelta
    }
    return MapRectangle.of(
        GeodeticMapCoordinates.ofRadians(topLeft.latitude + safeLatitudeDelta, topLeft.longitude + longitudeDelta),
        GeodeticMapCoordinates.ofRadians(bottomRight.latitude + safeLatitudeDelta, bottomRight.longitude + longitudeDelta)
    )
}

sealed interface MapFeature

class MapLine(
    val from: GeodeticMapCoordinates,
    val to: GeodeticMapCoordinates,
) : MapFeature

class MapCircle(
    val center: GeodeticMapCoordinates,
)