package space.kscience.kmath.geometry

import space.kscience.kmath.misc.zipWithNextCircular
import space.kscience.trajectory.Trajectory2D

public fun Euclidean2DSpace.polygon(points: List<DoubleVector2D>): Polygon<Double> = object : Polygon<Double> {
    override val points: List<Vector2D<Double>> get() = points
}

public fun Euclidean2DSpace.intersects(polygon: Polygon<Double>, segment: LineSegment2D): Boolean =
    polygon.points.zipWithNextCircular { l, r -> segment(l, r) }.any { intersects(it, segment) }

public fun Euclidean2DSpace.intersects(polygon: Polygon<Double>, circle: Circle2D): Boolean =
    polygon.points.zipWithNextCircular { l, r -> segment(l, r) }.any { intersects(it, circle) }

public fun Euclidean2DSpace.intersectsTrajectory(polygon: Polygon<Double>, trajectory: Trajectory2D): Boolean =
    polygon.points.zipWithNextCircular { l, r ->
        segment(l, r)
    }.any { edge ->
        intersectsTrajectory(edge, trajectory)
    }