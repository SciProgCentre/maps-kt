package space.kscience.trajectory

import space.kscience.kmath.geometry.*
import space.kscience.kmath.geometry.Euclidean2DSpace.distanceTo
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.math.PI
import kotlin.math.atan2


public class Obstacles(public val obstacles: List<Obstacle>) {

    private inner class ObstacleConnection(
        val obstacleIndex: Int,
        val nodeIndex: Int,
        val direction: Trajectory2D.Direction,
    ) {
        val obstacle: Obstacle get() = obstacles[obstacleIndex]
        val circle: Circle2D get() = obstacle.circles[nodeIndex]
    }

    private inner class ObstacleTangent(
        val tangentTrajectory: StraightTrajectory2D,
        val from: ObstacleConnection?,
        val to: ObstacleConnection?,
    ) {
        /**
         * If false this tangent intersects another obstacle
         */
        val isValid by lazy {
            obstacles.indices.none {
                it != from?.obstacleIndex && it != to?.obstacleIndex && obstacles[it].intersects(tangentTrajectory)
            }
        }
    }


    /**
     * All tangents between two obstacles
     *
     * In general generates 4 paths.
     *  TODO check intersections.
     */
    private fun tangentsBetween(
        firstIndex: Int,
        secondIndex: Int,
    ): Map<DubinsPath.Type, ObstacleTangent> = with(Euclidean2DSpace) {
        val first = obstacles[firstIndex]
        val second = obstacles[secondIndex]
        val firstPolygon = polygon(first.circles.map { it.center })
        val secondPolygon = polygon(second.circles.map { it.center })
        buildMap {
            for (firstCircleIndex in first.circles.indices) {
                val firstCircle = first.circles[firstCircleIndex]
                for (secondCircleIndex in second.circles.indices) {
                    val secondCircle = second.circles[secondCircleIndex]
                    for ((pathType, segment) in tangentsBetweenCircles(
                        firstCircle,
                        secondCircle
                    )) {
                        if (!intersects(firstPolygon, segment) && !intersects(secondPolygon, segment)) {
                            put(
                                pathType,
                                ObstacleTangent(
                                    segment,
                                    ObstacleConnection(firstIndex, firstCircleIndex, pathType.first),
                                    ObstacleConnection(secondIndex, secondCircleIndex, pathType.third)
                                )
                            )
                        }
                    }
                }
            }
        }
    }


    private fun tangentsFromCircle(
        circle: Circle2D,
        direction: Trajectory2D.Direction,
        obstacleIndex: Int,
    ): Map<DubinsPath.Type, ObstacleTangent> = with(Euclidean2DSpace) {
        val obstacle = obstacles[obstacleIndex]
        val polygon = polygon(obstacle.circles.map { it.center })
        buildMap {
            for (circleIndex in obstacle.circles.indices) {
                val obstacleCircle = obstacle.circles[circleIndex]
                for ((pathType, segment) in tangentsBetweenCircles(
                    circle,
                    obstacleCircle
                )) {
                    if (pathType.first == direction && !intersects(polygon, segment)) {
                        put(
                            pathType,
                            ObstacleTangent(
                                segment,
                                null,
                                ObstacleConnection(obstacleIndex, circleIndex, pathType.third)
                            )
                        )
                    }
                }
            }
        }
    }

    private fun tangentToCircle(
        obstacleIndex: Int,
        obstacleDirection: Trajectory2D.Direction,
        circle: Circle2D,
        direction: Trajectory2D.Direction,
    ): ObstacleTangent? = with(Euclidean2DSpace) {
        val obstacle = obstacles[obstacleIndex]
        val polygon = polygon(obstacle.circles.map { it.center })
        for (circleIndex in obstacle.circles.indices) {
            val obstacleCircle = obstacle.circles[circleIndex]
            tangentsBetweenCircles(
                obstacleCircle,
                circle
            ).get(DubinsPath.Type(obstacleDirection, Trajectory2D.S, direction))?.takeIf {
                !intersects(polygon, it)
            }?.let {
                return ObstacleTangent(
                    it,
                    ObstacleConnection(obstacleIndex, circleIndex, obstacleDirection),
                    null,
                )
            }
        }
        return null
    }


    private val tangentsCache = hashMapOf<Pair<Int, Int>, Map<DubinsPath.Type, ObstacleTangent>>()

    private fun getAllTangents(i: Int, j: Int): Map<DubinsPath.Type, ObstacleTangent> =
        tangentsCache.getOrPut(i to j) {
            tangentsBetween(i, j)
        }


    /**
     * Circumvention trajectory alongside obstacle. Replacing first and last arcs with appropriate cuts
     */
    private fun trajectoryBetween(tangent1: ObstacleTangent, tangent2: ObstacleTangent): CompositeTrajectory2D {
        require(tangent1.to != null)
        require(tangent2.from != null)

        require(tangent1.to.obstacleIndex == tangent2.from.obstacleIndex)
        require(tangent1.to.direction == tangent2.from.direction)

        val circumvention = tangent1.to.obstacle.circumvention(
            tangent1.to.direction,
            tangent1.to.nodeIndex,
            tangent2.from.nodeIndex
        ).segments.toMutableList()

        //cutting first and last arcs to accommodate connection points
        val first = circumvention.first() as CircleTrajectory2D
        val last = circumvention.last() as CircleTrajectory2D
        circumvention[0] = CircleTrajectory2D(
            first.circle,
            tangent1.tangentTrajectory.endPose,
            first.end
        )
        circumvention[circumvention.lastIndex] = CircleTrajectory2D(
            last.circle,
            last.begin,
            tangent2.tangentTrajectory.beginPose
        )
        return CompositeTrajectory2D(circumvention)
    }

    private inner class TangentPath(val tangents: List<ObstacleTangent>) {
        val isFinished get() = tangents.last().to == null

        fun toTrajectory(): CompositeTrajectory2D = CompositeTrajectory2D(
            buildList<Trajectory2D> {
                add(tangents.first().tangentTrajectory)
                tangents.zipWithNext().forEach { (l, r) ->
                    addAll(trajectoryBetween(l, r).segments)
                    add(r.tangentTrajectory)
                }
            }
        )
    }

    public fun allTrajectoriesAvoiding(
        startCircle: Circle2D,
        startDirection: Trajectory2D.Direction,
        endCircle: Circle2D,
        endDirection: Trajectory2D.Direction,
    ): Collection<Trajectory2D> {
        val directTangent: StraightTrajectory2D = tangentsBetweenCircles(startCircle, endCircle).get(
            DubinsPath.Type(startDirection, Trajectory2D.S, endDirection)
        ) ?: return emptySet()

        //fast return if no obstacles intersect direct path
        if (obstacles.none { it.intersects(directTangent) }) return listOf(directTangent)

        /**
         * Continue current tangent to final point or to the next obstacle
         */
        fun TangentPath.nextSteps(): Collection<TangentPath> {
            val connection = tangents.last().to
            require(connection != null)

            //indices of obstacles that are not on previous path
            val remainingObstacleIndices = obstacles.indices - tangents.mapNotNull { it.to?.obstacleIndex }

            //a tangent to end point, null if tangent could not be constructed
            val tangentToEnd: ObstacleTangent = tangentToCircle(
                connection.obstacleIndex,
                connection.direction,
                endCircle,
                endDirection
            ) ?: return emptySet()

            if (remainingObstacleIndices.none { obstacles[it].intersects(tangentToEnd.tangentTrajectory) }) return setOf(
                TangentPath(tangents + tangentToEnd)
            )

            // tangents to other obstacles
            return remainingObstacleIndices.sortedWith(
                compareByDescending<Int> { obstacles[it].intersects(tangentToEnd.tangentTrajectory) } //take intersecting obstacles
                    .thenBy { connection.circle.center.distanceTo(obstacles[it].center) } //then nearest
            ).firstNotNullOf { nextObstacleIndex ->
                //all tangents from cache
                getAllTangents(connection.obstacleIndex, nextObstacleIndex).filter {
                    //filtered by direction
                    it.key.first == connection.direction
                }.values.takeIf { it.isNotEmpty() } // skip if empty
            }.map {
                TangentPath(tangents + it)
            }
        }


        //find nearest obstacle that has valid tangents to
        val tangentsToFirstObstacle: Collection<ObstacleTangent> = obstacles.indices.sortedWith(
            compareByDescending<Int> { obstacles[it].intersects(directTangent) } //take intersecting obstacles
                .thenBy { startCircle.center.distanceTo(obstacles[it].center) } //then nearest
        ).firstNotNullOf { obstacleIndex ->
            tangentsFromCircle(startCircle, startDirection, obstacleIndex).values
                .filter { it.isValid }.takeIf { it.isNotEmpty() }
        }

        var paths = tangentsToFirstObstacle.map { TangentPath(listOf(it)) }

        while (!paths.all { it.isFinished }) {
            paths = paths.flatMap { it.nextSteps() }
        }
        return paths.map { it.toTrajectory() }
    }


    public companion object {
        private data class LR<T>(val l: T, val r: T) {
            operator fun get(direction: Trajectory2D.Direction) = when (direction) {
                Trajectory2D.L -> l
                Trajectory2D.R -> r
            }
        }

        private fun normalVectors(v: DoubleVector2D, r: Double): Pair<DoubleVector2D, DoubleVector2D> =
            with(Euclidean2DSpace) {
                Pair(
                    r * vector(v.y / norm(v), -v.x / norm(v)),
                    r * vector(-v.y / norm(v), v.x / norm(v))
                )
            }

        private fun constructTangentCircles(
            pose: DubinsPose2D,
            r: Double,
        ): LR<Circle2D> = with(Euclidean2DSpace) {
            val direction = DubinsPose2D.bearingToVector(pose.bearing)
            //TODO optimize to use bearing
            val center1 = pose + normalVectors(direction, r).first
            val center2 = pose + normalVectors(direction, r).second
            val p1 = center1 - pose
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

        public fun avoidObstacles(
            start: DubinsPose2D,
            finish: DubinsPose2D,
            startingRadius: Double,
            obstacleList: List<Obstacle>,
            finalRadius: Double = startingRadius,
        ): List<Trajectory2D> {
            val obstacles = Obstacles(obstacleList)
            val initialCircles = constructTangentCircles(
                start,
                startingRadius
            )

            //two circles for the final point
            val finalCircles = constructTangentCircles(
                finish,
                finalRadius
            )
            val lr = listOf(Trajectory2D.L, Trajectory2D.R)
            return buildList {
                lr.forEach { beginDirection ->
                    lr.forEach { endDirection ->
                        addAll(
                            obstacles.allTrajectoriesAvoiding(
                                initialCircles[beginDirection],
                                beginDirection,
                                finalCircles[endDirection],
                                endDirection
                            )
                        )
                    }
                }
            }
        }

        public fun avoidObstacles(
            start: DubinsPose2D,
            finish: DubinsPose2D,
            trajectoryRadius: Double,
            vararg obstacles: Obstacle,
        ): List<Trajectory2D> = avoidObstacles(start, finish, trajectoryRadius, obstacles.toList())

        public fun avoidPolygons(
            start: DubinsPose2D,
            finish: DubinsPose2D,
            trajectoryRadius: Double,
            vararg polygons: Polygon<Double>,
        ): List<Trajectory2D> {
            val obstacles: List<Obstacle> = polygons.map { polygon ->
                Obstacle(polygon.points, trajectoryRadius)
            }
            return avoidObstacles(start, finish, trajectoryRadius, obstacles)
        }


        public fun avoidPolygons(
            start: DubinsPose2D,
            finish: DubinsPose2D,
            trajectoryRadius: Double,
            polygons: Collection<Polygon<Double>>,
        ): List<Trajectory2D> {
            val obstacles: List<Obstacle> = polygons.map { polygon ->
                Obstacle(polygon.points, trajectoryRadius)
            }
            return avoidObstacles(start, finish, trajectoryRadius, obstacles)
        }
    }

}