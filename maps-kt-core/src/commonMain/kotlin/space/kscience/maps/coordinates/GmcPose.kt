package space.kscience.maps.coordinates

import kotlinx.serialization.Serializable
import space.kscience.kmath.geometry.Angle
import space.kscience.kmath.geometry.normalized

/**
 * A coordinate-bearing pair
 */
@Serializable
public data class GmcPose(val coordinates: GeodeticMapCoordinates, val bearing: Angle) {
    val latitude: Angle get() = coordinates.latitude
    val longitude: Angle get() = coordinates.longitude
}

public fun GmcPose.reversed(): GmcPose = copy(bearing = (bearing + Angle.pi).normalized())

