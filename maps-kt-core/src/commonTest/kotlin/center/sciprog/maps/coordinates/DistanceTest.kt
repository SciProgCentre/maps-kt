package center.sciprog.maps.coordinates

import kotlin.test.Ignore
import kotlin.test.Test
import kotlin.test.assertEquals

internal class DistanceTest {
    companion object {
        val moscow = GMC.ofDegrees(55.76058287719673, 37.60358622841869)
        val spb = GMC.ofDegrees(59.926686023580444, 30.36038109122013)
    }

    @Test
    fun ellipsoidParameters() {
        assertEquals(298.257223563, GeoEllipsoid.WGS84.inverseF, 1e-6)
    }

    @Test
    @Ignore
    fun greatCircleDistance() {
        assertEquals(
            expected = 632.035,
            actual = GeoEllipsoid.greatCircleAngleBetween(moscow, spb).value *
                    GeoEllipsoid.WGS84.equatorRadius.kilometers,
            absoluteTolerance = 0.1
        )
    }

    @Test
    fun largeDistance() {
        val curve = GeoEllipsoid.WGS84.curveBetween(moscow, spb)
        val distance = curve.distance

        assertEquals(632.035426877, distance.kilometers, 0.1)

    }
}