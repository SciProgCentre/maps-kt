/*
 * Copyright 2018-2023 KMath contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package space.kscience.trajectory

import space.kscience.intersectsTrajectory
import space.kscience.kmath.geometry.Angle
import space.kscience.kmath.geometry.Polygon
import space.kscience.kmath.geometry.Vector2D
import space.kscience.kmath.geometry.euclidean2d.Circle2D
import space.kscience.kmath.geometry.euclidean2d.Float64Space2D
import space.kscience.kmath.misc.zipWithNextCircular
import space.kscience.kmath.structures.Float64
import space.kscience.polygon


public interface Obstacle {
    public val arcs: List<CircleTrajectory2D>
    public val center: Vector2D<Double>

    /**
     * A closed right-handed circuit minimal path circumvention of the obstacle.
     */
    public val circumvention: CompositeTrajectory2D
//
//    /**
//     * A polygon created from the arc centers of the obstacle
//     */
//    public val core: Polygon<Double>

    public fun intersectsTrajectory(trajectory: Trajectory2D): Boolean

    public companion object {

    }
}

private class CircleObstacle(val circle: Circle2D<Float64>) : Obstacle {
    override val center: Vector2D<Double> get() = circle.center

    override val arcs: List<CircleTrajectory2D>
        get() = listOf(CircleTrajectory2D(circle, Angle.zero, Angle.piTimes2))

    override val circumvention: CompositeTrajectory2D
        get() = CompositeTrajectory2D(arcs)


    override fun intersectsTrajectory(trajectory: Trajectory2D): Boolean =
        Float64Space2D.intersectsTrajectory(circumvention, trajectory)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as CircleObstacle

        return circle == other.circle
    }

    override fun hashCode(): Int {
        return circle.hashCode()
    }

    override fun toString(): String = "Obstacle(circle=$circle)"


}

private class CoreObstacle(override val circumvention: CompositeTrajectory2D) : Obstacle {
    override val arcs: List<CircleTrajectory2D> by lazy {
        circumvention.segments.filterIsInstance<CircleTrajectory2D>()
    }

    override val center: Vector2D<Double> by lazy {
        Float64Space2D.vector(
            arcs.sumOf { it.center.x } / arcs.size,
            arcs.sumOf { it.center.y } / arcs.size
        )
    }

    val core: Polygon<Vector2D<Float64>> by lazy {
        Float64Space2D.polygon(arcs.map { it.circle.center })
    }

    override fun intersectsTrajectory(trajectory: Trajectory2D): Boolean =
        Float64Space2D.intersectsTrajectory(core, trajectory)


    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as CoreObstacle

        return arcs == other.arcs
    }

    override fun hashCode(): Int {
        return arcs.hashCode()
    }

    override fun toString(): String {
        return "Obstacle(circles=$arcs)"
    }
}

public fun Obstacle(circles: List<Circle2D<Float64>>): Obstacle = with(Float64Space2D) {
    require(circles.isNotEmpty()) { "Can't create circumvention for an empty obstacle" }
    //Create a single circle obstacle
    if(circles.size == 1) return CircleObstacle(circles.first())

    val center = vector(
        circles.sumOf { it.center.x },
        circles.sumOf { it.center.y }
    ) / circles.size

    if (circles.size == 1) {
        return CoreObstacle(
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

    val tangents = convex.zipWithNextCircular { a: Circle2D<Float64>, b: Circle2D<Float64> ->
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


    return CoreObstacle(circumvention)
}


public fun Obstacle(vararg circles: Circle2D<Float64>): Obstacle = Obstacle(listOf(*circles))

public fun Obstacle(points: List<Vector2D<Double>>, radius: Double): Obstacle =
    Obstacle(points.map { Circle2D(it, radius) })





