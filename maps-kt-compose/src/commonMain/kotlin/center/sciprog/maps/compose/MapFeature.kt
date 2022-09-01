package center.sciprog.maps.compose

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.PointMode
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import center.sciprog.maps.coordinates.*
import kotlin.math.floor

public interface MapFeature {
    public val zoomRange: IntRange
    public fun getBoundingBox(zoom: Double): GmcRectangle?
    public val layer: Int
}

public interface DraggableMapFeature : MapFeature {
    public fun withCoordinates(newCoordinates: GeodeticMapCoordinates): MapFeature
}

public fun Iterable<MapFeature>.computeBoundingBox(zoom: Double): GmcRectangle? =
    mapNotNull { it.getBoundingBox(zoom) }.wrapAll()

internal fun Pair<Double, Double>.toCoordinates() = GeodeticMapCoordinates.ofDegrees(first, second)

internal val defaultZoomRange = 1..18

/**
 * A feature that decides what to show depending on the zoom value (it could change size of shape)
 */
public class MapFeatureSelector(
    public val selector: (zoom: Int) -> MapFeature,
    override val layer: Int
) : MapFeature {
    override val zoomRange: IntRange get() = defaultZoomRange

    override fun getBoundingBox(zoom: Double): GmcRectangle? = selector(floor(zoom).toInt()).getBoundingBox(zoom)
}

public class MapDrawFeature(
    public val position: GeodeticMapCoordinates,
    override val zoomRange: IntRange = defaultZoomRange,
    public val drawFeature: DrawScope.() -> Unit,
    override val layer: Int
) : DraggableMapFeature {
    override fun getBoundingBox(zoom: Double): GmcRectangle {
        //TODO add box computation
        return GmcRectangle(position, position)
    }

    override fun withCoordinates(newCoordinates: GeodeticMapCoordinates): MapFeature =
        MapDrawFeature(newCoordinates, zoomRange, drawFeature, layer)
}

public class MapPointsFeature(
    public val points: List<GeodeticMapCoordinates>,
    override val zoomRange: IntRange = defaultZoomRange,
    public val stroke: Float = 2f,
    public val color: Color = Color.Red,
    public val pointMode: PointMode = PointMode.Points,
    override val layer: Int
) : MapFeature {
    override fun getBoundingBox(zoom: Double): GmcRectangle {
        return GmcRectangle(points.first(), points.last())
    }
}

public class MapCircleFeature(
    public val center: GeodeticMapCoordinates,
    override val zoomRange: IntRange = defaultZoomRange,
    public val size: Float = 5f,
    public val color: Color = Color.Red,
    override val layer: Int
) : DraggableMapFeature {
    override fun getBoundingBox(zoom: Double): GmcRectangle {
        val scale = WebMercatorProjection.scaleFactor(zoom)
        return GmcRectangle.square(center, (size / scale).radians, (size / scale).radians)
    }

    override fun withCoordinates(newCoordinates: GeodeticMapCoordinates): MapFeature =
        MapCircleFeature(newCoordinates, zoomRange, size, color, layer)
}

public class MapRectangleFeature(
    public val center: GeodeticMapCoordinates,
    override val zoomRange: IntRange = defaultZoomRange,
    public val size: DpSize = DpSize(5.dp, 5.dp),
    public val color: Color = Color.Red,
    override val layer: Int
) : DraggableMapFeature {
    override fun getBoundingBox(zoom: Double): GmcRectangle {
        val scale = WebMercatorProjection.scaleFactor(zoom)
        return GmcRectangle.square(center, (size.height.value / scale).radians, (size.width.value / scale).radians)
    }

    override fun withCoordinates(newCoordinates: GeodeticMapCoordinates): MapFeature =
        MapRectangleFeature(newCoordinates, zoomRange, size, color, layer)
}

public class MapLineFeature(
    public val a: GeodeticMapCoordinates,
    public val b: GeodeticMapCoordinates,
    override val zoomRange: IntRange = defaultZoomRange,
    public val color: Color = Color.Red,
    override val layer: Int
) : MapFeature {
    override fun getBoundingBox(zoom: Double): GmcRectangle = GmcRectangle(a, b)
}

public class MapArcFeature(
    public val oval: GmcRectangle,
    public val startAngle: Float,
    public val endAngle: Float,
    override val zoomRange: IntRange = defaultZoomRange,
    public val color: Color = Color.Red,
    override val layer: Int
) : MapFeature {
    override fun getBoundingBox(zoom: Double): GmcRectangle = oval
}

public class MapBitmapImageFeature(
    public val position: GeodeticMapCoordinates,
    public val image: ImageBitmap,
    public val size: IntSize = IntSize(15, 15),
    override val zoomRange: IntRange = defaultZoomRange,
    override val layer: Int
) : DraggableMapFeature {
    override fun getBoundingBox(zoom: Double): GmcRectangle = GmcRectangle(position, position)

    override fun withCoordinates(newCoordinates: GeodeticMapCoordinates): MapFeature =
        MapBitmapImageFeature(newCoordinates, image, size, zoomRange, layer)
}

public class MapVectorImageFeature(
    public val position: GeodeticMapCoordinates,
    public val painter: Painter,
    public val size: DpSize,
    override val zoomRange: IntRange = defaultZoomRange,
    override val layer: Int
) : DraggableMapFeature {
    override fun getBoundingBox(zoom: Double): GmcRectangle = GmcRectangle(position, position)

    override fun withCoordinates(newCoordinates: GeodeticMapCoordinates): MapFeature =
        MapVectorImageFeature(newCoordinates, painter, size, zoomRange, layer)
}

@Composable
public fun MapVectorImageFeature(
    position: GeodeticMapCoordinates,
    image: ImageVector,
    size: DpSize = DpSize(20.dp, 20.dp),
    zoomRange: IntRange = defaultZoomRange,
    layer: Int
): MapVectorImageFeature = MapVectorImageFeature(position, rememberVectorPainter(image), size, zoomRange, layer)

/**
 * A group of other features
 */
public class MapFeatureGroup(
    public val children: Map<FeatureId, MapFeature>,
    override val zoomRange: IntRange = defaultZoomRange,
    override val layer: Int
) : MapFeature {
    override fun getBoundingBox(zoom: Double): GmcRectangle? =
        children.values.mapNotNull { it.getBoundingBox(zoom) }.wrapAll()
}

public class MapTextFeature(
    public val position: GeodeticMapCoordinates,
    public val text: String,
    override val zoomRange: IntRange = defaultZoomRange,
    public val color: Color,
    public val fontConfig: MapTextFeatureFont.() -> Unit,
    override val layer: Int
) : DraggableMapFeature {
    override fun getBoundingBox(zoom: Double): GmcRectangle = GmcRectangle(position, position)

    override fun withCoordinates(newCoordinates: GeodeticMapCoordinates): MapFeature =
        MapTextFeature(newCoordinates, text, zoomRange, color, fontConfig, layer)
}
