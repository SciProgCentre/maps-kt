package center.sciprog.maps.features

import androidx.compose.runtime.Composable
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.DrawStyle
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.VectorPainter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlin.math.floor


/**
 * @param T type of coordinates used for the view point
 */
public interface ViewPoint<T: Any> {
    public val focus: T
    public val zoom: Double
}

public interface Rectangle<T: Any>{
    public val topLeft: T
    public val bottomRight: T

    public operator fun contains(point: T): Boolean
}

public interface Feature<T: Any> {
    public interface Attribute<T>

    public val zoomRange: ClosedFloatingPointRange<Double>

    public var attributes: AttributeMap

    public fun getBoundingBox(zoom: Double): Rectangle<T>?
}

public interface SelectableFeature<T: Any> : Feature<T> {
    public operator fun contains(point: ViewPoint<T>): Boolean = getBoundingBox(point.zoom)?.let {
        point.focus in it
    } ?: false
}

public interface DraggableFeature<T: Any> : SelectableFeature<T> {
    public fun withCoordinates(newCoordinates: T): Feature<T>
}

public fun <T: Any> Iterable<Feature<T>>.computeBoundingBox(zoom: Double): Rectangle<T>? =
    mapNotNull { it.getBoundingBox(zoom) }.wrapAll()

//public fun Pair<Number, Number>.toCoordinates(): GeodeticMapCoordinates =
//    GeodeticMapCoordinates.ofDegrees(first.toDouble(), second.toDouble())

internal val defaultZoomRange = 1.0..Double.POSITIVE_INFINITY

/**
 * A feature that decides what to show depending on the zoom value (it could change size of shape)
 */
public class FeatureSelector<T: Any>(
    override var attributes: AttributeMap = AttributeMap(),
    public val selector: (zoom: Int) -> Feature<T>,
) : Feature<T> {
    override val zoomRange: ClosedFloatingPointRange<Double> get() = defaultZoomRange

    override fun getBoundingBox(zoom: Double): Rectangle<T>? = selector(floor(zoom).toInt()).getBoundingBox(zoom)
}

public class DrawFeature<T: Any>(
    public val position: T,
    override val zoomRange: ClosedFloatingPointRange<Double> = defaultZoomRange,
    override var attributes: AttributeMap = AttributeMap(),
    public val drawFeature: DrawScope.() -> Unit,
) : DraggableFeature<T> {
    override fun getBoundingBox(zoom: Double): Rectangle<T> {
        //TODO add box computation
        return GmcRectangle(position, position)
    }

    override fun withCoordinates(newCoordinates: T): Feature<T> =
        DrawFeature(newCoordinates, zoomRange, attributes, drawFeature)
}

public class PathFeature<T: Any>(
    public val rectangle: Rectangle<T>,
    public val path: Path,
    public val brush: Brush,
    public val style: DrawStyle = Fill,
    public val targetRect: Rect = path.getBounds(),
    override val zoomRange: ClosedFloatingPointRange<Double> = defaultZoomRange,
    override var attributes: AttributeMap = AttributeMap(),
) : DraggableFeature<T> {
    override fun withCoordinates(newCoordinates: T): Feature<T> =
        PathFeature(rectangle.moveTo(newCoordinates), path, brush, style, targetRect, zoomRange)

    override fun getBoundingBox(zoom: Double): Rectangle<T> = rectangle

}

public class PointsFeature<T: Any>(
    public val points: List<T>,
    override val zoomRange: ClosedFloatingPointRange<Double> = defaultZoomRange,
    public val stroke: Float = 2f,
    public val color: Color = Color.Red,
    public val pointMode: PointMode = PointMode.Points,
    override var attributes: AttributeMap = AttributeMap(),
) : Feature<T> {
    override fun getBoundingBox(zoom: Double): Rectangle<T> = GmcRectangle(points.first(), points.last())
}

public data class CircleFeature<T: Any>(
    public val center: T,
    override val zoomRange: ClosedFloatingPointRange<Double> = defaultZoomRange,
    public val size: Float = 5f,
    public val color: Color = Color.Red,
    override var attributes: AttributeMap = AttributeMap(),
) : DraggableFeature<T> {
    override fun getBoundingBox(zoom: Double): Rectangle<T> {
        val scale = WebMercatorProjection.scaleFactor(zoom)
        return GmcRectangle.square(center, (size / scale).radians, (size / scale).radians)
    }

    override fun withCoordinates(newCoordinates: T): Feature<T> =
        CircleFeature(newCoordinates, zoomRange, size, color, attributes)
}

public class RectangleFeature<T: Any>(
    public val center: T,
    override val zoomRange: ClosedFloatingPointRange<Double> = defaultZoomRange,
    public val size: DpSize = DpSize(5.dp, 5.dp),
    public val color: Color = Color.Red,
    override var attributes: AttributeMap = AttributeMap(),
) : DraggableFeature<T> {
    override fun getBoundingBox(zoom: Double):  Rectangle<T> {
        val scale = WebMercatorProjection.scaleFactor(zoom)
        return GmcRectangle.square(center, (size.height.value / scale).radians, (size.width.value / scale).radians)
    }

    override fun withCoordinates(newCoordinates: T): Feature<T> =
        RectangleFeature(newCoordinates, zoomRange, size, color, attributes)
}

public class LineFeature<T: Any>(
    public val a: T,
    public val b: T,
    override val zoomRange: ClosedFloatingPointRange<Double> = defaultZoomRange,
    public val color: Color = Color.Red,
    override var attributes: AttributeMap = AttributeMap(),
) : SelectableFeature<T> {
    override fun getBoundingBox(zoom: Double):  Rectangle<T> = GmcRectangle(a, b)

    override fun contains(point: ViewPoint<T>): Boolean {
        return super.contains(point)
    }
}

/**
 * @param startAngle the angle from parallel downwards for the start of the arc
 * @param arcLength arc length
 */
public class ArcFeature<T:Any>(
    public val oval: Rectangle<T>,
    public val startAngle: Angle,
    public val arcLength: Angle,
    override val zoomRange: ClosedFloatingPointRange<Double> = defaultZoomRange,
    public val color: Color = Color.Red,
    override var attributes: AttributeMap = AttributeMap(),
) : DraggableFeature<T> {
    override fun getBoundingBox(zoom: Double): Rectangle<T> = oval

    override fun withCoordinates(newCoordinates: T): Feature<T> =
        ArcFeature(oval.moveTo(newCoordinates), startAngle, arcLength, zoomRange, color, attributes)
}

public class BitmapImageFeature<T: Any>(
    public val position: T,
    public val image: ImageBitmap,
    public val size: IntSize = IntSize(15, 15),
    override val zoomRange: ClosedFloatingPointRange<Double> = defaultZoomRange,
    override var attributes: AttributeMap = AttributeMap(),
) : DraggableFeature<T> {
    override fun getBoundingBox(zoom: Double): Rectangle<T> = GmcRectangle(position, position)

    override fun withCoordinates(newCoordinates: T): Feature<T> =
        BitmapImageFeature(newCoordinates, image, size, zoomRange, attributes)
}

public class VectorImageFeature<T: Any>(
    public val position: T,
    public val image: ImageVector,
    public val size: DpSize = DpSize(20.dp, 20.dp),
    override val zoomRange: ClosedFloatingPointRange<Double> = defaultZoomRange,
    override var attributes: AttributeMap = AttributeMap(),
) : DraggableFeature<T> {
    override fun getBoundingBox(zoom: Double):  Rectangle<T> = GmcRectangle(position, position)

    override fun withCoordinates(newCoordinates: T): Feature<T> =
        VectorImageFeature(newCoordinates, image, size, zoomRange, attributes)

    @Composable
    public fun painter(): VectorPainter = rememberVectorPainter(image)
}

/**
 * A group of other features
 */
public class FeatureGroup<T: Any>(
    public val children: Map<FeatureId<*>, Feature<T>>,
    override val zoomRange: ClosedFloatingPointRange<Double> = defaultZoomRange,
    override var attributes: AttributeMap = AttributeMap(),
) : Feature<T> {
    override fun getBoundingBox(zoom: Double): Rectangle<T>? =
        children.values.mapNotNull { it.getBoundingBox(zoom) }.wrapAll()
}

public class TextFeature<T: Any>(
    public val position: T,
    public val text: String,
    override val zoomRange: ClosedFloatingPointRange<Double> = defaultZoomRange,
    public val color: Color = Color.Black,
    override var attributes: AttributeMap = AttributeMap(),
    public val fontConfig: MapTextFeatureFont.() -> Unit,
) : DraggableFeature<T> {
    override fun getBoundingBox(zoom: Double): Rectangle<T> = GmcRectangle(position, position)

    override fun withCoordinates(newCoordinates: T): Feature<T> =
        TextFeature(newCoordinates, text, zoomRange, color, attributes, fontConfig)
}
