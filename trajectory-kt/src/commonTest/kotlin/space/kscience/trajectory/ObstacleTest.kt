/*
 * Copyright 2018-2023 KMath contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package space.kscience.trajectory

import space.kscience.kmath.geometry.Circle2D
import space.kscience.kmath.geometry.Euclidean2DSpace.vector
import kotlin.test.Test
import kotlin.test.assertEquals

class ObstacleTest {
    @Test
    fun firstPath() {
        val startPoint = vector(-5.0, -1.0)
        val startDirection = vector(1.0, 1.0)
        val startRadius = 0.5
        val finalPoint = vector(20.0, 4.0)
        val finalDirection = vector(1.0, -1.0)

        val outputTangents = Obstacle.allPathsAvoiding(
            DubinsPose2D.of(startPoint, startDirection),
            DubinsPose2D.of(finalPoint, finalDirection),
            startRadius,
            Obstacle(Circle2D(vector(7.0, 1.0), 5.0))
        )
        val length = outputTangents.minOf { it.length }
        assertEquals(27.2113183, length, 1e-6)
    }

    @Test
    fun secondPath() {
        val startPoint = vector(-5.0, -1.0)
        val startDirection = vector(1.0, 1.0)
        val radius = 0.5
        val finalPoint = vector(20.0, 4.0)
        val finalDirection = vector(1.0, -1.0)

        val paths = Obstacle.allPathsAvoiding(
            DubinsPose2D.of(startPoint, startDirection),
            DubinsPose2D.of(finalPoint, finalDirection),
            radius,
            Obstacle(
                Circle2D(vector(1.0, 6.5), 0.5),
                Circle2D(vector(2.0, 1.0), 0.5),
                Circle2D(vector(6.0, 0.0), 0.5),
                Circle2D(vector(5.0, 5.0), 0.5)
            ), Obstacle(
                Circle2D(vector(10.0, 1.0), 0.5),
                Circle2D(vector(16.0, 0.0), 0.5),
                Circle2D(vector(14.0, 6.0), 0.5),
                Circle2D(vector(9.0, 4.0), 0.5)
            )
        )
        val length = paths.minOf { it.length }
        assertEquals(28.9678224, length, 1e-6)
    }

    @Test
    fun nearPoints() {
        val startPoint = vector(-1.0, 0.0)
        val startDirection = vector(0.0, 1.0)
        val startRadius = 1.0
        val finalPoint = vector(0, -1)
        val finalDirection = vector(1.0, 0)

        val paths = Obstacle.allPathsAvoiding(
            DubinsPose2D.of(startPoint, startDirection),
            DubinsPose2D.of(finalPoint, finalDirection),
            startRadius,
            Obstacle(
                Circle2D(vector(0.0, 0.0), 1.0),
                Circle2D(vector(0.0, 1.0), 1.0),
                Circle2D(vector(1.0, 1.0), 1.0),
                Circle2D(vector(1.0, 0.0), 1.0)
            )
        )
        val length = paths.minOf { it.length }
        println(length)
        //assertEquals(28.9678224, length, 1e-6)
    }

    @Test
    fun equalObstacles() {
        val circle1 = Circle2D(vector(1.0, 6.5), 0.5)
        val circle2 = Circle2D(vector(1.0, 6.5), 0.5)
        assertEquals(circle1, circle2)
        val obstacle1 = ObstacleShell(listOf(circle1))
        val obstacle2 = ObstacleShell(listOf(circle2))
        assertEquals(obstacle1, obstacle2)
    }

}