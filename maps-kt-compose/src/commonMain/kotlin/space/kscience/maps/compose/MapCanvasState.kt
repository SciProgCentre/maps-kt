package space.kscience.maps.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.DpRect
import androidx.compose.ui.unit.dp
import space.kscience.kmath.geometry.radians
import space.kscience.maps.compose.MapCanvasState.Companion.remember
import space.kscience.maps.coordinates.Gmc
import space.kscience.maps.coordinates.MercatorProjection
import space.kscience.maps.coordinates.WebMercatorCoordinates
import space.kscience.maps.coordinates.WebMercatorProjection
import space.kscience.maps.features.*
import kotlin.math.*


/**
 * Represents the state of a map canvas, extending the functionality of [CanvasState] to handle
 * map-specific operations.
 *
 * This class utilizes the Web Mercator projection for map rendering and provides utilities
 * to manage zoom levels, convert coordinates, and track view states.
 * It operates with
 * geodetic map coordinates (GMC) and simplifies interaction with the map's coordinate space.
 *
 * The class is internal to prevent direct instantiation; use the [remember] function to
 * create or get a [MapCanvasState] instance within a composable.
 *
 * @constructor
 * Creates an instance of [MapCanvasState] with the given configuration and default tile size
 * (used only for scale computation).
 *
 * @param config The configuration for view-related behaviors such as zoom, clicks, and canvas size changes.
 * @param tileSize The tile size used to compute scale, defaulting to [MapTileProvider.DEFAULT_TILE_SIZE].
 */
public class MapCanvasState internal constructor(
    config: ViewConfig<Gmc>,
    public val tileSize: Int = MapTileProvider.DEFAULT_TILE_SIZE
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
                canvasSize.width.value / rectangle.longitudeDelta.toRadians().value,
                canvasSize.height.value / rectangle.latitudeDelta.toRadians().value
            ) * 2 * PI / tileSize
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
            config: ViewConfig<Gmc> = ViewConfig(),
            initialViewPoint: ViewPoint<Gmc>? = null,
            initialRectangle: Rectangle<Gmc>? = null,
            tileSize: Int = MapTileProvider.DEFAULT_TILE_SIZE,
        ): MapCanvasState = remember {
            MapCanvasState(config, tileSize).apply {
                if (initialViewPoint != null) {
                    viewPoint = initialViewPoint
                } else if (initialRectangle != null) {
                    viewPoint = computeViewPoint(initialRectangle)
                }
            }
        }
    }
}
