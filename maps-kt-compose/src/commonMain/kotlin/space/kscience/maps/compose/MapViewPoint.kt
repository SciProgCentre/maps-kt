package space.kscience.maps.compose

import space.kscience.maps.coordinates.GeodeticMapCoordinates
import space.kscience.maps.coordinates.Gmc
import space.kscience.maps.coordinates.WebMercatorProjection
import space.kscience.maps.features.ViewPoint

/**
 * Observable position on the map. Includes observation coordinate and [zoom] factor
 */
internal data class MapViewPoint(
    override val focus: GeodeticMapCoordinates,
    override val zoom: Float,
) : ViewPoint<Gmc>{
    val scaleFactor: Float by lazy { WebMercatorProjection.scaleFactor(zoom) }

    public companion object{
        public val globe: MapViewPoint = MapViewPoint(Gmc.ofRadians(0.0, 0.0), 1f)
    }
}