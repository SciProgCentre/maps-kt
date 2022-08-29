package center.sciprog.maps.coordinates

/**
 * A coordinate-bearing pair
 */
public data class GmcPose(val coordinates: GeodeticMapCoordinates, val bearing: Angle) {
    val latitude: Angle get() = coordinates.latitude
    val longitude: Angle get() = coordinates.longitude
}

public fun GmcPose.reversed(): GmcPose = copy(bearing = (bearing + Angle.pi).normalized())

