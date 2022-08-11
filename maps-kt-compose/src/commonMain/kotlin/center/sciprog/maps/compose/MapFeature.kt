package center.sciprog.maps.compose

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import center.sciprog.maps.coordinates.GeodeticMapCoordinates
import center.sciprog.maps.coordinates.GmcBox
import center.sciprog.maps.coordinates.wrapAll

public interface MapFeature {
    public val zoomRange: IntRange
    public fun getBoundingBox(zoom: Int): GmcBox?
}

public fun Iterable<MapFeature>.computeBoundingBox(zoom: Int): GmcBox? =
    mapNotNull { it.getBoundingBox(zoom) }.wrapAll()

internal fun Pair<Double, Double>.toCoordinates() = GeodeticMapCoordinates.ofDegrees(first, second)

internal val defaultZoomRange = 1..18

/**
 * A feature that decides what to show depending on the zoom value (it could change size of shape)
 */
public class MapFeatureSelector(
    public val selector: (zoom: Int) -> MapFeature,
) : MapFeature {
    override val zoomRange: IntRange get() = defaultZoomRange

    override fun getBoundingBox(zoom: Int): GmcBox? = selector(zoom).getBoundingBox(zoom)
}

public class MapDrawFeature(
    public val position: GeodeticMapCoordinates,
    override val zoomRange: IntRange = defaultZoomRange,
    private val computeBoundingBox: (zoom: Int) -> GmcBox,
    public val drawFeature: DrawScope.() -> Unit,
) : MapFeature {
    override fun getBoundingBox(zoom: Int): GmcBox = computeBoundingBox(zoom)
}

public class MapCircleFeature(
    public val center: GeodeticMapCoordinates,
    override val zoomRange: IntRange = defaultZoomRange,
    public val size: Float = 5f,
    public val color: Color = Color.Red,
) : MapFeature {
    override fun getBoundingBox(zoom: Int): GmcBox = GmcBox(center, center)
}

public class MapRectangleFeature(
    public val center: GeodeticMapCoordinates,
    override val zoomRange: IntRange = defaultZoomRange,
    public val size: DpSize = DpSize(5.dp, 5.dp),
    public val color: Color = Color.Red,
) : MapFeature {
    override fun getBoundingBox(zoom: Int): GmcBox = GmcBox(center, center)
}

public class MapLineFeature(
    public val a: GeodeticMapCoordinates,
    public val b: GeodeticMapCoordinates,
    override val zoomRange: IntRange = defaultZoomRange,
    public val color: Color = Color.Red,
) : MapFeature {
    override fun getBoundingBox(zoom: Int): GmcBox = GmcBox(a, b)
}

public class MapArcFeature(
    public val oval: GmcBox,
    public val startAngle: Float,
    public val endAngle: Float,
    override val zoomRange: IntRange = defaultZoomRange,
    public val color: Color = Color.Red,
) : MapFeature {
    override fun getBoundingBox(zoom: Int): GmcBox = oval
}

public class MapBitmapImageFeature(
    public val position: GeodeticMapCoordinates,
    public val image: ImageBitmap,
    public val size: IntSize = IntSize(15, 15),
    override val zoomRange: IntRange = defaultZoomRange,
) : MapFeature {
    override fun getBoundingBox(zoom: Int): GmcBox = GmcBox(position, position)
}

public class MapVectorImageFeature(
    public val position: GeodeticMapCoordinates,
    public val painter: Painter,
    public val size: DpSize,
    override val zoomRange: IntRange = defaultZoomRange,
) : MapFeature {
    override fun getBoundingBox(zoom: Int): GmcBox = GmcBox(position, position)
}

@Composable
public fun MapVectorImageFeature(
    position: GeodeticMapCoordinates,
    image: ImageVector,
    size: DpSize = DpSize(20.dp, 20.dp),
    zoomRange: IntRange = defaultZoomRange,
): MapVectorImageFeature = MapVectorImageFeature(position, rememberVectorPainter(image), size, zoomRange)

/**
 * A group of other features
 */
public class MapFeatureGroup(
    public val children: Map<FeatureId, MapFeature>,
    override val zoomRange: IntRange = defaultZoomRange,
) : MapFeature {
    override fun getBoundingBox(zoom: Int): GmcBox? = children.values.mapNotNull { it.getBoundingBox(zoom) }.wrapAll()
}