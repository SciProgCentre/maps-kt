package center.sciprog.maps.compose

import center.sciprog.maps.coordinates.GeodeticMapCoordinates
import center.sciprog.maps.coordinates.Gmc
import center.sciprog.maps.coordinates.WebMercatorProjection
import center.sciprog.maps.coordinates.radians
import center.sciprog.maps.features.ViewPoint

/**
 * Observable position on the map. Includes observation coordinate and [zoom] factor
 */
internal data class MapViewPoint(
    override val focus: GeodeticMapCoordinates,
    override val zoom: Float,
) : ViewPoint<Gmc>{
    val scaleFactor: Float by lazy { WebMercatorProjection.scaleFactor(zoom) }

    public companion object{
        public val globe: MapViewPoint = MapViewPoint(GeodeticMapCoordinates(0.0.radians, 0.0.radians), 1f)
    }
}