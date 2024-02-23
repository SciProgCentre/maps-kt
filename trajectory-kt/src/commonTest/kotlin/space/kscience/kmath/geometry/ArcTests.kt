/*
 * Copyright 2018-2022 KMath contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package space.kscience.kmath.geometry

import space.kscience.circle
import space.kscience.containsPoint
import space.kscience.kmath.geometry.euclidean2d.Circle2D
import space.kscience.kmath.geometry.euclidean2d.Float64Space2D
import space.kscience.kmath.geometry.euclidean2d.circumference
import space.kscience.trajectory.CircleTrajectory2D
import space.kscience.trajectory.Trajectory2D
import kotlin.math.PI
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ArcTests {

    @Test
    fun arc() = with(Float64Space2D) {
        val circle = Circle2D(vector(0.0, 0.0), 2.0)
        val arc = CircleTrajectory2D(
            circle.center,
            vector(-2.0, 0.0),
            vector(0.0, 2.0),
            Trajectory2D.R
        )
        assertEquals(circle.circumference / 4, arc.length, 1.0)
        assertEquals(0.0, arc.beginPose.bearing.toDegrees().value)
        assertEquals(90.0, arc.endPose.bearing.toDegrees().value)
    }

    @Test
    fun quarter() = with(Float64Space2D) {
        val circle = circle(1, 0, 1)
        val arc = CircleTrajectory2D(
            circle,
            (PI/2).radians,
            (PI/2).radians
        )
        assertEquals(Trajectory2D.R, arc.direction)
        assertEquals(PI, arc.arcEnd.toRadians().value, 1e-4)
    }

    @Test
    fun arcContains() = with(Float64Space2D) {
        val circle = circle(0, 0, 1.0)

        val arc1 = CircleTrajectory2D(circle, Angle.pi / 4, Angle.piDiv2)
        assertTrue { arc1.containsPoint(vector(1, 0)) }
        assertFalse { arc1.containsPoint(vector(0, 1)) }
        assertFalse { arc1.containsPoint(vector(-1, 0)) }

        val arc2 = CircleTrajectory2D(circle, Angle.pi / 4, -Angle.piDiv2 * 3)
        assertEquals(Trajectory2D.L, arc2.direction)
        assertFalse { arc2.containsPoint(vector(1, 0)) }
        assertTrue { arc2.containsPoint(vector(0, 1)) }
        assertTrue { arc2.containsPoint(vector(-1, 0)) }

    }
}
