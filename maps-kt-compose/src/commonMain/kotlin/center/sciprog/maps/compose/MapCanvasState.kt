package center.sciprog.maps.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.DpRect
import androidx.compose.ui.unit.dp
import center.sciprog.maps.coordinates.Gmc
import center.sciprog.maps.coordinates.MercatorProjection
import center.sciprog.maps.coordinates.WebMercatorCoordinates
import center.sciprog.maps.coordinates.WebMercatorProjection
import center.sciprog.maps.features.*
import space.kscience.kmath.geometry.radians
import kotlin.math.*


public class MapCanvasState private constructor(
    public val mapTileProvider: MapTileProvider,
    config: ViewConfig<Gmc>,
) : CanvasState<Gmc>(config) {
    override val space: CoordinateSpace<Gmc> get() = WebMercatorSpace

    private val scaleFactor: Float
        get() = WebMercatorProjection.scaleFactor(zoom)

    public val intZoom: Int get() = floor(zoom).toInt()

    public val centerCoordinates: WebMercatorCoordinates
        get() = WebMercatorProjection.toMercator(viewPoint.focus, intZoom) ?: WebMercatorCoordinates(intZoom, 0f, 0f)

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
        val mercator = WebMercatorProjection.toMercator(this, intZoom) ?: WebMercatorCoordinates(intZoom, 0f, 0f)
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
                canvasSize.width.value / rectangle.longitudeDelta.radians,
                canvasSize.height.value / rectangle.latitudeDelta.radians
            ) * 2 * PI / mapTileProvider.tileSize
        ).coerceIn(0.0..22.0)
        return space.ViewPoint(rectangle.center, zoom.toFloat())
    }

    override fun ViewPoint<Gmc>.moveBy(x: Dp, y: Dp): ViewPoint<Gmc> {
        val deltaX = x.value / tileScale
        val deltaY = y.value / tileScale
        val newCoordinates = Gmc.normalized(
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
        ): MapCanvasState = remember {
            MapCanvasState(mapTileProvider, config).apply {
                if (initialViewPoint != null) {
                    viewPoint = initialViewPoint
                } else if (initialRectangle != null) {
                    viewPoint = computeViewPoint(initialRectangle)
                }
            }
        }
    }
}
