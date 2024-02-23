/*
 * Copyright 2018-2022 KMath contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
@file:UseSerializers(Float64Space2D.VectorSerializer::class)

package space.kscience.trajectory

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import space.kscience.intersects
import space.kscience.intersectsOrInside
import space.kscience.kmath.geometry.*
import space.kscience.kmath.geometry.euclidean2d.Circle2D
import space.kscience.kmath.geometry.euclidean2d.Float64Space2D
import space.kscience.kmath.geometry.euclidean2d.Float64Space2D.distanceTo
import space.kscience.kmath.structures.Float64
import space.kscience.tangent
import kotlin.math.atan2

@Serializable
public sealed interface Trajectory2D {
    public val length: Double

    public val beginPose: Pose2D
    public val endPose: Pose2D

    /**
     * Produce a trajectory with reversed order of points
     */
    public fun reversed(): Trajectory2D

    public sealed interface Type

    public sealed interface Direction : Type

    public object R : Direction {
        override fun toString(): String = "R"
    }

    public object S : Type {
        override fun toString(): String = "S"
    }

    public object L : Direction {
        override fun toString(): String = "L"
    }
}


public val Vector2D<Float64>.bearing: Angle get() = (atan2(x, y).radians).normalized()

/**
 * Straight path segment. The order of start and end defines the direction
 */
@Serializable
@SerialName("straight")
public data class StraightTrajectory2D(
    override val begin: Vector2D<Float64>,
    override val end: Vector2D<Float64>,
) : Trajectory2D, LineSegment2D {

    override val length: Double get() = begin.distanceTo(end)

    public val bearing: Angle get() = with(Float64Space2D) { (end - begin).bearing }

    override val beginPose: Pose2D get() = Pose2D(begin, bearing)
    override val endPose: Pose2D get() = Pose2D(end, bearing)

    override fun reversed(): StraightTrajectory2D = StraightTrajectory2D(end, begin)
}

public fun StraightTrajectory2D(segment: LineSegment2D): StraightTrajectory2D =
    StraightTrajectory2D(segment.begin, segment.end)

/**
 * An arc segment
 */
@Serializable
@SerialName("arc")
public data class CircleTrajectory2D(
    public val circle: Circle2D<Float64>,
    public val arcStart: Angle,
    public val arcAngle: Angle,
) : Trajectory2D {
    public val direction: Trajectory2D.Direction = if (arcAngle > Angle.zero) Trajectory2D.R else Trajectory2D.L

    public val arcEnd: Angle = arcStart + arcAngle
    override val beginPose: Pose2D get() = circle.tangent(arcStart, direction)
    override val endPose: Pose2D get() = circle.tangent(arcEnd, direction)

    override val length: Double by lazy {
        circle.radius * kotlin.math.abs(arcAngle.toRadians().value)
    }

    val center: Vector2D<Double> get() = circle.center


    override fun reversed(): CircleTrajectory2D = CircleTrajectory2D(circle, arcEnd, -arcAngle)

    public companion object
}

public fun CircleTrajectory2D(
    center: Vector2D<Float64>,
    start: Vector2D<Float64>,
    end: Vector2D<Float64>,
    direction: Trajectory2D.Direction,
): CircleTrajectory2D = with(Float64Space2D) {
    val startVector = start - center
    val endVector = end - center
    val startRadius = norm(startVector)
    val endRadius = norm(endVector)
    require((startRadius - endRadius) / startRadius < 1e-6) { "Start and end points have different radii" }
    val radius = (startRadius + endRadius) / 2
    val startBearing = startVector.bearing
    val endBearing = endVector.bearing
    CircleTrajectory2D(
        Circle2D(center, radius),
        startBearing,
        when (direction) {
            Trajectory2D.L -> if (endBearing >= startBearing) {
                endBearing - startBearing - Angle.piTimes2
            } else {
                endBearing - startBearing
            }

            Trajectory2D.R -> if (endBearing >= startBearing) {
                endBearing - startBearing
            } else {
                endBearing + Angle.piTimes2 - startBearing
            }
        }
    )
}

public fun CircleTrajectory2D(
    circle: Circle2D<Float64>,
    start: Vector2D<Float64>,
    end: Vector2D<Float64>,
    direction: Trajectory2D.Direction,
): CircleTrajectory2D = with(Float64Space2D) {
    val startVector = start - circle.center
    val endVector = end - circle.center
    val startBearing = startVector.bearing
    val endBearing = endVector.bearing
    CircleTrajectory2D(
        circle,
        startBearing,
        when (direction) {
            Trajectory2D.L -> if (endBearing >= startBearing) {
                endBearing - startBearing - Angle.piTimes2
            } else {
                endBearing - startBearing
            }

            Trajectory2D.R -> if (endBearing >= startBearing) {
                endBearing - startBearing
            } else {
                endBearing + Angle.piTimes2 - startBearing
            }
        }
    )
}

@Deprecated("Use angle notation instead")
public fun CircleTrajectory2D(
    circle: Circle2D<Float64>,
    beginPose: Pose2D,
    endPose: Pose2D,
): CircleTrajectory2D = with(Float64Space2D) {
    val vectorToBegin = beginPose - circle.center
    val vectorToEnd = endPose - circle.center
    //TODO check pose bearing
    return CircleTrajectory2D(circle, vectorToBegin.bearing, vectorToEnd.bearing - vectorToBegin.bearing)
}

@Serializable
@SerialName("composite")
public class CompositeTrajectory2D(public val segments: List<Trajectory2D>) : Trajectory2D {
    override val length: Double get() = segments.sumOf { it.length }

    override val beginPose: Pose2D get() = segments.first().beginPose
    override val endPose: Pose2D get() = segments.last().endPose

    override fun reversed(): CompositeTrajectory2D = CompositeTrajectory2D(segments.map { it.reversed() }.reversed())
}

public fun CompositeTrajectory2D(vararg segments: Trajectory2D): CompositeTrajectory2D =
    CompositeTrajectory2D(segments.toList())

public fun Float64Space2D.intersectsTrajectory(a: Trajectory2D, b: Trajectory2D): Boolean = when (a) {
    is CircleTrajectory2D -> when (b) {
        is CircleTrajectory2D -> intersectsOrInside(a.circle, b.circle)
        is StraightTrajectory2D -> intersects(a.circle, b)
        is CompositeTrajectory2D -> b.segments.any { intersectsTrajectory(it, a) }
    }

    is StraightTrajectory2D -> when (b) {
        is CircleTrajectory2D -> intersects(a, b.circle)
        is StraightTrajectory2D -> intersects(a, b)
        is CompositeTrajectory2D -> b.segments.any { intersectsTrajectory(it, a) }
    }

    is CompositeTrajectory2D -> a.segments.any { intersectsTrajectory(it, b) }
}
