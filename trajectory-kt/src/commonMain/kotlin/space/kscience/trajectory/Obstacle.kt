/*
 * Copyright 2018-2023 KMath contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package space.kscience.trajectory

import space.kscience.kmath.geometry.*
import space.kscience.kmath.geometry.Euclidean2DSpace.distanceTo
import space.kscience.kmath.geometry.Euclidean2DSpace.minus
import space.kscience.kmath.geometry.Euclidean2DSpace.norm
import space.kscience.kmath.geometry.Euclidean2DSpace.plus
import space.kscience.kmath.geometry.Euclidean2DSpace.times
import space.kscience.kmath.geometry.Euclidean2DSpace.vector
import space.kscience.kmath.misc.zipWithNextCircular
import space.kscience.kmath.operations.DoubleField.pow
import kotlin.math.*

internal data class Tangent(
    val startCircle: Circle2D,
    val endCircle: Circle2D,
    val startObstacle: Obstacle,
    val endObstacle: Obstacle,
    val lineSegment: LineSegment2D,
    val startDirection: Trajectory2D.Direction,
    val endDirection: Trajectory2D.Direction = startDirection,
) : LineSegment2D by lineSegment


private class LR<T>(val l: T, val r: T) {
    operator fun get(direction: Trajectory2D.Direction) = when (direction) {
        Trajectory2D.L -> l
        Trajectory2D.R -> r
    }
}

private class TangentPath(val tangents: List<Tangent>) {
    fun last() = tangents.last()
}

private fun TangentPath(vararg tangents: Tangent) = TangentPath(listOf(*tangents))

/**
 * Create inner and outer tangents between two circles.
 * This method returns a map of segments using [DubinsPath] connection type notation.
 */
internal fun tangentsBetweenCircles(
    first: Circle2D,
    second: Circle2D,
): Map<DubinsPath.Type, LineSegment2D> = with(Euclidean2DSpace) {
    //return empty map for concentric circles
    if (first.center.equalsVector(second.center)) return emptyMap()

    // A line connecting centers
    val line = LineSegment(first.center, second.center)
    // Distance between centers
    val distance = line.begin.distanceTo(line.end)
    val angle1 = atan2(second.center.x - first.center.x, second.center.y - first.center.y)
    var angle2: Double

    return listOf(
        DubinsPath.Type.RSR,
        DubinsPath.Type.RSL,
        DubinsPath.Type.LSR,
        DubinsPath.Type.LSL
    ).associateWith { route ->
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
        if (distance * distance < r * r) error("Circles should not intersect")

        val l = sqrt(distance * distance - r * r)
        angle2 = if (r1.absoluteValue > r2.absoluteValue) {
            angle1 + r1.sign * atan2(r.absoluteValue, l)
        } else {
            angle1 - r2.sign * atan2(r.absoluteValue, l)
        }
        val w = vector(-cos(angle2), sin(angle2))

        LineSegment(
            first.center + w * r1,
            second.center + w * r2
        )
    }
}

internal class Obstacle(
    public val circles: List<Circle2D>,
) {

    public val center: Vector2D<Double> = vector(
        circles.sumOf { it.center.x } / circles.size,
        circles.sumOf { it.center.y } / circles.size
    )

    internal val tangents: List<LineSegment2D>
    public val direction: Trajectory2D.Direction

    init {
        if (circles.size < 2) {
            tangents = emptyList()
            direction = Trajectory2D.R
        } else {
            val lslTangents = circles.zipWithNextCircular { a, b ->
                tangentsBetweenCircles(a, b)[DubinsPath.Type.LSL]!!
            }
            val rsrTangents = circles.zipWithNextCircular { a, b ->
                tangentsBetweenCircles(a, b)[DubinsPath.Type.RSR]!!
            }
            val center = vector(
                circles.sumOf { it.center.x } / circles.size,
                circles.sumOf { it.center.y } / circles.size
            )
            val lslToCenter =
                lslTangents.sumOf { it.begin.distanceTo(center) } + lslTangents.sumOf { it.end.distanceTo(center) }
            val rsrToCenter =
                rsrTangents.sumOf { it.begin.distanceTo(center) } + rsrTangents.sumOf { it.end.distanceTo(center) }

            if (rsrToCenter >= lslToCenter) {
                this.tangents = rsrTangents
                this.direction = Trajectory2D.R
            } else {
                this.tangents = lslTangents
                this.direction = Trajectory2D.L
            }
        }
    }

    internal fun nextTangent(circle: Circle2D, direction: Trajectory2D.Direction): Tangent {
        val circleIndex = circles.indexOf(circle)
        if (circleIndex == -1) error("Circle does not belong to this tangent")

        val nextCircleIndex = if (direction == this.direction) {
            if (circleIndex == circles.lastIndex) 0 else circleIndex + 1
        } else {
            if (circleIndex == 0) circles.lastIndex else circleIndex - 1
        }

        return Tangent(
            circle,
            circles[nextCircleIndex],
            this,
            this,
            LineSegment(
                tangents[nextCircleIndex].end,
                tangents[nextCircleIndex].begin
            ),
            direction
        )
    }

    override fun equals(other: Any?): Boolean {
        if (other == null || other !is Obstacle) return false
        return circles == other.circles
    }

    override fun hashCode(): Int {
        return circles.hashCode()
    }
}

internal fun Obstacle(vararg circles: Circle2D): Obstacle = Obstacle(listOf(*circles))

private fun LineSegment2D.intersectsSegment(other: LineSegment2D): Boolean {
    fun crossProduct(v1: DoubleVector2D, v2: DoubleVector2D): Double {
        return v1.x * v2.y - v1.y * v2.x
    }
    return if (crossProduct(other.begin - begin, other.end - begin).sign ==
        crossProduct(other.begin - end, other.end - end).sign
    ) {
        false
    } else {
        crossProduct(begin - other.begin, end - other.begin).sign != crossProduct(
            begin - other.end,
            end - other.end
        ).sign
    }
}

private fun LineSegment2D.intersectsCircle(circle: Circle2D): Boolean {
    val a = (begin.x - end.x).pow(2.0) + (begin.y - end.y).pow(2.0)
    val b = 2 * ((begin.x - end.x) * (end.x - circle.center.x) +
            (begin.y - end.y) * (end.y - circle.center.y))
    val c = (end.x - circle.center.x).pow(2.0) + (end.y - circle.center.y).pow(2.0) -
            circle.radius.pow(2.0)
    val d = b.pow(2.0) - 4 * a * c
    if (d < 1e-6) {
        return false
    } else {
        val t1 = (-b - d.pow(0.5)) * 0.5 / a
        val t2 = (-b + d.pow(0.5)) * 0.5 / a
        if (((0 < t1) and (t1 < 1)) or ((0 < t2) and (t2 < 1))) {
            return true
        }
    }
    return false
}

/**
 * Check if segment has any intersections with an obstacle
 */
private fun LineSegment2D.intersectsObstacle(obstacle: Obstacle): Boolean =
    obstacle.tangents.any { tangent -> intersectsSegment(tangent) }
            || obstacle.circles.any { circle -> intersectsCircle(circle) }


/**
 * All tangents between two obstacles
 *
 * In general generates 4 paths.
 *  TODO check intersections.
 */
private fun outerTangents(first: Obstacle, second: Obstacle): Map<DubinsPath.Type, Tangent> = buildMap {

    for (firstCircle in first.circles) {
        for (secondCircle in second.circles) {
            for ((pathType, segment) in tangentsBetweenCircles(firstCircle, secondCircle)) {
                val tangent = Tangent(
                    firstCircle,
                    secondCircle,
                    first,
                    second,
                    segment,
                    pathType.first,
                    pathType.third
                )

                if (!(tangent.intersectsObstacle(first)) && !(tangent.intersectsObstacle(second))) {
                    put(
                        pathType,
                        tangent
                    )
                }
            }
        }
    }
}

private fun arcLength(
    circle: Circle2D,
    point1: DoubleVector2D,
    point2: DoubleVector2D,
    direction: Trajectory2D.Direction,
): Double {
    val phi1 = atan2(point1.y - circle.center.y, point1.x - circle.center.x)
    val phi2 = atan2(point2.y - circle.center.y, point2.x - circle.center.x)
    var angle = 0.0
    when (direction) {
        Trajectory2D.L -> {
            angle = if (phi2 >= phi1) {
                phi2 - phi1
            } else {
                2 * PI + phi2 - phi1
            }
        }

        Trajectory2D.R -> {
            angle = if (phi2 >= phi1) {
                2 * PI - (phi2 - phi1)
            } else {
                -(phi2 - phi1)
            }
        }
    }
    return circle.radius * angle
}

private fun normalVectors(v: DoubleVector2D, r: Double): Pair<DoubleVector2D, DoubleVector2D> {
    return Pair(
        r * vector(v.y / norm(v), -v.x / norm(v)),
        r * vector(-v.y / norm(v), v.x / norm(v))
    )
}


private fun constructTangentCircles(
    point: DoubleVector2D,
    direction: DoubleVector2D,
    r: Double,
): LR<Circle2D> {
    val center1 = point + normalVectors(direction, r).first
    val center2 = point + normalVectors(direction, r).second
    val p1 = center1 - point
    return if (atan2(p1.y, p1.x) - atan2(direction.y, direction.x) in listOf(PI / 2, -3 * PI / 2)) {
        LR(
            Circle2D(center1, r),
            Circle2D(center2, r)
        )
    } else {
        LR(
            Circle2D(center2, r),
            Circle2D(center1, r)
        )
    }
}

private fun sortedObstacles(
    currentObstacle: Obstacle,
    obstacles: List<Obstacle>,
): List<Obstacle> {
    return obstacles.sortedBy { norm(it.center - currentObstacle.center) }
}

private fun tangentsAlongTheObstacle(
    initialCircle: Circle2D,
    direction: Trajectory2D.Direction,
    finalCircle: Circle2D,
    obstacle: Obstacle,
): List<Tangent> {
    val dubinsTangents = mutableListOf<Tangent>()
    var tangent = obstacle.nextTangent(initialCircle, direction)
    dubinsTangents.add(tangent)
    while (tangent.endCircle != finalCircle) {
        tangent = obstacle.nextTangent(tangent.endCircle, direction)
        dubinsTangents.add(tangent)
    }
    return dubinsTangents
}

/**
 * Check if all proposed paths have ended at [finalObstacle]
 */
private fun allFinished(
    paths: List<TangentPath>,
    finalObstacle: Obstacle,
): Boolean {
    for (path in paths) {
        if (path.last().endObstacle != finalObstacle) {
            return false
        }
    }
    return true
}

private fun LineSegment2D.toTrajectory() = StraightTrajectory2D(begin, end)


private fun TangentPath.toTrajectory(): CompositeTrajectory2D = CompositeTrajectory2D(
    buildList {
        tangents.zipWithNext().forEach { (left, right) ->
            add(left.lineSegment.toTrajectory())
            add(
                CircleTrajectory2D.of(
                    right.startCircle.center,
                    left.lineSegment.end,
                    right.lineSegment.begin,
                    right.startDirection
                )
            )
        }

        add(tangents.last().lineSegment.toTrajectory())
    }
)

internal fun findAllPaths(
    start: DubinsPose2D,
    startingRadius: Double,
    finish: DubinsPose2D,
    finalRadius: Double,
    obstacles: List<Obstacle>,
): List<CompositeTrajectory2D> {
    fun DubinsPose2D.direction() = vector(cos(bearing), sin(bearing))

    // two circles for the initial point
    val initialCircles = constructTangentCircles(
        start,
        start.direction(),
        startingRadius
    )

    //two circles for the final point
    val finalCircles = constructTangentCircles(
        finish,
        finish.direction(),
        finalRadius
    )

    //all valid trajectories
    val trajectories = mutableListOf<CompositeTrajectory2D>()

    for (i in listOf(Trajectory2D.L, Trajectory2D.R)) {
        for (j in listOf(Trajectory2D.L, Trajectory2D.R)) {
            //Using obstacle to minimize code bloat
            val finalObstacle = Obstacle(finalCircles[j])

            var currentPaths: List<TangentPath> = listOf(
                TangentPath(
                    //We need only the direction of the final segment from this
                    Tangent(
                        initialCircles[i],
                        initialCircles[i],
                        Obstacle(initialCircles[i]),
                        Obstacle(initialCircles[i]),
                        LineSegment(start, start),
                        i
                    )
                )
            )
            while (!allFinished(currentPaths, finalObstacle)) {
                // paths after next obstacle iteration
                val newPaths = mutableListOf<TangentPath>()
                // for each path propagate it one obstacle further
                for (tangentPath: TangentPath in currentPaths) {
                    val currentCircle = tangentPath.last().endCircle
                    val currentDirection: Trajectory2D.Direction = tangentPath.last().endDirection
                    val currentObstacle = tangentPath.last().endObstacle

                    // If path is finished, ignore it
                    // TODO avoid returning to ignored obstacle on the next cycle
                    if (currentObstacle == finalObstacle) {
                        newPaths.add(tangentPath)
                    } else {
                        val tangentToFinal: Tangent = outerTangents(currentObstacle, finalObstacle)[DubinsPath.Type(
                            currentDirection,
                            Trajectory2D.S,
                            j
                        )] ?: TODO("Intersecting obstacles are not supported")

                        // searching for the nearest obstacle that intersects with the direct path
                        val nextObstacle = sortedObstacles(currentObstacle, obstacles).find { obstacle ->
                            tangentToFinal.intersectsObstacle(obstacle)
                        } ?: finalObstacle

                        //TODO add break check for end of path

                        // All valid tangents from current obstacle to the next one
                        val nextTangents: Collection<Tangent> = outerTangents(
                            currentObstacle,
                            nextObstacle
                        ).filter { (key, tangent) ->
                            obstacles.none { obstacle -> tangent.intersectsObstacle(obstacle) } && // does not intersect other obstacles
                                    key.first == currentDirection && // initial direction is the same as end of previous segment direction
                                    (nextObstacle != finalObstacle || key.third == j) // if it is the last, it should be the same as the one we are searching for
                        }.values

                        for (tangent in nextTangents) {
                            val tangentsAlong = if (tangent.startCircle == tangentPath.last().endCircle) {
                                //if the previous segment last circle is the same as first circle of the next segment

                                //If obstacle consists of single circle, do not walk around
                                if (tangent.startObstacle.circles.size < 2){
                                    emptyList()
                                } else {
                                    val lengthMaxPossible = arcLength(
                                        tangent.startCircle,
                                        tangentPath.last().lineSegment.end,
                                        tangent.startObstacle.nextTangent(
                                            tangent.startCircle,
                                            currentDirection
                                        ).lineSegment.begin,
                                        currentDirection
                                    )

                                    val lengthCalculated = arcLength(
                                        tangent.startCircle,
                                        tangentPath.last().lineSegment.end,
                                        tangent.lineSegment.begin,
                                        currentDirection
                                    )
                                    // ensure that path does not go inside the obstacle
                                    if (lengthCalculated > lengthMaxPossible) {
                                        tangentsAlongTheObstacle(
                                            currentCircle,
                                            currentDirection,
                                            tangent.startCircle,
                                            currentObstacle
                                        )
                                    } else {
                                        emptyList()
                                    }
                                }
                            } else {
                                tangentsAlongTheObstacle(
                                    currentCircle,
                                    currentDirection,
                                    tangent.startCircle,
                                    currentObstacle
                                )
                            }
                            newPaths.add(TangentPath(tangentPath.tangents + tangentsAlong + tangent))
                        }
                    }
                }
                currentPaths = newPaths
            }

            trajectories += currentPaths.map { tangentPath ->
                val lastDirection: Trajectory2D.Direction = tangentPath.last().endDirection
                val end = finalCircles[j]
                TangentPath(
                    tangentPath.tangents +
                            Tangent(
                                end,
                                end,
                                Obstacle(end),
                                Obstacle(end),
                                LineSegment(finish, finish),
                                startDirection = lastDirection,
                                endDirection = j
                            )
                )
            }.map { it.toTrajectory() }
        }
    }
    return trajectories
}


public object Obstacles {
    public fun allPathsAvoiding(
        start: DubinsPose2D,
        finish: DubinsPose2D,
        trajectoryRadius: Double,
        obstaclePolygons: List<Polygon<Double>>,
    ): List<CompositeTrajectory2D> {
        val obstacles: List<Obstacle> = obstaclePolygons.map { polygon ->
            Obstacle(polygon.points.map { point -> Circle2D(point, trajectoryRadius) })
        }
        return findAllPaths(start, trajectoryRadius, finish, trajectoryRadius, obstacles)
    }
}





