package space.kscience.kmath.geometry

import space.kscience.kmath.misc.zipWithNextCircular

public fun Euclidean2DSpace.polygon(points: List<DoubleVector2D>): Polygon<Double> = object : Polygon<Double> {
    override val points: List<Vector2D<Double>> get() = points
}

public fun Euclidean2DSpace.intersects(polygon: Polygon<Double>, segment: LineSegment2D): Boolean =
    polygon.points.zipWithNextCircular { l, r -> segment(l, r) }.any { intersects(it, segment) }