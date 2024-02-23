package space.kscience

import space.kscience.kmath.geometry.LineSegment2D
import space.kscience.kmath.geometry.Polygon
import space.kscience.kmath.geometry.Vector2D
import space.kscience.kmath.geometry.euclidean2d.Circle2D
import space.kscience.kmath.geometry.euclidean2d.Float64Space2D
import space.kscience.kmath.misc.zipWithNextCircular
import space.kscience.kmath.structures.Float64
import space.kscience.trajectory.Trajectory2D

public fun Float64Space2D.polygon(points: List<Vector2D<Double>>): Polygon<Vector2D<Float64>> =
    object : Polygon<Vector2D<Float64>> {
        override val points: List<Vector2D<Double>> get() = points
    }

public fun Float64Space2D.intersects(polygon: Polygon<Vector2D<Float64>>, segment: LineSegment2D): Boolean =
    polygon.points.zipWithNextCircular { l, r -> segment(l, r) }.any { intersects(it, segment) }

public fun Float64Space2D.intersects(polygon: Polygon<Vector2D<Float64>>, circle: Circle2D<Float64>): Boolean =
    polygon.points.zipWithNextCircular { l, r -> segment(l, r) }.any { intersects(it, circle) }

public fun Float64Space2D.intersectsTrajectory(polygon: Polygon<Vector2D<Float64>>, trajectory: Trajectory2D): Boolean =
    polygon.points.zipWithNextCircular { l, r ->
        segment(l, r)
    }.any { edge ->
        intersectsTrajectory(edge, trajectory)
    }