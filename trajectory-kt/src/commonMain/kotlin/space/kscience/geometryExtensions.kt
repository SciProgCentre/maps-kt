package space.kscience

import space.kscience.kmath.geometry.*
import space.kscience.kmath.geometry.euclidean2d.Circle2D
import space.kscience.kmath.geometry.euclidean2d.Float64Space2D
import space.kscience.kmath.structures.Float64
import space.kscience.trajectory.*
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sign
import kotlin.math.sqrt

public fun Float64Space2D.circle(x: Number, y: Number, radius: Number): Circle2D<Float64> =
    Circle2D(vector(x, y), radius = radius.toDouble())

public fun Float64Space2D.segment(begin: Vector2D<Float64>, end: Vector2D<Float64>): LineSegment2D =
    LineSegment(begin, end)

public fun Float64Space2D.segment(x1: Number, y1: Number, x2: Number, y2: Number): LineSegment2D =
    LineSegment(vector(x1, y1), vector(x2, y2))

public fun Float64Space2D.intersectsOrInside(circle1: Circle2D<Float64>, circle2: Circle2D<Float64>): Boolean {
    val distance = norm(circle2.center - circle1.center)
    return distance <= circle1.radius + circle2.radius
}

/**
 * https://mathworld.wolfram.com/Circle-LineIntersection.html
 */
public fun Float64Space2D.intersects(segment: LineSegment2D, circle: Circle2D<Float64>): Boolean {
    val direction = segment.end - segment.begin
    val radiusVector = segment.begin - circle.center

    val a = direction dot direction
    val b = 2 * (radiusVector dot direction)
    val c = (radiusVector dot radiusVector) - circle.radius.pow(2)

    val discriminantSquared = b * b - 4 * a * c
    if (discriminantSquared < 0) return false

    val discriminant = sqrt(discriminantSquared)

    val t1 = (-b - discriminant) / (2 * a) // first intersection point in relative coordinates
    val t2 = (-b + discriminant) / (2 * a) //second intersection point in relative coordinates

    return t1 in 0.0..1.0 || t2 in 0.0..1.0
}


public fun Float64Space2D.intersects(circle: Circle2D<Float64>, segment: LineSegment2D): Boolean =
    intersects(segment, circle)

public fun Float64Space2D.intersects(segment1: LineSegment2D, segment2: LineSegment2D): Boolean {
    infix fun Vector2D<Float64>.cross(v2: Vector2D<Float64>): Double = x * v2.y - y * v2.x
    infix fun Vector2D<Float64>.crossSign(v2: Vector2D<Float64>) = cross(v2).sign

    return with(Float64Space2D) {
        (segment2.begin - segment1.begin) crossSign (segment2.end - segment1.begin) !=
                (segment2.begin - segment1.end) crossSign (segment2.end - segment1.end) &&
                (segment1.begin - segment2.begin) crossSign (segment1.end - segment2.begin) !=
                (segment1.begin - segment2.end) crossSign (segment1.end - segment2.end)
    }
}

public fun Float64Space2D.intersectsTrajectory(segment: LineSegment2D, trajectory: Trajectory2D): Boolean =
    when (trajectory) {
        is CircleTrajectory2D -> intersects(segment, trajectory.circle)
        is StraightTrajectory2D -> intersects(segment, trajectory)
        is CompositeTrajectory2D -> trajectory.segments.any { trajectorySegment ->
            intersectsTrajectory(segment, trajectorySegment)
        }
    }

/**
 * Compute tangent pose to a circle
 *
 * @param bearing is counted the same way as in [Pose2D], from positive y clockwise
 */
public fun Circle2D<Float64>.tangent(bearing: Angle, direction: Trajectory2D.Direction): Pose2D = with(Float64Space2D) {
    val coordinates: Vector2D<Double> = vector(center.x + radius * sin(bearing), center.y + radius * cos(bearing))
    val tangentAngle = when (direction) {
        Trajectory2D.R -> bearing + Angle.piDiv2
        Trajectory2D.L -> bearing - Angle.piDiv2
    }.normalized()
    Pose2D(coordinates, tangentAngle)
}


public fun CircleTrajectory2D.containsPoint(point: Vector2D<Float64>): Boolean = with(Float64Space2D) {
    val radiusVector = point - center
    if (abs(norm(radiusVector) - circle.radius) > 1e-4 * circle.radius) error("Wrong radius")
    val radiusVectorBearing = radiusVector.bearing
    val offset = (radiusVectorBearing - arcStart).normalized()
    when {
        arcAngle >= Angle.zero -> offset < arcAngle
        else -> arcAngle < offset - Angle.piTimes2
    }
}
