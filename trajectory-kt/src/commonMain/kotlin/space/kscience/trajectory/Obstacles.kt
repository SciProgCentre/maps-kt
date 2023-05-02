package space.kscience.trajectory

import space.kscience.kmath.geometry.*
import kotlin.collections.component1
import kotlin.collections.component2


public class Obstacles(public val obstacles: List<Obstacle>) {

    private inner class ObstacleConnection(
        val obstacleIndex: Int,
        val nodeIndex: Int,
        val direction: Trajectory2D.Direction,
    ) {
        val obstacle: Obstacle get() = obstacles[obstacleIndex]
        val circle: Circle2D get() = obstacle.arcs[nodeIndex].circle
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
        buildMap {
            for (firstCircleIndex in first.arcs.indices) {
                val firstCircle = first.arcs[firstCircleIndex]
                for (secondCircleIndex in second.arcs.indices) {
                    val secondCircle = second.arcs[secondCircleIndex]
                    for ((pathType, segment) in tangentsBetweenArcs(
                        firstCircle,
                        secondCircle
                    )) {
                        if (!first.intersects(segment) && !second.intersects(segment)) {
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


    private fun tangentsFromArc(
        arc: CircleTrajectory2D,
        obstacleIndex: Int,
    ): Map<DubinsPath.Type, ObstacleTangent> = with(Euclidean2DSpace) {
        val obstacle = obstacles[obstacleIndex]
        buildMap {
            for (circleIndex in obstacle.arcs.indices) {
                val obstacleArc = obstacle.arcs[circleIndex]
                for ((pathType, segment) in tangentsBetweenArcs(
                    arc.copy(arcAngle = Angle.piTimes2), //extend arc to full circle
                    obstacleArc
                )) {
                    if (pathType.first == arc.direction && !intersects(obstacle.polygon, segment)) {
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

    private fun tangentToArc(
        obstacleIndex: Int,
        obstacleDirection: Trajectory2D.Direction,
        arc: CircleTrajectory2D
    ): ObstacleTangent? = with(Euclidean2DSpace) {
        val obstacle = obstacles[obstacleIndex]
        for (circleIndex in obstacle.arcs.indices) {
            val obstacleArc = obstacle.arcs[circleIndex]
            tangentsBetweenCircles(
                obstacleArc.circle,
                arc.circle
            )[DubinsPath.Type(obstacleDirection, Trajectory2D.S, arc.direction)]?.takeIf {
                obstacleArc.containsPoint(it.begin) && !obstacle.intersects(it)
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
        //arc between end of the tangent and end of previous arc (begin of the next one)
        circumvention[0] = CircleTrajectory2D(
            first.circle,
            tangent1.tangentTrajectory.endPose,
            first.endPose,
            tangent1.to.direction
        )
        circumvention[circumvention.lastIndex] = CircleTrajectory2D(
            last.circle,
            last.beginPose,
            tangent2.tangentTrajectory.beginPose,
            tangent2.from.direction
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

    private fun avoiding(
        dubinsPath: CompositeTrajectory2D,
    ): Collection<Trajectory2D> = with(Euclidean2DSpace) {
        //fast return if no obstacles intersect the direct path
        if (obstacles.none { it.intersects(dubinsPath) }) return listOf(dubinsPath)

        val beginArc = dubinsPath.segments.first() as CircleTrajectory2D
        val endArc = dubinsPath.segments.last() as CircleTrajectory2D

        /**
         * Continue current tangent to final point or to the next obstacle
         */
        /**
         * Continue current tangent to final point or to the next obstacle
         */
        fun TangentPath.nextSteps(): Collection<TangentPath> {
            val connection = tangents.last().to
            require(connection != null)

            //indices of obstacles that are not on previous path
            val remainingObstacleIndices = obstacles.indices - tangents.mapNotNull { it.to?.obstacleIndex }.toSet()

            //a tangent to end point, null if tangent could not be constructed
            val tangentToEnd: ObstacleTangent = tangentToArc(
                connection.obstacleIndex,
                connection.direction,
                endArc
            ) ?: return emptySet()

            // if no intersections, finish
            if (obstacles.indices.none { obstacles[it].intersects(tangentToEnd.tangentTrajectory) }) return setOf(
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
            compareByDescending<Int> { obstacles[it].intersects(dubinsPath) } //take intersecting obstacles
                .thenBy { beginArc.circle.center.distanceTo(obstacles[it].center) } //then nearest
        ).firstNotNullOfOrNull { obstacleIndex ->
            tangentsFromArc(beginArc, obstacleIndex).values
                .filter { it.isValid }.takeIf { it.isNotEmpty() }
        }?: return emptySet()

        var paths = tangentsToFirstObstacle.map { TangentPath(listOf(it)) }

        while (!paths.all { it.isFinished }) {
            paths = paths.flatMap { if(it.isFinished) listOf(it) else  it.nextSteps() }
        }
        return paths.map {
            CompositeTrajectory2D(
                //arc from starting point
                CircleTrajectory2D(
                    beginArc.circle,
                    beginArc.beginPose,
                    it.tangents.first().tangentTrajectory.beginPose,
                    beginArc.direction
                ),
                it.toTrajectory(),
                //arc to the end point
                CircleTrajectory2D(
                    endArc.circle,
                    it.tangents.last().tangentTrajectory.endPose,
                    endArc.endPose,
                    endArc.direction
                ),
            )
        }
    }

    public fun allTrajectories(
        start: Pose2D,
        finish: Pose2D,
        radius: Double,
    ): List<Trajectory2D> {

        val dubinsPaths: List<CompositeTrajectory2D> = DubinsPath.all(start, finish, radius)

        return dubinsPaths.flatMap {
            avoiding(it)
        }
    }


    public companion object {
//        private data class LR<T>(val l: T, val r: T) {
//            operator fun get(direction: Trajectory2D.Direction) = when (direction) {
//                Trajectory2D.L -> l
//                Trajectory2D.R -> r
//            }
//        }
//
//        private fun constructTangentCircles(
//            pose: Pose2D,
//            r: Double,
//        ): LR<Circle2D> = with(Euclidean2DSpace) {
//            val center1 = pose + vector(r*sin(pose.bearing + Angle.piDiv2), r*cos(pose.bearing + Angle.piDiv2))
//            val center2 = pose + vector(r*sin(pose.bearing - Angle.piDiv2), r*cos(pose.bearing - Angle.piDiv2))
//            LR(
//                Circle2D(center2, r),
//                Circle2D(center1, r)
//            )
//        }

        public fun avoidObstacles(
            start: Pose2D,
            finish: Pose2D,
            radius: Double,
            obstacleList: List<Obstacle>,
        ): List<Trajectory2D> {
            val obstacles = Obstacles(obstacleList)
            return obstacles.allTrajectories(start, finish, radius)
        }

        public fun avoidObstacles(
            start: Pose2D,
            finish: Pose2D,
            radius: Double,
            vararg obstacles: Obstacle,
        ): List<Trajectory2D> = avoidObstacles(start, finish, radius, obstacles.toList())

        public fun avoidPolygons(
            start: Pose2D,
            finish: Pose2D,
            radius: Double,
            vararg polygons: Polygon<Double>,
        ): List<Trajectory2D> {
            val obstacles: List<Obstacle> = polygons.map { polygon ->
                Obstacle(polygon.points, radius)
            }
            return avoidObstacles(start, finish, radius, obstacles)
        }


        public fun avoidPolygons(
            start: Pose2D,
            finish: Pose2D,
            radius: Double,
            polygons: Collection<Polygon<Double>>,
        ): List<Trajectory2D> {
            val obstacles: List<Obstacle> = polygons.map { polygon ->
                Obstacle(polygon.points, radius)
            }
            return avoidObstacles(start, finish, radius, obstacles)
        }
    }

}