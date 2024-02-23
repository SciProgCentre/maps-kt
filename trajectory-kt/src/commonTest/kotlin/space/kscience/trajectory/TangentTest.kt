/*
 * Copyright 2018-2023 KMath contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package space.kscience.trajectory

import space.kscience.kmath.geometry.LineSegment
import space.kscience.kmath.geometry.equalsLine
import space.kscience.kmath.geometry.euclidean2d.Circle2D
import space.kscience.kmath.geometry.euclidean2d.Float64Space2D
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TangentTest {
    @Test
    fun tangents() = with(Float64Space2D) {
        val c1 = Circle2D(vector(0.0, 0.0), 1.0)
        val c2 = Circle2D(vector(4.0, 0.0), 1.0)
        val routes = listOf(
            DubinsPath.Type.RSR,
            DubinsPath.Type.RSL,
            DubinsPath.Type.LSR,
            DubinsPath.Type.LSL
        )
        val segments = listOf(
            LineSegment(
                begin = vector(0.0, 1.0),
                end = vector(4.0, 1.0)
            ),
            LineSegment(
                begin = vector(0.5, 0.8660254),
                end = vector(3.5, -0.8660254)
            ),
            LineSegment(
                begin = vector(0.5, -0.8660254),
                end = vector(3.5, 0.8660254)
            ),
            LineSegment(
                begin = vector(0.0, -1.0),
                end = vector(4.0, -1.0)
            )
        )

        val tangentMap = tangentsBetweenCircles(c1, c2)
        val tangentMapKeys = tangentMap.keys.toList()
        val tangentMapValues = tangentMap.values.toList()

        assertEquals(routes, tangentMapKeys)
        for (i in segments.indices) {
            assertTrue(segments[i].equalsLine(Float64Space2D, tangentMapValues[i]))
        }
    }

    @Test
    fun concentric() = with(Float64Space2D) {
        val c1 = Circle2D(vector(0.0, 0.0), 10.0)
        val c2 = Circle2D(vector(0.0, 0.0), 1.0)
        assertEquals(emptyMap(), tangentsBetweenCircles(c1, c2))
    }
}