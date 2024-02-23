/*
 * Copyright 2018-2022 KMath contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package space.kscience.kmath.geometry

import space.kscience.kmath.geometry.euclidean2d.Float64Space2D
import space.kscience.trajectory.StraightTrajectory2D
import kotlin.math.pow
import kotlin.math.sqrt
import kotlin.test.Test
import kotlin.test.assertEquals

class LineTests {

    @Test
    fun lineTest() = with(Float64Space2D){
        val straight = StraightTrajectory2D(vector(0.0, 0.0), vector(100.0, 100.0))
        assertEquals(sqrt(100.0.pow(2) + 100.0.pow(2)), straight.length)
        assertEquals(45.0, straight.bearing.toDegrees().value)
    }

    @Test
    fun lineAngleTest() = with(Float64Space2D){
        //val zero = Vector2D(0.0, 0.0)
        val north = StraightTrajectory2D(zero, vector(0.0, 2.0))
        assertEquals(0.0, north.bearing.toDegrees().value)
        val east = StraightTrajectory2D(zero, vector(2.0, 0.0))
        assertEquals(90.0, east.bearing.toDegrees().value)
        val south = StraightTrajectory2D(zero, vector(0.0, -2.0))
        assertEquals(180.0, south.bearing.toDegrees().value)
        val west = StraightTrajectory2D(zero, vector(-2.0, 0.0))
        assertEquals(270.0, west.bearing.toDegrees().value)
    }
}
