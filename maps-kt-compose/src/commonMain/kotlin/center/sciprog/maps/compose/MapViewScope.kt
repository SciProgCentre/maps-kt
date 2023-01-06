package center.sciprog.maps.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.DpRect
import androidx.compose.ui.unit.dp
import center.sciprog.maps.coordinates.*
import center.sciprog.maps.features.*
import kotlin.math.*

public class MapViewScope internal constructor(
    public val mapTileProvider: MapTileProvider,
    config: ViewConfig<Gmc>,
) : CoordinateViewScope<Gmc>(config) {
    override val space: CoordinateSpace<Gmc> get() = WebMercatorSpace

    private val scaleFactor: Float
        get() = WebMercatorProjection.scaleFactor(zoom)

    public val intZoom: Int get() = floor(zoom).toInt()

    public val centerCoordinates: WebMercatorCoordinates
        get() = WebMercatorProjection.toMercator(viewPoint.focus, intZoom)

    public val tileScale: Float
        get() = 2f.pow(zoom - floor(zoom))

    /*
     * Convert screen independent offset to GMC, adjusting for fractional zoom
     */
    override fun DpOffset.toCoordinates(): Gmc {
        val mercator = WebMercatorCoordinates(
            intZoom,
            (x - canvasSize.width / 2).value / tileScale + centerCoordinates.x,
            (y - canvasSize.height / 2).value / tileScale + centerCoordinates.y,
        )
        return WebMercatorProjection.toGeodetic(mercator)
    }

    override fun Gmc.toDpOffset(): DpOffset {
        val mercator = WebMercatorProjection.toMercator(this, intZoom)
        return DpOffset(
            (canvasSize.width / 2 + (mercator.x.dp - centerCoordinates.x.dp) * tileScale),
            (canvasSize.height / 2 + (mercator.y.dp - centerCoordinates.y.dp) * tileScale)
        )
    }

    override fun Rectangle<Gmc>.toDpRect(): DpRect {
        val topLeft = topLeft.toDpOffset()
        val bottomRight = bottomRight.toDpOffset()
        return DpRect(topLeft.x, topLeft.y, bottomRight.x, bottomRight.y)
    }

    override fun computeViewPoint(rectangle: Rectangle<Gmc>): ViewPoint<Gmc> {
        val zoom = log2(
            min(
                canvasSize.width.value / rectangle.longitudeDelta.radians.value,
                canvasSize.height.value / rectangle.latitudeDelta.radians.value
            ) * PI / mapTileProvider.tileSize
        )
        return space.ViewPoint(rectangle.center, zoom.toFloat())
    }

    override fun ViewPoint<Gmc>.moveBy(x: Dp, y: Dp): ViewPoint<Gmc> {
        val deltaX = x.value / tileScale
        val deltaY = y.value / tileScale
        val newCoordinates = GeodeticMapCoordinates(
            (focus.latitude + (deltaY / scaleFactor).radians).coerceIn(
                -MercatorProjection.MAXIMUM_LATITUDE,
                MercatorProjection.MAXIMUM_LATITUDE
            ),
            focus.longitude + (deltaX / scaleFactor).radians
        )
        return space.ViewPoint(newCoordinates, zoom)
    }

    public companion object {
        @Composable
        public fun remember(
            mapTileProvider: MapTileProvider,
            config: ViewConfig<Gmc> = ViewConfig(),
            initialViewPoint: ViewPoint<Gmc>? = null,
            initialRectangle: Rectangle<Gmc>? = null,
        ): MapViewScope = remember {
            MapViewScope(mapTileProvider, config).also { mapState ->
                if (initialViewPoint != null) {
                    mapState.viewPoint = initialViewPoint
                } else if (initialRectangle != null) {
                    mapState.viewPoint = mapState.computeViewPoint(initialRectangle)
                }
            }
        }
    }
}