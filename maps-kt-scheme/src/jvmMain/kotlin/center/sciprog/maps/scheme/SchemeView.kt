package center.sciprog.maps.scheme

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.unit.DpSize
import center.sciprog.attributes.z
import center.sciprog.maps.compose.mapControls
import center.sciprog.maps.features.*
import mu.KotlinLogging
import kotlin.math.min


private val logger = KotlinLogging.logger("SchemeView")

@Composable
public fun SchemeView(
    state: XYViewScope,
    features: FeatureGroup<XY>,
    modifier: Modifier = Modifier.fillMaxSize(),
): Unit = key(state, features) {
    with(state) {
        //Can't do that inside canvas
        val painterCache: Map<PainterFeature<XY>, Painter> =
            features.features.filterIsInstance<PainterFeature<XY>>().associateWith { it.getPainter() }

        Canvas(modifier = modifier.mapControls(state, features)) {

            if (canvasSize != size.toDpSize()) {
                canvasSize = size.toDpSize()
                logger.debug { "Recalculate canvas. Size: $size" }
            }

            clipRect {
                features.featureMap.values.sortedBy { it.z }
                    .filter { viewPoint.zoom in it.zoomRange }
                    .forEach { feature ->
                        drawFeature(state, painterCache, feature)
                    }
            }

            selectRect?.let { dpRect ->
                val rect = dpRect.toRect()
                drawRect(
                    color = Color.Blue,
                    topLeft = rect.topLeft,
                    size = rect.size,
                    alpha = 0.5f,
                    style = Stroke(
                        width = 2f,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                    )
                )
            }
        }
    }

}

public fun Rectangle<XY>.computeViewPoint(
    canvasSize: DpSize = defaultCanvasSize,
): ViewPoint<XY> {
    val zoom = min(
        canvasSize.width.value / width,
        canvasSize.height.value / height
    )

    return XYViewPoint(center, zoom.toFloat())
}

/**
 * A builder for a Scheme with static features.
 */
@Composable
public fun SchemeView(
    features: FeatureGroup<XY>,
    initialViewPoint: ViewPoint<XY>? = null,
    initialRectangle: Rectangle<XY>? = null,
    config: ViewConfig<XY> = ViewConfig(),
    modifier: Modifier = Modifier.fillMaxSize(),
) {

    val state = XYViewScope.remember(
        config,
        initialViewPoint = initialViewPoint,
        initialRectangle = initialRectangle ?: features.getBoundingBox(Float.MAX_VALUE),
    )

    SchemeView(state, features, modifier)
}

/**
 * Draw a scheme using convenient parameters. If neither [initialViewPoint], noe [initialRectangle] is defined,
 * use map features to infer view region.
 * @param initialViewPoint The view point of the map using center and zoom. Is used if provided
 * @param initialRectangle The rectangle to be used for view point computation. Used if [initialViewPoint] is not defined.
 * @param buildFeatures - a builder for features
 */
@Composable
public fun SchemeView(
    initialViewPoint: ViewPoint<XY>? = null,
    initialRectangle: Rectangle<XY>? = null,
    config: ViewConfig<XY> = ViewConfig(),
    modifier: Modifier = Modifier.fillMaxSize(),
    buildFeatures: FeatureGroup<XY>.() -> Unit = {},
) {
    val featureState = FeatureGroup.remember(XYCoordinateSpace, buildFeatures)
    val mapState: XYViewScope = XYViewScope.remember(
        config,
        initialViewPoint = initialViewPoint,
        initialRectangle = initialRectangle ?: featureState.features.computeBoundingBox(
            XYCoordinateSpace,
            Float.MAX_VALUE
        ),
    )

    SchemeView(mapState, featureState, modifier)
}

