package space.kscience.maps.coordinates

import kotlin.test.Test
import kotlin.test.assertEquals

class MercatorTest {
    @Test
    fun sphereForwardBackward(){
        val moscow = GeodeticMapCoordinates.ofDegrees(55.76058287719673, 37.60358622841869)
        val mercator = MapProjection.epsg3857.toProjection(moscow)
        //https://epsg.io/transform#s_srs=4326&t_srs=3857&x=37.6035862&y=55.7605829
        assertEquals(4186.0120709, mercator.x.kilometers, 1e-4)
        assertEquals(7510.9013658, mercator.y.kilometers, 1e-4)
        val backwards = MapProjection.epsg3857.toGeodetic(mercator)
        assertEquals(moscow.latitude.toDegrees().value, backwards.latitude.toDegrees().value, 1e-6)
        assertEquals(moscow.longitude.toDegrees().value, backwards.longitude.toDegrees().value, 1e-6)
    }

    @Test
    fun ellipseForwardBackward(){
        val moscow = GeodeticMapCoordinates.ofDegrees(55.76058287719673, 37.60358622841869)
        val projection = MercatorProjection(ellipsoid = GeoEllipsoid.WGS84)
        val mercator = projection.toProjection(moscow)
        val backwards = projection.toGeodetic(mercator)
        assertEquals(moscow.latitude.toDegrees().value, backwards.latitude.toDegrees().value, 1e-6)
        assertEquals(moscow.longitude.toDegrees().value, backwards.longitude.toDegrees().value, 1e-6)
    }
}