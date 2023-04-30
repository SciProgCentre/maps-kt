/*
 * Copyright 2018-2023 KMath contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package space.kscience.trajectory

import space.kscience.kmath.geometry.Angle
import space.kscience.kmath.geometry.Circle2D
import space.kscience.kmath.geometry.Euclidean2DSpace
import space.kscience.kmath.geometry.Vector2D
import space.kscience.kmath.misc.zipWithNextCircular


public interface Obstacle {
    public val circles: List<Circle2D>
    public val center: Vector2D<Double>

    public val circumvention: CompositeTrajectory2D

    /**
     * Check if obstacle has intersection with given [Trajectory2D]
     */
    public fun intersects(trajectory: Trajectory2D): Boolean =
        Euclidean2DSpace.trajectoryIntersects(circumvention, trajectory)


    public companion object {

    }
}

private class ObstacleImpl(override val circles: List<Circle2D>) : Obstacle {
    override val center: Vector2D<Double> by lazy {
        Euclidean2DSpace.vector(
            circles.sumOf { it.center.x } / circles.size,
            circles.sumOf { it.center.y } / circles.size
        )
    }

    override val circumvention: CompositeTrajectory2D by lazy {
        with(Euclidean2DSpace) {
            /**
             * A closed right-handed circuit minimal path circumvention of an obstacle.
             * @return null if number of distinct circles in the obstacle is less than
             */
            require(circles.isNotEmpty()) { "Can't create circumvention for an empty obstacle" }

            if (circles.size == 1) {
                // a circumvention consisting of a single circle, starting on top
                val circle = circles.first()
                val top = vector(circle.center.x + circle.radius, circle.center.y)
                val startEnd = DubinsPose2D(
                    top,
                    Angle.piDiv2
                )
                return@lazy CompositeTrajectory2D(
                    CircleTrajectory2D(circle, startEnd, startEnd)
                )
            }

            //TODO use convex hull
            //distinct and sorted in right-handed direction
            val circles = circles.distinct().sortedBy {
                (it.center - center).bearing
            }

            val tangents = circles.zipWithNextCircular { a: Circle2D, b: Circle2D ->
                tangentsBetweenCircles(a, b)[DubinsPath.Type.RSR]
                    ?: error("Can't find right handed circumvention")
            }

            val trajectory: List<Trajectory2D> = buildList {
                for (i in 0 until tangents.lastIndex) {
                    add(tangents[i])
                    add(CircleTrajectory2D(circles[i + 1], tangents[i].endPose, tangents[i + 1].beginPose))
                }
                add(tangents.last())
                add(CircleTrajectory2D(circles[0], tangents.last().endPose, tangents.first().beginPose))
            }

            return@lazy CompositeTrajectory2D(trajectory)

        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as ObstacleImpl

        return circles == other.circles
    }

    override fun hashCode(): Int {
        return circles.hashCode()
    }

    override fun toString(): String {
        return "Obstacle(circles=$circles)"
    }


}

public fun Obstacle(vararg circles: Circle2D): Obstacle = ObstacleImpl(listOf(*circles))

public fun Obstacle(points: List<Vector2D<Double>>, radius: Double): Obstacle =
    ObstacleImpl(points.map { Circle2D(it, radius) })





