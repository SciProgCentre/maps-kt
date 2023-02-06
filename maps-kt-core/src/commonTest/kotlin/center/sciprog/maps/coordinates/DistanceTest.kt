package center.sciprog.maps.coordinates

import space.kscience.kmath.geometry.radians
import kotlin.test.Test
import kotlin.test.assertEquals

internal class DistanceTest {
    companion object {
        val moscow = Gmc.ofDegrees(55.76058287719673, 37.60358622841869)
        val spb = Gmc.ofDegrees(59.926686023580444, 30.36038109122013)
    }

    @Test
    fun ellipsoidParameters() {
        assertEquals(298.257223563, GeoEllipsoid.WGS84.inverseF, 1e-6)
    }

    @Test
    fun curveBetween() {
        val curve = GeoEllipsoid.WGS84.curveBetween(moscow, spb)
        val distance = curve.distance

        assertEquals(632.035426877, distance.kilometers, 0.0001)
        assertEquals(-0.6947937116552751, curve.forward.bearing.radians, 0.0001)
    }

    @Test
    fun curveInDirection() {
        val curve = GeoEllipsoid.WGS84.curveInDirection(
            GmcPose(moscow, (-0.6947937116552751).radians), Distance(632.035426877)
        )

        assertEquals(spb.latitude.radians, curve.backward.latitude.radians, 0.0001)
        assertEquals(spb.longitude.radians, curve.backward.longitude.radians, 0.0001)
    }
}