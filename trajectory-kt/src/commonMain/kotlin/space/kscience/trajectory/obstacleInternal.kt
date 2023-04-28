package space.kscience.trajectory

import space.kscience.kmath.geometry.*
import space.kscience.kmath.geometry.Euclidean2DSpace.distanceTo
import space.kscience.kmath.geometry.Euclidean2DSpace.minus
import space.kscience.kmath.geometry.Euclidean2DSpace.plus
import space.kscience.kmath.geometry.Euclidean2DSpace.times
import space.kscience.kmath.misc.zipWithNextCircular
import space.kscience.kmath.operations.DoubleField.pow
import kotlin.math.*


internal data class ObstacleNode(
    val obstacle: Obstacle,
    val nodeIndex: Int,
    val direction: Trajectory2D.Direction,
) {
    val circle get() = obstacle.circles[nodeIndex]
}

internal data class ObstacleTangent(
    val lineSegment: LineSegment2D,
    val beginNode: ObstacleNode,
    val endNode: ObstacleNode,
) : LineSegment2D by lineSegment {
    val startCircle get() = beginNode.circle
    val startDirection get() = beginNode.direction
    val endCircle get() = endNode.circle
    val endDirection get() = endNode.direction
}


private class LR<T>(val l: T, val r: T) {
    operator fun get(direction: Trajectory2D.Direction) = when (direction) {
        Trajectory2D.L -> l
        Trajectory2D.R -> r
    }
}

private class TangentPath(val tangents: List<ObstacleTangent>) {
    fun last() = tangents.last()
}

private fun TangentPath(vararg tangents: ObstacleTangent) = TangentPath(listOf(*tangents))

/**
 * Create inner and outer tangents between two circles.
 * This method returns a map of segments using [DubinsPath] connection type notation.
 */
internal fun tangentsBetweenCircles(
    first: Circle2D,
    second: Circle2D,
): Map<DubinsPath.Type, LineSegment2D> = with(Euclidean2DSpace) {
    // Distance between centers
    val distanceBetweenCenters: Double = first.center.distanceTo(second.center)

    // return empty map if one circle is inside another
    val minRadius = min(first.radius, second.radius)
    val maxRadius = max(first.radius, second.radius)

    val listOfTangents = when {
        // one circle inside another, no tangents
        distanceBetweenCenters + minRadius <= maxRadius -> return emptyMap()
        // circles intersect, only outer tangents
        distanceBetweenCenters - minRadius <= maxRadius -> listOf(DubinsPath.Type.RSR, DubinsPath.Type.LSL)
        // no intersections all tangents
        else -> listOf(DubinsPath.Type.RSR, DubinsPath.Type.RSL, DubinsPath.Type.LSR, DubinsPath.Type.LSL)
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

        LineSegment(
            first.center + w * r1,
            second.center + w * r2
        )
    }
}

private fun Circle2D.isInside(other: Circle2D): Boolean {
    return center.distanceTo(other.center) + radius <= other.radius
}


internal class ObstacleShell(
    nodes: List<Circle2D>,
) : Obstacle {
    override val circles: List<Circle2D>
    override val center: Vector2D<Double>
    private val shell: List<LineSegment2D>
    private val shellDirection: Trajectory2D.Direction

    init {
        this.center = Euclidean2DSpace.vector(
            nodes.sumOf { it.center.x } / nodes.size,
            nodes.sumOf { it.center.y } / nodes.size
        )

//        this.circles = nodes.filter { node ->
//            //filter nodes inside other nodes
//            nodes.none{ node !== it  && node.isInside(it) }
//        }

        this.circles = nodes.distinct()

        if (nodes.size < 2) {
            shell = emptyList()
            shellDirection = Trajectory2D.R
        } else {

            //ignore cases when one circle is inside another one
            val lslTangents = circles.zipWithNextCircular { a, b ->
                tangentsBetweenCircles(a, b)[DubinsPath.Type.LSL] ?: error("Intersecting circles")
            }

            val rsrTangents = circles.zipWithNextCircular { a, b ->
                tangentsBetweenCircles(a, b)[DubinsPath.Type.RSR] ?: error("Intersecting circles")
            }


            val lslToCenter = lslTangents.sumOf { it.begin.distanceTo(center) } +
                    lslTangents.sumOf { it.end.distanceTo(center) }
            val rsrToCenter = rsrTangents.sumOf { it.begin.distanceTo(center) } +
                    rsrTangents.sumOf { it.end.distanceTo(center) }

            if (rsrToCenter >= lslToCenter) {
                this.shell = rsrTangents
                this.shellDirection = Trajectory2D.R
            } else {
                this.shell = lslTangents
                this.shellDirection = Trajectory2D.L
            }
        }
    }

    constructor(obstacle: Obstacle) : this(obstacle.circles)

    /**
     * Check if segment has any intersections with this obstacle
     */
    override fun intersects(segment: LineSegment2D): Boolean =
        shell.any { tangent -> segment.intersectsSegment(tangent) }
                || circles.any { circle -> segment.intersectsCircle(circle) }

    override fun intersects(circle: Circle2D): Boolean =
        shell.any{tangent -> tangent.intersectsCircle(circle) }

    /**
     * Tangent to next obstacle node in given direction
     */
    fun nextTangent(circleIndex: Int, direction: Trajectory2D.Direction): ObstacleTangent {
        if (circleIndex == -1) error("Circle does not belong to this tangent")

        val nextCircleIndex = if (direction == this.shellDirection) {
            if (circleIndex == circles.lastIndex) 0 else circleIndex + 1
        } else {
            if (circleIndex == 0) circles.lastIndex else circleIndex - 1
        }

        return ObstacleTangent(
            LineSegment(
                shell[nextCircleIndex].end,
                shell[nextCircleIndex].begin
            ),
            ObstacleNode(this, circleIndex, direction),
            ObstacleNode(this, nextCircleIndex, direction),
        )
    }

    /**
     * All tangents in given direction
     */
    internal fun tangentsAlong(
        initialCircleIndex: Int,
        finalCircleIndex: Int,
        direction: Trajectory2D.Direction,
    ): List<ObstacleTangent> {
        return buildList {
            var currentIndex = initialCircleIndex
            do {
                val tangent = nextTangent(currentIndex, direction)
                add(tangent)
                currentIndex = tangent.endNode.nodeIndex
            } while (currentIndex != finalCircleIndex)
        }
    }

    override fun equals(other: Any?): Boolean {
        if (other == null || other !is ObstacleShell) return false
        return circles == other.circles
    }

    override fun hashCode(): Int {
        return circles.hashCode()
    }
}

internal fun ObstacleShell(vararg circles: Circle2D): ObstacleShell = ObstacleShell(listOf(*circles))


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

    val aNormalized = a / (a * a + b * b + c * c)
    val bNormalized = b / (a * a + b * b + c * c)
    val cNormalized = c / (a * a + b * b + c * c)

    val d = bNormalized.pow(2.0) - 4 * aNormalized * cNormalized
    if (d < 1e-6) {
        return false
    } else {
        val t1 = (-bNormalized - d.pow(0.5)) * 0.5 / aNormalized
        val t2 = (-bNormalized + d.pow(0.5)) * 0.5 / aNormalized
        if (((0 < t1) and (t1 < 1)) or ((0 < t2) and (t2 < 1))) {
            return true
        }
    }
    return false
}


/**
 * All tangents between two obstacles
 *
 * In general generates 4 paths.
 *  TODO check intersections.
 */
private fun outerTangents(first: Obstacle, second: Obstacle): Map<DubinsPath.Type, ObstacleTangent> = buildMap {

    for (firstCircleIndex in first.circles.indices) {
        for (secondCircleIndex in second.circles.indices) {
            for ((pathType, segment) in tangentsBetweenCircles(
                first.circles[firstCircleIndex],
                second.circles[secondCircleIndex]
            )) {
                val tangent = ObstacleTangent(
                    segment,
                    ObstacleNode(first, firstCircleIndex, pathType.first),
                    ObstacleNode(second, secondCircleIndex, pathType.third)
                )

                if (!(first.intersects(tangent)) && !(second.intersects(tangent))) {
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
        r * Euclidean2DSpace.vector(v.y / Euclidean2DSpace.norm(v), -v.x / Euclidean2DSpace.norm(v)),
        r * Euclidean2DSpace.vector(-v.y / Euclidean2DSpace.norm(v), v.x / Euclidean2DSpace.norm(v))
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
    return obstacles.sortedBy { Euclidean2DSpace.norm(it.center - currentObstacle.center) }
}

/**
 * Check if all proposed paths have ended at [finalObstacle]
 */
private fun allFinished(
    paths: List<TangentPath>,
    finalObstacle: Obstacle,
): Boolean {
    for (path in paths) {
        if (path.last().endNode.obstacle !== finalObstacle) {
            return false
        }
    }
    return true
}

private fun LineSegment2D.toTrajectory() = StraightTrajectory2D(begin, end)


private fun TangentPath.toTrajectory(): CompositeTrajectory2D = CompositeTrajectory2D(
    buildList {
        tangents.zipWithNext().forEach { (left, right: ObstacleTangent) ->
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
    obstacles: List<ObstacleShell>,
): List<CompositeTrajectory2D> {
    fun DubinsPose2D.direction() = Euclidean2DSpace.vector(cos(bearing), sin(bearing))

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
            val initialObstacle = ObstacleShell(initialCircles[i])
            val finalObstacle = ObstacleShell(finalCircles[j])

            var currentPaths: List<TangentPath> = listOf(
                TangentPath(
                    //We need only the direction of the final segment from this
                    ObstacleTangent(
                        LineSegment(start, start),
                        ObstacleNode(initialObstacle, 0, i),
                        ObstacleNode(initialObstacle, 0, i),
                    )
                )
            )
            while (!allFinished(currentPaths, finalObstacle)) {
                // paths after next obstacle iteration
                val newPaths = mutableListOf<TangentPath>()
                // for each path propagate it one obstacle further
                for (tangentPath: TangentPath in currentPaths) {
                    val currentNode = tangentPath.last().endNode
                    val currentDirection: Trajectory2D.Direction = tangentPath.last().endDirection
                    val currentObstacle: ObstacleShell = ObstacleShell(tangentPath.last().endNode.obstacle)

                    // If path is finished, ignore it
                    // TODO avoid returning to ignored obstacle on the next cycle
                    if (currentObstacle == finalObstacle) {
                        newPaths.add(tangentPath)
                    } else {
                        val tangentToFinal: ObstacleTangent =
                            outerTangents(currentObstacle, finalObstacle)[DubinsPath.Type(
                                currentDirection,
                                Trajectory2D.S,
                                j
                            )] ?: break

                        // searching for the nearest obstacle that intersects with the direct path
                        val nextObstacle = sortedObstacles(currentObstacle, obstacles).find { obstacle ->
                            obstacle.intersects(tangentToFinal)
                        } ?: finalObstacle

                        //TODO add break check for end of path

                        // All valid tangents from current obstacle to the next one
                        val nextTangents: Collection<ObstacleTangent> = outerTangents(
                            currentObstacle,
                            nextObstacle
                        ).filter { (key, tangent) ->
                            obstacles.none { obstacle -> obstacle.intersects(tangent) } && // does not intersect other obstacles
                                    key.first == currentDirection && // initial direction is the same as end of previous segment direction
                                    (nextObstacle != finalObstacle || key.third == j) // if it is the last, it should be the same as the one we are searching for
                        }.values

                        for (tangent in nextTangents) {
                            val tangentsAlong = if (tangent.startCircle === tangentPath.last().endCircle) {
                                //if the previous segment last circle is the same as first circle of the next segment

                                //If obstacle consists of single circle, do not walk around
                                if (currentObstacle.circles.size < 2) {
                                    emptyList()
                                } else {
                                    val lengthMaxPossible = arcLength(
                                        tangent.startCircle,
                                        tangentPath.last().lineSegment.end,
                                        currentObstacle.nextTangent(
                                            tangent.beginNode.nodeIndex,
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
                                        currentObstacle.tangentsAlong(
                                            currentNode.nodeIndex,
                                            tangent.beginNode.nodeIndex,
                                            currentDirection,
                                        )
                                    } else {
                                        emptyList()
                                    }
                                }
                            } else {
                                currentObstacle.tangentsAlong(
                                    currentNode.nodeIndex,
                                    tangent.beginNode.nodeIndex,
                                    currentDirection,
                                )
                            }
                            newPaths.add(TangentPath(tangentPath.tangents + tangentsAlong + tangent))
                        }
                    }
                }
                currentPaths = newPaths
            }

            trajectories += currentPaths.map { tangentPath ->
//                val lastDirection: Trajectory2D.Direction = tangentPath.last().endDirection
                val end = Obstacle(finalCircles[j])
                TangentPath(
                    tangentPath.tangents +
                            ObstacleTangent(
                                LineSegment(finish, finish),
                                ObstacleNode(end, 0, j),
                                ObstacleNode(end, 0, j)
                            )
                )
            }.map { it.toTrajectory() }
        }
    }
    return trajectories
}
