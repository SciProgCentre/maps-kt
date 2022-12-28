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
import center.sciprog.maps.compose.mapControls
import center.sciprog.maps.features.*
import mu.KotlinLogging
import kotlin.math.min


private val logger = KotlinLogging.logger("SchemeView")

@Composable
public fun SchemeView(
    state: XYViewScope,
    featuresState: FeatureCollection<XY>,
    modifier: Modifier = Modifier.fillMaxSize(),
) {
    with(state) {
        val painterCache: Map<PainterFeature<XY>, Painter> = key(featuresState) {
            featuresState.features.values.filterIsInstance<PainterFeature<XY>>().associateWith { it.getPainter() }
        }

        Canvas(modifier = modifier.mapControls(state, featuresState.features).fillMaxSize()) {

            if (canvasSize != size.toDpSize()) {
                canvasSize = size.toDpSize()
                logger.debug { "Recalculate canvas. Size: $size" }
            }

            clipRect {
                featuresState.features.values
                    .filter { viewPoint.zoom in it.zoomRange }
                    .sortedBy { it.z }
                    .forEach { background ->
                        drawFeature(state, painterCache, background)
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
    initialViewPoint: ViewPoint<XY>? = null,
    initialRectangle: Rectangle<XY>? = null,
    featureMap: Map<FeatureId<*>, Feature<XY>>,
    config: ViewConfig<XY> = ViewConfig(),
    modifier: Modifier = Modifier.fillMaxSize(),
) {


    val featureState = key(featureMap) {
        FeatureCollection.build(XYCoordinateSpace) {
            featureMap.forEach { feature(it.key.id, it.value) }
        }
    }

    val state = rememberMapState(
        config,
        initialViewPoint = initialViewPoint,
        initialRectangle = initialRectangle ?: featureState.features.values.computeBoundingBox(XYCoordinateSpace, 1f),
    )

    SchemeView(state, featureState, modifier)
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
    buildFeatures: FeatureCollection<XY>.() -> Unit = {},
) {
    val featureState = FeatureCollection.remember(XYCoordinateSpace, buildFeatures)
    val mapState: XYViewScope = rememberMapState(
        config,
        initialViewPoint = initialViewPoint,
        initialRectangle = initialRectangle ?: featureState.features.values.computeBoundingBox(XYCoordinateSpace, 1f),
    )

    SchemeView(mapState, featureState, modifier)
}

///**
// * A builder for a Scheme with static features.
// */
//@Composable
//public fun SchemeView(
//    initialViewPoint: XYViewPoint? = null,
//    initialRectangle: XYRectangle? = null,
//    featureMap: Map<FeatureId<*>,>,
//    config: SchemeViewConfig = SchemeViewConfig(),
//    modifier: Modifier = Modifier.fillMaxSize(),
//) {
//    val featuresState = key(featureMap) {
//        SchemeFeaturesState.build {
//            featureMap.forEach(::addFeature)
//        }
//    }
//
//    val viewPointOverride: XYViewPoint = remember(initialViewPoint, initialRectangle) {
//        initialViewPoint
//            ?: initialRectangle?.computeViewPoint()
//            ?: featureMap.values.computeBoundingBox(1f)?.computeViewPoint()
//            ?: XYViewPoint(XY(0f, 0f))
//    }
//
//    SchemeView(viewPointOverride, featuresState, config, modifier)
//}
//
///**
// * Draw a map using convenient parameters. If neither [initialViewPoint], noe [initialRectangle] is defined,
// * use map features to infer view region.
// * @param initialViewPoint The view point of the map using center and zoom. Is used if provided
// * @param initialRectangle The rectangle to be used for view point computation. Used if [initialViewPoint] is not defined.
// * @param buildFeatures - a builder for features
// */
//@Composable
//public fun SchemeView(
//    initialViewPoint: XYViewPoint? = null,
//    initialRectangle: Rectangle<XY>? = null,
//    config: ViewConfig<XY> = ViewConfig(),
//    modifier: Modifier = Modifier.fillMaxSize(),
//    buildFeatures: FeaturesState<XY>.() -> Unit = {},
//) {
//    val featureState = FeaturesState.remember(XYCoordinateSpace, buildFeatures)
//
//    val features = featureState.features
//
//    val viewPointOverride: XYViewPoint = remember(initialViewPoint, initialRectangle) {
//        initialViewPoint
//            ?: initialRectangle?.computeViewPoint()
//            ?: features.values.computeBoundingBox(1f)?.computeViewPoint()
//            ?: XYViewPoint(XY(0f, 0f))
//    }
//
////    val featureDrag = DragHandle.withPrimaryButton { _, start, end ->
////        val zoom = start.zoom
////        featureState.findAllWithAttribute(DraggableAttribute) { it }.forEach { id ->
////            val feature = features[id] as? DraggableMapFeature ?: return@forEach
////            val boundingBox = feature.getBoundingBox(zoom) ?: return@forEach
////            if (start.focus in boundingBox) {
////                featureState.addFeature(id, feature.withCoordinates(end.focus))
////                return@withPrimaryButton false
////            }
////        }
////        return@withPrimaryButton true
////    }
////
////
////    val newConfig = config.copy(
////        dragHandle = DragHandle.combine(featureDrag, config.dragHandle)
////    )
//
//    SchemeView(
//        initialViewPoint = viewPointOverride,
//        featuresState = featureState,
//        config = config,
//        modifier = modifier,
//    )
//}
