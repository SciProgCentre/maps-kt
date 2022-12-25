package center.sciprog.maps.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.*
import center.sciprog.maps.coordinates.*
import center.sciprog.maps.features.*
import kotlin.math.*

internal class MapState internal constructor(
    config: ViewConfig<Gmc>,
    canvasSize: DpSize,
    viewPoint: ViewPoint<Gmc>,
    val tileSize: Int,
) : CoordinateViewState<Gmc>(config, canvasSize, viewPoint) {
    override val space: CoordinateSpace<Gmc> get() = GmcCoordinateSpace

    public val scaleFactor: Double
        get() = WebMercatorProjection.scaleFactor(viewPoint.zoom)

    val intZoom: Int get() = floor(zoom).toInt()

    public val centerCoordinates: WebMercatorCoordinates
        get() = WebMercatorProjection.toMercator(viewPoint.focus, intZoom)

    internal val tileScale: Double
        get() = 2.0.pow(viewPoint.zoom - floor(viewPoint.zoom))

    private fun DpOffset.toMercator(): WebMercatorCoordinates = WebMercatorCoordinates(
        intZoom,
        (x - canvasSize.width / 2).value / tileScale + centerCoordinates.x,
        (y - canvasSize.height / 2).value / tileScale + centerCoordinates.y,
    )

    /*
     * Convert screen independent offset to GMC, adjusting for fractional zoom
     */
    override fun DpOffset.toCoordinates(): Gmc =
        WebMercatorProjection.toGeodetic(toMercator())

    internal fun WebMercatorCoordinates.toOffset(): DpOffset = DpOffset(
        (canvasSize.width / 2 + (x.dp - centerCoordinates.x.dp) * tileScale.toFloat()),
        (canvasSize.height / 2 + (y.dp - centerCoordinates.y.dp) * tileScale.toFloat())
    )

    override fun Gmc.toDpOffset(): DpOffset =
        WebMercatorProjection.toMercator(this, intZoom).toOffset()

    override fun Rectangle<Gmc>.toDpRect(): DpRect {
        val topLeft = topLeft.toDpOffset()
        val bottomRight = bottomRight.toDpOffset()
        return DpRect(topLeft.x, topLeft.y, bottomRight.x, bottomRight.y)
    }

    override fun viewPointFor(rectangle: Rectangle<Gmc>): ViewPoint<Gmc> {
        val zoom = log2(
            min(
                canvasSize.width.value / rectangle.longitudeDelta.radians.value,
                canvasSize.height.value / rectangle.latitudeDelta.radians.value
            ) * PI / tileSize
        )
        return MapViewPoint(rectangle.center, zoom)
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
        return MapViewPoint(newCoordinates, zoom)
    }
}

@Composable
internal fun rememberMapState(
    config: ViewConfig<Gmc>,
    canvasSize: DpSize,
    viewPoint: ViewPoint<Gmc>,
    tileSize: Int,
): MapState = remember {
    MapState(config, canvasSize, viewPoint, tileSize)
}