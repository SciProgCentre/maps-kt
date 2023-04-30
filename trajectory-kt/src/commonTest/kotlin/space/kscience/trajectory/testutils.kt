/*
 * Copyright 2018-2022 KMath contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package space.kscience.trajectory

import space.kscience.kmath.geometry.Euclidean2DSpace
import space.kscience.kmath.geometry.radians
import space.kscience.kmath.geometry.sin


fun assertEquals(expected: DubinsPose2D, actual: DubinsPose2D, precision: Double = 1e-6){
    kotlin.test.assertEquals(expected.x, actual.x, precision)
    kotlin.test.assertEquals(expected.y, actual.y, precision)
    kotlin.test.assertEquals(expected.bearing.radians, actual.bearing.radians, precision)
}

fun StraightTrajectory2D.inverse() = StraightTrajectory2D(end, begin)

fun StraightTrajectory2D.shift(shift: Int, width: Double): StraightTrajectory2D = with(Euclidean2DSpace) {
    val dX = width * sin(inverse().bearing)
    val dY = width * sin(bearing)

    return StraightTrajectory2D(
        vector(begin.x - dX * shift, begin.y - dY * shift),
        vector(end.x - dX * shift, end.y - dY * shift)
    )
}
