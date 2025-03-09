/*
 * Copyright 2018-2023 KMath contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package space.kscience.trajectory

import space.kscience.kmath.geometry.euclidean2d.Float64Space2D
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull


class DubinsTests {

    @Test
    fun dubinsTest() = with(Float64Space2D) {
        val straight = StraightTrajectory2D(vector(0.0, 0.0), vector(100.0, 100.0))
        val lineP1 = straight.shift(1, 10.0).inverse()

        val start = Pose2D(straight.end, straight.bearing)
        val end = Pose2D(lineP1.begin, lineP1.bearing)
        val radius = 2.0
        val dubins: List<CompositeTrajectory2D> = DubinsPath.all(start, end, radius)

        val absoluteDistance = start.distanceTo(end)
        println("Absolute distance: $absoluteDistance")

        val expectedLengths = mapOf(
            DubinsPath.Type.RLR to 13.067681939031397,
            DubinsPath.Type.RSR to 12.28318530717957,
            DubinsPath.Type.LSL to 32.84955592153878,
            DubinsPath.Type.RSL to 23.37758938854081,
            DubinsPath.Type.LSR to 23.37758938854081
        )

        expectedLengths.forEach {
            val path = dubins.find { p -> DubinsPath.trajectoryTypeOf(p) == it.key }
            assertNotNull(path, "Path ${it.key} not found")
            println("${it.key}: ${path.length}")
            assertEquals(it.value, path.length, 1e-4)

            val a = path.segments[0] as CircleTrajectory2D
            val b = path.segments[1]
            val c = path.segments[2] as CircleTrajectory2D

            assertEquals(start, a.beginPose, 1e-4)
            assertEquals(end, c.endPose, 1e-4)

            // Not working, theta double precision inaccuracy
            if (b is CircleTrajectory2D) {
                assertEquals(a.endPose, b.beginPose, 1e-4)
                assertEquals(c.beginPose, b.endPose, 1e-4)
            } else if (b is StraightTrajectory2D) {
                assertEquals(a.endPose, Pose2D(b.begin, b.bearing), 1e-4)
                assertEquals(c.beginPose, Pose2D(b.end, b.bearing), 1e-4)
            }
        }
    }
}
