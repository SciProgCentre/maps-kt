package centre.sciprog.maps.compose

import androidx.compose.runtime.Composable
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.VectorPainter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.unit.IntSize
import centre.sciprog.maps.GeodeticMapCoordinates

//TODO replace zoom range with zoom-based representation change
sealed class MapFeature(val zoomRange: IntRange)

private val defaultZoomRange = 1..18

internal fun Pair<Double, Double>.toCoordinates() = GeodeticMapCoordinates.ofDegrees(first, second)

class MapCircleFeature(
    val center: GeodeticMapCoordinates,
    zoomRange: IntRange = defaultZoomRange,
    val size: Float = 5f,
    val color: Color = Color.Red,
) : MapFeature(zoomRange)

fun MapCircleFeature(
    centerCoordinates: Pair<Double, Double>,
    zoomRange: IntRange = defaultZoomRange,
    size: Float = 5f,
    color: Color = Color.Red,
) = MapCircleFeature(
    centerCoordinates.toCoordinates(),
    zoomRange,
    size,
    color
)

class MapLineFeature(
    val a: GeodeticMapCoordinates,
    val b: GeodeticMapCoordinates,
    zoomRange: IntRange = defaultZoomRange,
    val color: Color = Color.Red,
) : MapFeature(zoomRange)

fun MapLineFeature(
    aCoordinates: Pair<Double, Double>,
    bCoordinates: Pair<Double, Double>,
    zoomRange: IntRange = defaultZoomRange,
    color: Color = Color.Red,
) = MapLineFeature(aCoordinates.toCoordinates(), bCoordinates.toCoordinates(), zoomRange, color)

class MapTextFeature(
    val position: GeodeticMapCoordinates,
    val text: String,
    zoomRange: IntRange = defaultZoomRange,
    val color: Color = Color.Red,
) : MapFeature(zoomRange)

class MapBitmapImageFeature(
    val position: GeodeticMapCoordinates,
    val image: ImageBitmap,
    val size: IntSize = IntSize(15, 15),
    zoomRange: IntRange = defaultZoomRange,
    val color: Color = Color.Red,
) : MapFeature(zoomRange)


class MapVectorImageFeature internal constructor(
    val position: GeodeticMapCoordinates,
    val painter: VectorPainter,
    val size: Size,
    zoomRange: IntRange = defaultZoomRange,
    val color: Color = Color.Red,
) : MapFeature(zoomRange)

@Composable
fun MapVectorImageFeature(
    position: GeodeticMapCoordinates,
    image: ImageVector,
    size: Size = Size(20f,20f),
    zoomRange: IntRange = defaultZoomRange,
    color: Color = Color.Red,
): MapVectorImageFeature = MapVectorImageFeature(position, rememberVectorPainter(image), size, zoomRange, color)