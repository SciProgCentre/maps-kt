/*
 * Copyright 2018-2023 KMath contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package space.kscience.trajectory

import space.kscience.kmath.geometry.Circle2D
import space.kscience.kmath.geometry.LineSegment2D
import space.kscience.kmath.geometry.Polygon
import space.kscience.kmath.geometry.Vector2D


public interface Obstacle {
    public val circles: List<Circle2D>
    public val center: Vector2D<Double>

    public fun intersects(segment: LineSegment2D): Boolean

    public companion object {
        public fun allPathsAvoiding(
            start: DubinsPose2D,
            finish: DubinsPose2D,
            trajectoryRadius: Double,
            vararg obstacles: Obstacle,
        ): List<CompositeTrajectory2D> {
            val obstacleShells: List<ObstacleShell> = obstacles.map { polygon ->
                ObstacleShell(polygon.circles)
            }
            return findAllPaths(start, trajectoryRadius, finish, trajectoryRadius, obstacleShells)
        }

        public fun allPathsAvoiding(
            start: DubinsPose2D,
            finish: DubinsPose2D,
            trajectoryRadius: Double,
            vararg obstacles: Polygon<Double>,
        ): List<CompositeTrajectory2D> {
            val obstacleShells: List<ObstacleShell> = obstacles.map { polygon ->
                ObstacleShell(polygon.points.map { Circle2D(it, trajectoryRadius) })
            }
            return findAllPaths(start, trajectoryRadius, finish, trajectoryRadius, obstacleShells)
        }

    }
}

public fun Obstacle(vararg circles: Circle2D): Obstacle = ObstacleShell(listOf(*circles))

//public fun Trajectory2D.intersects(
//    polygon: Polygon<Double>,
//    radius: Double,
//): Boolean {
//    val obstacle = Obstacle(polygon.points.map { point -> Circle2D(point, radius) })
//    return when (this) {
//        is CircleTrajectory2D -> {
//            val nearestCircle = obstacle.circles.minBy { it.center.distanceTo(circle.center) }
//
//        }
//        is StraightTrajectory2D -> obstacle.intersects(this)
//        is CompositeTrajectory2D -> segments.any { it.intersects(polygon, radius) }
//    }
//}





