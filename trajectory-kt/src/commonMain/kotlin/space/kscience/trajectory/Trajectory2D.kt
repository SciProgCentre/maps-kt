/*
 * Copyright 2018-2022 KMath contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
@file:UseSerializers(Euclidean2DSpace.VectorSerializer::class)

package space.kscience.trajectory

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import space.kscience.kmath.geometry.*
import space.kscience.kmath.geometry.Euclidean2DSpace.distanceTo
import space.kscience.kmath.geometry.Euclidean2DSpace.minus
import kotlin.math.atan2

@Serializable
public sealed interface Trajectory2D {
    public val length: Double

    public val beginPose: DubinsPose2D
    public val endPose: DubinsPose2D

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


public val DoubleVector2D.bearing: Angle get() = (atan2(x, y).radians).normalized()

/**
 * Straight path segment. The order of start and end defines the direction
 */
@Serializable
@SerialName("straight")
public data class StraightTrajectory2D(
    override val begin: DoubleVector2D,
    override val end: DoubleVector2D,
) : Trajectory2D, LineSegment2D {

    override val length: Double get() = begin.distanceTo(end)

    public val bearing: Angle get() = (end - begin).bearing

    override val beginPose: DubinsPose2D get() = DubinsPose2D(begin, bearing)
    override val endPose: DubinsPose2D get() = DubinsPose2D(end, bearing)

    override fun reversed(): StraightTrajectory2D = StraightTrajectory2D(end, begin)
}

public fun StraightTrajectory2D(segment: LineSegment2D): StraightTrajectory2D =
    StraightTrajectory2D(segment.begin, segment.end)

/**
 * An arc segment
 */
@Serializable
@SerialName("arc")
public data class CircleTrajectory2D (
    public val circle: Circle2D,
    public val begin: DubinsPose2D,
    public val end: DubinsPose2D,
) : Trajectory2D {

    override val beginPose: DubinsPose2D get() = begin
    override val endPose: DubinsPose2D get() = end

    /**
     * Arc length in radians
     */
    val arcAngle: Angle
        get() = if (direction == Trajectory2D.L) {
            begin.bearing - end.bearing
        } else {
            end.bearing - begin.bearing
        }.normalized()


    override val length: Double by lazy {
        circle.radius * arcAngle.radians
    }

    public val direction: Trajectory2D.Direction by lazy {
        when {
            begin.y < circle.center.y -> if (begin.bearing > Angle.pi) Trajectory2D.R else Trajectory2D.L
            begin.y > circle.center.y -> if (begin.bearing < Angle.pi) Trajectory2D.R else Trajectory2D.L
            else -> if (begin.bearing == Angle.zero) {
                if (begin.x < circle.center.x) Trajectory2D.R else Trajectory2D.L
            } else {
                if (begin.x > circle.center.x) Trajectory2D.R else Trajectory2D.L
            }
        }
    }

    override fun reversed(): CircleTrajectory2D = CircleTrajectory2D(circle, end.reversed(), begin.reversed())

    public companion object {
        public fun of(
            center: DoubleVector2D,
            start: DoubleVector2D,
            end: DoubleVector2D,
            direction: Trajectory2D.Direction,
        ): CircleTrajectory2D {
            fun calculatePose(
                vector: DoubleVector2D,
                theta: Angle,
                direction: Trajectory2D.Direction,
            ): DubinsPose2D = DubinsPose2D(
                vector,
                when (direction) {
                    Trajectory2D.L -> (theta - Angle.piDiv2).normalized()
                    Trajectory2D.R -> (theta + Angle.piDiv2).normalized()
                }
            )

            val s1 = StraightTrajectory2D(center, start)
            val s2 = StraightTrajectory2D(center, end)
            val pose1 = calculatePose(start, s1.bearing, direction)
            val pose2 = calculatePose(end, s2.bearing, direction)
            val trajectory = CircleTrajectory2D(Circle2D(center, s1.length), pose1, pose2)
            if (trajectory.direction != direction) error("Trajectory direction mismatch")
            return trajectory
        }
    }
}

@Serializable
@SerialName("composite")
public class CompositeTrajectory2D(public val segments: List<Trajectory2D>) : Trajectory2D {
    override val length: Double get() = segments.sumOf { it.length }

    override val beginPose: DubinsPose2D get() = segments.first().beginPose
    override val endPose: DubinsPose2D get() = segments.last().endPose

    override fun reversed(): CompositeTrajectory2D = CompositeTrajectory2D(segments.map { it.reversed() }.reversed())
}

public fun CompositeTrajectory2D(vararg segments: Trajectory2D): CompositeTrajectory2D =
    CompositeTrajectory2D(segments.toList())

public fun Euclidean2DSpace.trajectoryIntersects(a: Trajectory2D, b: Trajectory2D): Boolean = when (a) {
    is CircleTrajectory2D -> when (b) {
        is CircleTrajectory2D -> intersectsOrInside(a.circle, b.circle)
        is StraightTrajectory2D -> intersects(a.circle, b)
        is CompositeTrajectory2D -> b.segments.any { trajectoryIntersects(it, b) }
    }

    is StraightTrajectory2D -> when (b) {
        is CircleTrajectory2D -> intersects(a, b.circle)
        is StraightTrajectory2D -> intersects(a, b)
        is CompositeTrajectory2D -> b.segments.any { trajectoryIntersects(it, b) }
    }

    is CompositeTrajectory2D -> a.segments.any { trajectoryIntersects(it, b) }
}
