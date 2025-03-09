package space.kscience.maps.coordinates

import kotlinx.serialization.Serializable
import space.kscience.kmath.geometry.*

/**
 * Geodetic coordinated
 *
 * @param elevation is optional
 */
@Serializable
public class GeodeticMapCoordinates(
    public val latitude: Angle,
    public val longitude: Angle,
    public val elevation: Distance? = null,
) : Vector2D<Angle> {
    init {
        require(latitude in (-Angle.piDiv2)..(Angle.piDiv2)) {
            "Latitude $latitude is not in (-PI/2)..(PI/2)"
        }
        require(longitude in (-Angle.pi..Angle.pi)) {
            "Longitude $longitude is not in (-PI..PI) range"
        }
    }
    override val x: Angle get() = longitude

    override val y: Angle get() = latitude

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as GeodeticMapCoordinates

        return latitude == other.latitude && longitude == other.longitude
    }

    override fun hashCode(): Int {
        var result = latitude.hashCode()
        result = 31 * result + longitude.hashCode()
        return result
    }

    override fun toString(): String {
        return "GMC(latitude=${latitude.toDegrees().value} deg, longitude=${longitude.toDegrees().value} deg)"
    }


    public companion object {
        public fun normalized(
            latitude: Angle,
            longitude: Angle,
            elevation: Distance? = null,
        ): GeodeticMapCoordinates = GeodeticMapCoordinates(
            latitude.coerceIn(-Angle.piDiv2..Angle.piDiv2), longitude.normalized(Angle.zero), elevation
        )

        public fun ofRadians(
            latitude: Double,
            longitude: Double,
            elevation: Distance? = null,
        ): GeodeticMapCoordinates = normalized(latitude.radians, longitude.radians, elevation)

        public fun ofDegrees(
            latitude: Double,
            longitude: Double,
            elevation: Distance? = null,
        ): GeodeticMapCoordinates = normalized(latitude.degrees, longitude.degrees, elevation)
    }
}


/**
 * Short name for GeodeticMapCoordinates
 */
public typealias Gmc = GeodeticMapCoordinates

//public interface GeoToScreenConversion {
//    public fun getScreenX(gmc: GeodeticMapCoordinates): Double
//    public fun getScreenY(gmc: GeodeticMapCoordinates): Double
//
//    public fun invalidationFlow(): Flow<Unit>
//}
//
//public interface ScreenMapCoordinates {
//    public val gmc: GeodeticMapCoordinates
//    public val converter: GeoToScreenConversion
//
//    public val x: Double get() = converter.getScreenX(gmc)
//    public val y: Double get() = converter.getScreenX(gmc)
//}