/*
 * Copyright 2018-2023 KMath contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package space.kscience.trajectory

import space.kscience.kmath.geometry.*
import space.kscience.kmath.misc.zipWithNextCircular


public interface Obstacle {
    public val arcs: List<CircleTrajectory2D>
    public val center: Vector2D<Double>

    /**
     * A closed right-handed circuit minimal path circumvention of an obstacle.
     */
    public val circumvention: CompositeTrajectory2D

    public val polygon: Polygon<Double>


    /**
     * Check if obstacle has intersection with given [Trajectory2D]
     */
    public fun intersects(trajectory: Trajectory2D): Boolean =
        Euclidean2DSpace.intersectsTrajectory(polygon, trajectory)


    public companion object {

    }
}

private class ObstacleImpl(override val circumvention: CompositeTrajectory2D) : Obstacle {
    override val arcs: List<CircleTrajectory2D> by lazy {
        circumvention.segments.filterIsInstance<CircleTrajectory2D>()
    }

    override val center: Vector2D<Double> by lazy {
        Euclidean2DSpace.vector(
            arcs.sumOf { it.center.x } / arcs.size,
            arcs.sumOf { it.center.y } / arcs.size
        )
    }

    override val polygon: Polygon<Double> by lazy {
        Euclidean2DSpace.polygon(arcs.map { it.circle.center })
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as ObstacleImpl

        return arcs == other.arcs
    }

    override fun hashCode(): Int {
        return arcs.hashCode()
    }

    override fun toString(): String {
        return "Obstacle(circles=$arcs)"
    }
}

public fun Obstacle(circles: List<Circle2D>): Obstacle = with(Euclidean2DSpace) {
    val center = vector(
        circles.sumOf { it.center.x },
        circles.sumOf { it.center.y }
    )/ circles.size


    require(circles.isNotEmpty()) { "Can't create circumvention for an empty obstacle" }

    if (circles.size == 1) {
        return ObstacleImpl(
            CompositeTrajectory2D(
                CircleTrajectory2D(circles.first(), Angle.zero, Angle.piTimes2)
            )
        )
    }

    //TODO use convex hull
    //distinct and sorted in right-handed direction
    val convex = circles.distinct().sortedBy {
        (it.center - center).bearing
    }

    val tangents = convex.zipWithNextCircular { a: Circle2D, b: Circle2D ->
        tangentsBetweenCircles(a, b)[DubinsPath.Type.RSR]
            ?: error("Can't find right handed circumvention")
    }

    val trajectory: List<Trajectory2D> = buildList {
        for (i in 0 until tangents.lastIndex) {
            add(tangents[i])
            add(CircleTrajectory2D(convex[i + 1], tangents[i].endPose, tangents[i + 1].beginPose, Trajectory2D.R))
        }
        add(tangents.last())
        add(CircleTrajectory2D(convex[0], tangents.last().endPose, tangents.first().beginPose, Trajectory2D.R))
    }

    val circumvention = CompositeTrajectory2D(trajectory)


    return ObstacleImpl(circumvention)
}


public fun Obstacle(vararg circles: Circle2D): Obstacle = Obstacle(listOf(*circles))

public fun Obstacle(points: List<Vector2D<Double>>, radius: Double): Obstacle =
    Obstacle(points.map { Circle2D(it, radius) })





