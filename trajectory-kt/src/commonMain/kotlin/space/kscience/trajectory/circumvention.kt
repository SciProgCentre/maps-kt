package space.kscience.trajectory

import space.kscience.containsPoint
import space.kscience.kmath.geometry.*
import space.kscience.kmath.geometry.euclidean2d.Circle2D
import space.kscience.kmath.geometry.euclidean2d.Float64Space2D
import space.kscience.kmath.structures.Float64
import space.kscience.trajectory.DubinsPath.Type
import kotlin.math.*


/**
 * Create inner and outer tangents between two circles.
 * This method returns a map of segments using [DubinsPath] connection type notation.
 */
internal fun tangentsBetweenCircles(
    first: Circle2D<Float64>,
    second: Circle2D<Float64>,
): Map<Type, StraightTrajectory2D> = with(Float64Space2D) {
    // Distance between centers
    val distanceBetweenCenters: Double = first.center.distanceTo(second.center)

    // return empty map if one circle is inside another
    val minRadius = min(first.radius, second.radius)
    val maxRadius = max(first.radius, second.radius)

    val listOfTangents = when {
        // one circle inside another, no tangents
        distanceBetweenCenters + minRadius <= maxRadius -> return emptyMap()
        // circles intersect, only outer tangents
        distanceBetweenCenters - minRadius <= maxRadius -> listOf(Type.RSR, Type.LSL)
        // no intersections all tangents
        else -> listOf(Type.RSR, Type.RSL, Type.LSR, Type.LSL)
    }

    val angle1 = atan2(second.center.x - first.center.x, second.center.y - first.center.y)

    return listOfTangents.associateWith { route ->
        val r1 = when (route.first) {
            Trajectory2D.L -> -first.radius
            Trajectory2D.R -> first.radius
        }
        val r2 = when (route.third) {
            Trajectory2D.L -> -second.radius
            Trajectory2D.R -> second.radius
        }
        val r = if (r1.sign == r2.sign) {
            r1.absoluteValue - r2.absoluteValue
        } else {
            r1.absoluteValue + r2.absoluteValue
        }

        val l = sqrt(distanceBetweenCenters * distanceBetweenCenters - r * r)
        val angle2 = if (r1.absoluteValue > r2.absoluteValue) {
            angle1 + r1.sign * atan2(r.absoluteValue, l)
        } else {
            angle1 - r2.sign * atan2(r.absoluteValue, l)
        }
        val w = vector(-cos(angle2), sin(angle2))

        StraightTrajectory2D(
            first.center + w * r1,
            second.center + w * r2
        )
    }
}

internal fun tangentsBetweenArcs(
    first: CircleTrajectory2D,
    second: CircleTrajectory2D,
): Map<Type, StraightTrajectory2D> = tangentsBetweenCircles(first.circle, second.circle).filterValues {
    first.containsPoint(it.begin) && second.containsPoint(it.end)
}

/**
 * Create an obstacle circumvention in given [direction] starting (including) from obstacle node with given [fromIndex]
 */
public fun Obstacle.circumvention(direction: Trajectory2D.Direction, fromIndex: Int): CompositeTrajectory2D {
    require(fromIndex in arcs.indices) { "$fromIndex is not in ${arcs.indices}" }
    val startCircle = arcs[fromIndex].circle
    val segments = buildList {
        val reserve = mutableListOf<Trajectory2D>()

        val sourceSegments = when (direction) {
            Trajectory2D.L -> circumvention.reversed().segments
            Trajectory2D.R -> circumvention.segments
        }

        var i = 0
        while ((sourceSegments[i] as? CircleTrajectory2D)?.circle !== startCircle) {
            //put all segments before target circle on the reserve
            reserve.add(sourceSegments[i])
            i++
        }

        while (i < sourceSegments.size) {
            // put required segments on result list
            add(sourceSegments[i])
            i++
        }
        //add remaining segments from reserve
        addAll(reserve)
        check(i == sourceSegments.size)
    }
    return CompositeTrajectory2D(segments)
}

/**
 * Create an obstacle circumvention in given [direction] starting (including) from obstacle node with given [fromIndex]
 * and ending (including) at obstacle node with given [toIndex]
 */
public fun Obstacle.circumvention(
    direction: Trajectory2D.Direction,
    fromIndex: Int,
    toIndex: Int,
): CompositeTrajectory2D {
    require(toIndex in arcs.indices) { "$toIndex is not in ${arcs.indices}" }
    val toCircle = arcs[toIndex].circle
    val fullCircumvention = circumvention(direction, fromIndex).segments
    return CompositeTrajectory2D(
        buildList {
            var i = 0
            do {
                val segment = fullCircumvention[i]
                add(segment)
                i++
            } while ((segment as? CircleTrajectory2D)?.circle !== toCircle)
        }
    )
}