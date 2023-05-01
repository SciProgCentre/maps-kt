package space.kscience.kmath.geometry

import space.kscience.kmath.operations.DoubleField.pow
import space.kscience.trajectory.Pose2D
import space.kscience.trajectory.Trajectory2D
import kotlin.math.sign

public fun Euclidean2DSpace.circle(x: Number, y: Number, radius: Number): Circle2D =
    Circle2D(vector(x, y), radius = radius.toDouble())

public fun Euclidean2DSpace.segment(begin: DoubleVector2D, end: DoubleVector2D): LineSegment2D =
    LineSegment(begin, end)

public fun Euclidean2DSpace.segment(x1: Number, y1: Number, x2: Number, y2: Number): LineSegment2D =
    LineSegment(vector(x1, y1), vector(x2, y2))

public fun Euclidean2DSpace.intersectsOrInside(circle1: Circle2D, circle2: Circle2D): Boolean {
    val distance = norm(circle2.center - circle1.center)
    return distance <= circle1.radius + circle2.radius
}

/**
 * https://mathworld.wolfram.com/Circle-LineIntersection.html
 */
public fun Euclidean2DSpace.intersects(segment: LineSegment2D, circle: Circle2D): Boolean {
    val begin = segment.begin
    val end = segment.end
    val d = begin.distanceTo(end)
    val det = (begin.x - circle.center.x) * (end.y - circle.center.y) -
            (end.x - circle.center.x) * (begin.y - circle.center.y)

    val incidence = circle.radius.pow(2) * d.pow(2) - det.pow(2)

    return incidence >= 0
}

public fun Euclidean2DSpace.intersects(circle: Circle2D, segment: LineSegment2D): Boolean =
    intersects(segment, circle)

public fun Euclidean2DSpace.intersects(segment1: LineSegment2D, segment2: LineSegment2D): Boolean {
    infix fun DoubleVector2D.cross(v2: DoubleVector2D): Double = x * v2.y - y * v2.x
    infix fun DoubleVector2D.crossSign(v2: DoubleVector2D) = cross(v2).sign

    return with(Euclidean2DSpace) {
        (segment2.begin - segment1.begin) crossSign (segment2.end - segment1.begin) !=
                (segment2.begin - segment1.end) crossSign (segment2.end - segment1.end) &&
                (segment1.begin - segment2.begin) crossSign (segment1.end - segment2.begin) !=
                (segment1.begin - segment2.end) crossSign (segment1.end - segment2.end)
    }
}

/**
 * Compute tangent pose to a circle
 *
 * @param bearing is counted the same way as in [Pose2D], from positive y clockwise
 */
public fun Circle2D.tangent(bearing: Angle, direction: Trajectory2D.Direction): Pose2D = with(Euclidean2DSpace) {
    val coordinates: Vector2D<Double> = vector(center.x + radius * sin(bearing), center.y + radius * cos(bearing))
    val tangentAngle = when (direction) {
        Trajectory2D.R -> bearing + Angle.piDiv2
        Trajectory2D.L -> bearing - Angle.piDiv2
    }.normalized()
    Pose2D(coordinates, tangentAngle)
}

