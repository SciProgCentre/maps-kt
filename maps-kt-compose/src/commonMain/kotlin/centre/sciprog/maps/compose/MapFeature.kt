package centre.sciprog.maps.compose

import androidx.compose.runtime.Composable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.unit.IntSize
import centre.sciprog.maps.GeodeticMapCoordinates
import centre.sciprog.maps.GmcBox
import centre.sciprog.maps.wrapAll

//TODO replace zoom range with zoom-based representation change
sealed class MapFeature(val zoomRange: IntRange) {
    abstract fun getBoundingBox(zoom: Int): GmcBox?
}

fun Iterable<MapFeature>.computeBoundingBox(zoom: Int): GmcBox? =
    mapNotNull { it.getBoundingBox(zoom) }.wrapAll()

internal fun Pair<Double, Double>.toCoordinates() = GeodeticMapCoordinates.ofDegrees(first, second)

internal val defaultZoomRange = 1..18

/**
 * A feature that decides what to show depending on the zoom value (it could change size of shape)
 */
class MapFeatureSelector(val selector: (zoom: Int) -> MapFeature) : MapFeature(defaultZoomRange) {
    override fun getBoundingBox(zoom: Int): GmcBox? = selector(zoom).getBoundingBox(zoom)
}

class MapDrawFeature(
    val position: GeodeticMapCoordinates,
    zoomRange: IntRange = defaultZoomRange,
    val drawFeature: DrawScope.(Offset) -> Unit,
) : MapFeature(zoomRange) {
    override fun getBoundingBox(zoom: Int): GmcBox {
        //TODO add box computation
        return GmcBox(position, position)
    }
}

class MapCircleFeature(
    val center: GeodeticMapCoordinates,
    zoomRange: IntRange = defaultZoomRange,
    val size: Float = 5f,
    val color: Color = Color.Red,
) : MapFeature(zoomRange) {
    override fun getBoundingBox(zoom: Int): GmcBox = GmcBox(center, center)
}

class MapLineFeature(
    val a: GeodeticMapCoordinates,
    val b: GeodeticMapCoordinates,
    zoomRange: IntRange = defaultZoomRange,
    val color: Color = Color.Red,
) : MapFeature(zoomRange) {
    override fun getBoundingBox(zoom: Int): GmcBox = GmcBox(a, b)
}

class MapTextFeature(
    val position: GeodeticMapCoordinates,
    val text: String,
    zoomRange: IntRange = defaultZoomRange,
    val color: Color = Color.Red,
) : MapFeature(zoomRange) {
    override fun getBoundingBox(zoom: Int): GmcBox = GmcBox(position, position)
}

class MapBitmapImageFeature(
    val position: GeodeticMapCoordinates,
    val image: ImageBitmap,
    val size: IntSize = IntSize(15, 15),
    zoomRange: IntRange = defaultZoomRange,
) : MapFeature(zoomRange) {
    override fun getBoundingBox(zoom: Int): GmcBox = GmcBox(position, position)
}

class MapVectorImageFeature(
    val position: GeodeticMapCoordinates,
    val painter: Painter,
    val size: Size,
    zoomRange: IntRange = defaultZoomRange,
) : MapFeature(zoomRange) {
    override fun getBoundingBox(zoom: Int): GmcBox = GmcBox(position, position)
}

@Composable
fun MapVectorImageFeature(
    position: GeodeticMapCoordinates,
    image: ImageVector,
    size: Size = Size(20f, 20f),
    zoomRange: IntRange = defaultZoomRange,
): MapVectorImageFeature = MapVectorImageFeature(position, rememberVectorPainter(image), size, zoomRange)

/**
 * A group of other features
 */
class MapFeatureGroup(
    val children: Map<FeatureId, MapFeature>,
    zoomRange: IntRange = defaultZoomRange,
) : MapFeature(zoomRange) {
    override fun getBoundingBox(zoom: Int): GmcBox? = children.values.mapNotNull { it.getBoundingBox(zoom) }.wrapAll()
}
