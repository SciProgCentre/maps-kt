package center.sciprog.maps.compose

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.NativeCanvas
import center.sciprog.maps.coordinates.GeodeticMapCoordinates
import center.sciprog.maps.coordinates.GmcBox

public expect class Font constructor() {
    public var size: Float
}

public expect fun NativeCanvas.drawString(text: String, x: Float, y: Float, font: Font, color: Color)

public class MapTextFeature(
    public val position: GeodeticMapCoordinates,
    public val text: String,
    override val zoomRange: IntRange = defaultZoomRange,
    public val color: Color,
    public val fontConfig: Font.() -> Unit,
) : MapFeature {
    override fun getBoundingBox(zoom: Int): GmcBox = GmcBox(position, position)
}

public fun MapFeatureBuilder.text(
    position: GeodeticMapCoordinates,
    text: String,
    zoomRange: IntRange = defaultZoomRange,
    color: Color = Color.Red,
    font: Font.() -> Unit = { size = 16f },
    id: FeatureId? = null,
): FeatureId = addFeature(id, MapTextFeature(position, text, zoomRange, color, font))

public fun MapFeatureBuilder.text(
    position: Pair<Double, Double>,
    text: String,
    zoomRange: IntRange = defaultZoomRange,
    color: Color = Color.Red,
    font: Font.() -> Unit = { size = 16f },
    id: FeatureId? = null,
): FeatureId = addFeature(id, MapTextFeature(position.toCoordinates(), text, zoomRange, color, font))
