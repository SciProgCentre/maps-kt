package center.sciprog.maps.scheme

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.DpSize
import center.sciprog.maps.compose.canvasControls
import center.sciprog.maps.features.*
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlin.math.min


private val logger = KotlinLogging.logger("SchemeView")

@Composable
public fun SchemeView(
    state: XYCanvasState,
    features: FeatureGroup<XY>,
    modifier: Modifier = Modifier.fillMaxSize(),
): Unit {
    FeatureCanvas(state, features, modifier = modifier.canvasControls(state, features))
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

    val state = XYCanvasState.remember(
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
    val mapState: XYCanvasState = XYCanvasState.remember(
        config,
        initialViewPoint = initialViewPoint,
        initialRectangle = initialRectangle ?: featureState.features.computeBoundingBox(
            XYCoordinateSpace,
            Float.MAX_VALUE
        ),
    )

    SchemeView(mapState, featureState, modifier)
}

