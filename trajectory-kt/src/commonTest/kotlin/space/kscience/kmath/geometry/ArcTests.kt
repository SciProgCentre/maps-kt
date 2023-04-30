/*
 * Copyright 2018-2022 KMath contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package space.kscience.kmath.geometry

import space.kscience.trajectory.CircleTrajectory2D
import space.kscience.trajectory.DubinsPose2D
import space.kscience.trajectory.Trajectory2D
import kotlin.math.PI
import kotlin.test.Test
import kotlin.test.assertEquals

class ArcTests {

    @Test
    fun arc() = with(Euclidean2DSpace) {
        val circle = Circle2D(vector(0.0, 0.0), 2.0)
        val arc = CircleTrajectory2D.of(
            circle.center,
            vector(-2.0, 0.0),
            vector(0.0, 2.0),
            Trajectory2D.R
        )
        assertEquals(circle.circumference / 4, arc.length, 1.0)
        assertEquals(0.0, arc.begin.bearing.degrees)
        assertEquals(90.0, arc.end.bearing.degrees)
    }

    @Test
    fun quarter() = with(Euclidean2DSpace) {
        val circle = circle(1, 0, 1)
        val arc = CircleTrajectory2D(
            circle,
            DubinsPose2D(x = 2.0, y = 1.2246467991473532E-16, bearing = PI.radians),
            DubinsPose2D(x = 1.0, y = -1.0, bearing = (PI*3/2).radians)
        )
        assertEquals(Trajectory2D.R, arc.direction)
        assertEquals(PI / 2, arc.length, 1e-4)
    }
}
