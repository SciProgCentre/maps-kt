package space.kscience.maps.scheme

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.DpSize
import io.github.oshai.kotlinlogging.KotlinLogging
import space.kscience.maps.compose.canvasControls
import space.kscience.maps.features.*
import kotlin.math.min


private val logger = KotlinLogging.logger("SchemeView")

/**
 * A composable function that renders a view for visualizing and interacting with spatial features
 * within a two-dimensional coordinate system using a canvas.
 *
 * @param state The state object that manages the overall configuration and state of the canvas,
 * including zoom, focus, and coordinate transformations.
 * @param featureStore The store containing spatial features to be rendered on the canvas. It also
 * provides reactive updates for changes in the feature set.
 * @param modifier The Modifier applied to the composable. Defaults to filling the available space.
 * @return Unit
 */
@Composable
public fun SchemeView(
    state: XYCanvasState,
    featureStore: FeatureStore<XY>,
    modifier: Modifier = Modifier.fillMaxSize(),
): Unit {
    FeatureCanvas(state, featureStore.featureFlow, modifier = modifier.canvasControls(state, featureStore))
}

/**
 * Computes a view point for the canvas based on the current rectangle dimensions
 * and the specified canvas size.
 * The view point includes a calculated zoom value
 * to fit the rectangle within the canvas and its center as the focal point.
 *
 * @param canvasSize the size of the canvas on which the rectangle will be displayed.
 *        Defaults to the internal `defaultCanvasSize` value if not provided.
 * @return a [ViewPoint] object with the rectangle's center as the focal point and
 *         the computed zoom level required to fit the rectangle within the given canvas size.
 */
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
    features: FeatureStore<XY>,
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
    buildFeatures: FeatureStore<XY>.() -> Unit = {},
) {
    val featureState = FeatureStore.remember(XYCoordinateSpace, buildFeatures)
    val mapState: XYCanvasState = XYCanvasState.remember(
        config,
        initialViewPoint = initialViewPoint,
        initialRectangle = initialRectangle ?: featureState.getBoundingBox(
            Float.MAX_VALUE
        ),
    )

    SchemeView(mapState, featureState, modifier)
}

