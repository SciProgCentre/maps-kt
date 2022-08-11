package center.sciprog.maps.compose

import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import center.sciprog.maps.coordinates.*
import mu.KotlinLogging
import kotlin.math.*

@Composable
public fun MapViewState(
    initialViewPoint: (canvasSize: DpSize) -> MapViewPoint,
    mapTileProvider: MapTileProvider,
    features: Map<FeatureId, MapFeature> = emptyMap(),
    inferViewBoxFromFeatures: Boolean = false,
    buildFeatures: @Composable (MapFeatureBuilder.() -> Unit) = {},
): MapViewState {
    val featuresBuilder = MapFeatureBuilderImpl(features)
    featuresBuilder.buildFeatures()
    return MapViewState(
        initialViewPoint = initialViewPoint,
        mapTileProvider = mapTileProvider,
        features = featuresBuilder.build(),
        inferViewBoxFromFeatures = inferViewBoxFromFeatures
    )
}

public class MapViewState(
    public val initialViewPoint: (canvasSize: DpSize) -> MapViewPoint,
    public val mapTileProvider: MapTileProvider,
    public val features: Map<FeatureId, MapFeature> = emptyMap(),
    inferViewBoxFromFeatures: Boolean = false,
) {
    public var canvasSize: DpSize by mutableStateOf(DpSize(512.dp, 512.dp))
    public var viewPointInternal: MapViewPoint? by mutableStateOf(null)
    public val viewPoint: MapViewPoint by derivedStateOf {
        viewPointInternal ?: if (inferViewBoxFromFeatures) {
            features.values.computeBoundingBox(1)?.let { box ->
                val zoom = log2(
                    min(
                        canvasSize.width.value / box.width,
                        canvasSize.height.value / box.height
                    ) * PI / mapTileProvider.tileSize
                )
                MapViewPoint(box.center, zoom)
            } ?: initialViewPoint(canvasSize)
        } else {
            initialViewPoint(canvasSize)
        }
    }
    public val zoom: Int by derivedStateOf { floor(viewPoint.zoom).toInt() }

    public val tileScale: Double by derivedStateOf { 2.0.pow(viewPoint.zoom - zoom) }

    public val mapTiles: SnapshotStateList<MapTile> = mutableStateListOf()

    public val centerCoordinates: WebMercatorCoordinates by derivedStateOf {
        WebMercatorProjection.toMercator(
            viewPoint.focus,
            zoom
        )
    }

    public fun DpOffset.toMercator(): WebMercatorCoordinates = WebMercatorCoordinates(
        zoom,
        (x - canvasSize.width / 2).value / tileScale + centerCoordinates.x,
        (y - canvasSize.height / 2).value / tileScale + centerCoordinates.y,
    )

    /*
     * Convert screen independent offset to GMC, adjusting for fractional zoom
     */
    public fun DpOffset.toGeodetic(): GeodeticMapCoordinates =
        with(this@MapViewState) { WebMercatorProjection.toGeodetic(toMercator()) }

    // Selection rectangle. If null - no selection
    public var selectRect: Rect? by mutableStateOf(null)

    public fun WebMercatorCoordinates.toOffset(density: Density): Offset =
        with(density) {
            with(this@MapViewState) {
                Offset(
                    (canvasSize.width / 2 + (x.dp - centerCoordinates.x.dp) * tileScale.toFloat()).toPx(),
                    (canvasSize.height / 2 + (y.dp - centerCoordinates.y.dp) * tileScale.toFloat()).toPx()
                )
            }
        }

    //Convert GMC to offset in pixels (not DP), adjusting for zoom
    public fun GeodeticMapCoordinates.toOffset(density: Density): Offset =
        WebMercatorProjection.toMercator(this, zoom).toOffset(density)

    private val logger = KotlinLogging.logger("MapViewState")

    public fun DrawScope.drawFeature(zoom: Int, feature: MapFeature) {
        when (feature) {
            is MapFeatureSelector -> drawFeature(zoom, feature.selector(zoom))
            is MapCircleFeature -> drawCircle(
                feature.color,
                feature.size,
                center = feature.center.toOffset(this@drawFeature)
            )
            is MapRectangleFeature -> drawRect(
                feature.color,
                topLeft = feature.center.toOffset(this@drawFeature) - Offset(
                    feature.size.width.toPx() / 2,
                    feature.size.height.toPx() / 2
                ),
                size = feature.size.toSize()
            )
            is MapLineFeature -> drawLine(
                feature.color,
                feature.a.toOffset(this@drawFeature),
                feature.b.toOffset(this@drawFeature)
            )
            is MapArcFeature -> {
                val topLeft = feature.oval.topLeft.toOffset(this@drawFeature)
                val bottomRight = feature.oval.bottomRight.toOffset(this@drawFeature)

                val path = Path().apply {
                    addArcRad(Rect(topLeft, bottomRight), feature.startAngle, feature.endAngle - feature.startAngle)
                }

                drawPath(path, color = feature.color, style = Stroke())

            }
            is MapBitmapImageFeature -> drawImage(
                image = feature.image,
                topLeft = feature.position.toOffset(this@drawFeature)
            )
            is MapVectorImageFeature -> {
                val offset = feature.position.toOffset(this@drawFeature)
                val size = feature.size.toSize()
                translate(offset.x - size.width / 2, offset.y - size.height / 2) {
                    with(feature.painter) {
                        draw(size)
                    }
                }
            }
            is MapTextFeature -> drawIntoCanvas { canvas ->
                val offset = feature.position.toOffset(this@drawFeature)
                canvas.nativeCanvas.drawString(
                    feature.text,
                    offset.x + 5,
                    offset.y - 5,
                    Font().apply(feature.fontConfig),
                    feature.color
                )
            }
            is MapDrawFeature -> {
                val offset = feature.position.toOffset(this)
                translate(offset.x, offset.y) {
                    feature.drawFeature(this)
                }
            }
            is MapFeatureGroup -> {
                feature.children.values.forEach {
                    drawFeature(zoom, it)
                }
            }
            else -> {
                logger.error { "Unrecognized feature type: ${feature::class}" }
            }
        }
    }
}




