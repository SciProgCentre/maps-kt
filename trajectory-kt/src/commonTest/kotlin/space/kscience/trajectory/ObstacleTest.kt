/*
 * Copyright 2018-2023 KMath contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package space.kscience.trajectory

import space.kscience.kmath.geometry.Angle
import space.kscience.kmath.geometry.Degrees
import space.kscience.kmath.geometry.degrees
import space.kscience.kmath.geometry.euclidean2d.Circle2D
import space.kscience.kmath.geometry.euclidean2d.Float64Space2D
import kotlin.math.PI
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ObstacleTest {

    @Test
    fun equalObstacles() = with(Float64Space2D) {
        val circle1 = Circle2D(vector(1.0, 6.5), 0.5)
        val circle2 = Circle2D(vector(1.0, 6.5), 0.5)
        assertEquals(circle1, circle2)
        val obstacle1 = Obstacle(circle1)
        val obstacle2 = Obstacle(circle2)
        assertEquals(obstacle1, obstacle2)
    }

    @Test
    fun singePoint() = with(Float64Space2D) {
        val outputTangents: List<Trajectory2D> = Obstacles.avoidObstacles(
            Pose2D(-5, -1, Angle.pi / 4),
            Pose2D(20, 4, Angle.pi * 3 / 4),
            0.5,
            Obstacle(Circle2D(vector(7.0, 1.0), 5.0))
        )
        assertTrue { outputTangents.isNotEmpty() }
        val length = outputTangents.minOf { it.length }
        assertEquals(26.0, length, 2.0)
    }

    @Test
    fun twoObstacles() = with(Float64Space2D) {
        val paths = Obstacles.avoidObstacles(
            Pose2D(-5, -1, Angle.pi / 4),
            Pose2D(20, 4, Angle.pi * 3 / 4),
            0.5,
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
        assertTrue { paths.isNotEmpty() }
        val length = paths.minOf { it.length }
        assertEquals(28.0, length, 2.0)
    }

    @Test
    fun circumvention() = with(Float64Space2D) {
        val obstacle = Obstacle(
            Circle2D(vector(0.0, 0.0), 1.0),
            Circle2D(vector(0.0, 1.0), 1.0),
            Circle2D(vector(1.0, 1.0), 1.0),
            Circle2D(vector(1.0, 0.0), 1.0)
        )

        val circumvention = obstacle.circumvention

        assertEquals(4, circumvention.segments.count { it is CircleTrajectory2D })

        assertEquals(4 + 2 * PI, circumvention.length, 1e-4)
    }

    @Test
    fun closePoints() = with(Float64Space2D) {
        val obstacle = Obstacle(
            Circle2D(vector(0.0, 0.0), 1.0),
            Circle2D(vector(0.0, 1.0), 1.0),
            Circle2D(vector(1.0, 1.0), 1.0),
            Circle2D(vector(1.0, 0.0), 1.0)
        )

        val paths: List<Trajectory2D> = Obstacles.avoidObstacles(
            Pose2D(-1, -1, Angle.pi),
            Pose2D(-1, -1, Angle.piDiv2),
            1.0,
            obstacle
        )
        assertTrue { paths.isNotEmpty() }
        assertEquals(9.5, paths.minOf { it.length }, 1.0)
        assertEquals(12.5, paths.maxOf { it.length }, 1.0)
    }

    @Test
    fun largeCoordinates() = with(Float64Space2D) {
        val startPoints = listOf(
            Pose2D(x = 484149.535516561, y = 2995086.2534208703, bearing = Degrees(3.401475378237137))
        )

        val endPoints = listOf(
            Pose2D(x = 456663.8489126448, y = 2830054.1087567504, bearing = 325.32183928982727.degrees),
            Pose2D(x = 473093.1426061879, y = 2898525.45250675, bearing = Degrees(100.36609537114623))
        )

        val obstacle = Obstacle(
            Circle2D(vector(x = 446088.2236175772, y = 2895264.0759535935), radius = 5000.0),
            Circle2D(vector(x = 455587.51549431164, y = 2897116.5594902174), radius = 5000.0),
            Circle2D(vector(x = 465903.08440141426, y = 2893897.500160981), radius = 5000.0),
            Circle2D(vector(x = 462421.19397653354, y = 2879496.4842121634), radius = 5000.0),
            Circle2D(vector(x = 449231.8047505464, y = 2880132.403305273), radius = 5000.0)
        )

        startPoints.forEach { start ->
            endPoints.forEach { end ->
                val paths = Obstacles.avoidObstacles(
                    start,
                    end,
                    5000.0,
                    obstacle
                )
                assertTrue { paths.isNotEmpty() }
            }
        }

    }

}