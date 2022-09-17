package center.sciprog.maps.scheme

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.gestures.forEachGesture
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.*
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import mu.KotlinLogging
import org.jetbrains.skia.Font
import org.jetbrains.skia.Paint
import kotlin.math.max
import kotlin.math.min


private fun Color.toPaint(): Paint = Paint().apply {
    isAntiAlias = true
    color = toArgb()
}

private fun IntRange.intersect(other: IntRange) = max(first, other.first)..min(last, other.last)

private val logger = KotlinLogging.logger("SchemeView")

data class SchemeViewConfig(
    val zoomSpeed: Float = 1f / 3f,
    val onClick: SchemeViewPoint.() -> Unit = {},
    val onViewChange: SchemeViewPoint.() -> Unit = {},
    val onSelect: (SchemeRectangle) -> Unit = {},
    val zoomOnSelect: Boolean = true,
)

@Composable
public fun SchemeView(
    initialViewPoint: SchemeViewPoint,
    featuresState: SchemeFeaturesState,
    config: SchemeViewConfig = SchemeViewConfig(),
    modifier: Modifier = Modifier.fillMaxSize(),
) = key(initialViewPoint) {

    var canvasSize by remember { mutableStateOf(defaultCanvasSize) }

    var viewPoint by remember { mutableStateOf(initialViewPoint) }


    fun setViewPoint(newViewPoint: SchemeViewPoint) {
        config.onViewChange(newViewPoint)
        viewPoint = newViewPoint
    }


    fun DpOffset.toCoordinates(): SchemeCoordinates = SchemeCoordinates(
        (x - canvasSize.width / 2).value / viewPoint.scale + viewPoint.focus.x,
        (canvasSize.height / 2 - y).value / viewPoint.scale + viewPoint.focus.y
    )

    // Selection rectangle. If null - no selection
    var selectRect by remember { mutableStateOf<Rect?>(null) }

    @OptIn(ExperimentalComposeUiApi::class)
    val canvasModifier = modifier.pointerInput(Unit) {
        forEachGesture {
            awaitPointerEventScope {
                fun Offset.toDpOffset() = DpOffset(x.toDp(), y.toDp())

                val event: PointerEvent = awaitPointerEvent()
                event.changes.forEach { change ->
                    if (event.buttons.isPrimaryPressed) {
                        //Evaluating selection frame
                        if (event.keyboardModifiers.isShiftPressed) {
                            selectRect = Rect(change.position, change.position)
                            drag(change.id) { dragChange ->
                                selectRect?.let { rect ->
                                    val offset = dragChange.position
                                    selectRect = Rect(
                                        min(offset.x, rect.left),
                                        min(offset.y, rect.top),
                                        max(offset.x, rect.right),
                                        max(offset.y, rect.bottom)
                                    )
                                }
                            }
                            selectRect?.let { rect ->
                                //Use selection override if it is defined
                                val box = SchemeRectangle(
                                    rect.topLeft.toDpOffset().toCoordinates(),
                                    rect.bottomRight.toDpOffset().toCoordinates()
                                )
                                config.onSelect(box)
                                if (config.zoomOnSelect) {
                                    val newScale = min(
                                        canvasSize.width.value / box.width,
                                        canvasSize.height.value / box.height
                                    )

                                    val newViewPoint = SchemeViewPoint(box.center, newScale)

                                    setViewPoint(newViewPoint)
                                }
                                selectRect = null
                            }
                        } else {
                            val dragStart = change.position
                            val dpPos = DpOffset(dragStart.x.toDp(), dragStart.y.toDp())
                            config.onClick(SchemeViewPoint(dpPos.toCoordinates(), viewPoint.scale))
                            drag(change.id) { dragChange ->
                                val dragAmount = dragChange.position - dragChange.previousPosition
                                val newViewPoint = viewPoint.move(
                                    -dragAmount.x.toDp().value / viewPoint.scale,
                                    dragAmount.y.toDp().value / viewPoint.scale
                                )
                                setViewPoint(newViewPoint)
                            }
                        }
                    }
                }
            }
        }
    }.onPointerEvent(PointerEventType.Scroll) {
        val change = it.changes.first()
        val (xPos, yPos) = change.position
        //compute invariant point of translation
        val invariant = DpOffset(xPos.toDp(), yPos.toDp()).toCoordinates()
        val newViewPoint = viewPoint.zoom(-change.scrollDelta.y * config.zoomSpeed, invariant)
        setViewPoint(newViewPoint)
    }.fillMaxSize()

    val painterCache = key(featuresState) {
        featuresState.features().values.filterIsInstance<PainterFeature>().associateWith { it.painter() }
    }

    Canvas(canvasModifier) {
        fun SchemeCoordinates.toOffset(): Offset = Offset(
            (canvasSize.width / 2 + (x.dp - viewPoint.focus.x.dp) * viewPoint.scale).toPx(),
            (canvasSize.height / 2 + (viewPoint.focus.y.dp - y.dp) * viewPoint.scale).toPx()
        )


        fun DrawScope.drawFeature(scale: Float, feature: SchemeFeature) {
            when (feature) {
                is SchemeBackgroundFeature -> {
                    val offset = SchemeCoordinates(feature.rectangle.left, feature.rectangle.top).toOffset()

                    val backgroundSize = DpSize(
                        (feature.rectangle.width * scale).dp,
                        (feature.rectangle.height * scale).dp
                    ).toSize()

                    translate(offset.x, offset.y) {
                        with(painterCache[feature]!!) {
                            draw(backgroundSize)
                        }
                    }
                }

                is SchemeFeatureSelector -> drawFeature(scale, feature.selector(scale))
                is SchemeCircleFeature -> drawCircle(
                    feature.color,
                    feature.size,
                    center = feature.center.toOffset()
                )

                is SchemeLineFeature -> drawLine(feature.color, feature.a.toOffset(), feature.b.toOffset())
                is SchemeArcFeature -> {
                    val topLeft = feature.oval.leftTop.toOffset()
                    val bottomRight = feature.oval.rightBottom.toOffset()

                    val path = Path().apply {
                        addArcRad(
                            Rect(topLeft, bottomRight),
                            feature.startAngle,
                            feature.arcLength
                        )
                    }

                    drawPath(path, color = feature.color, style = Stroke())

                }

                is SchemeBitmapFeature -> drawImage(feature.image, feature.position.toOffset())
                is SchemeImageFeature -> {
                    val offset = feature.position.toOffset()
                    val imageSize = feature.size.toSize()
                    translate(offset.x - imageSize.width / 2, offset.y - imageSize.height / 2) {
                        with(painterCache[feature]!!) {
                            draw(imageSize)
                        }
                    }
                }

                is SchemeTextFeature -> drawIntoCanvas { canvas ->
                    val offset = feature.position.toOffset()
                    canvas.nativeCanvas.drawString(
                        feature.text,
                        offset.x + 5,
                        offset.y - 5,
                        Font().apply { size = 16f },
                        feature.color.toPaint()
                    )
                }

                is SchemeDrawFeature -> {
                    val offset = feature.position.toOffset()
                    translate(offset.x, offset.y) {
                        feature.drawFeature(this)
                    }
                }

                is SchemeFeatureGroup -> {
                    feature.children.values.forEach {
                        drawFeature(scale, it)
                    }
                }
            }
        }

        if (canvasSize != size.toDpSize()) {
            canvasSize = size.toDpSize()
            logger.debug { "Recalculate canvas. Size: $size" }
        }
        clipRect {
            featuresState.features().values.filterIsInstance<SchemeBackgroundFeature>().forEach { background ->
                drawFeature(viewPoint.scale, background)
            }
            featuresState.features().values.filter {
                it !is SchemeBackgroundFeature && viewPoint.scale in it.scaleRange
            }.forEach { feature ->
                drawFeature(viewPoint.scale, feature)
            }
        }
        selectRect?.let { rect ->
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


/**
 * A builder for a Scheme with static features.
 */
@Composable
public fun SchemeView(
    initialViewPoint: SchemeViewPoint? = null,
    initialRectangle: SchemeRectangle? = null,
    featureMap: Map<FeatureId, SchemeFeature>,
    config: SchemeViewConfig = SchemeViewConfig(),
    modifier: Modifier = Modifier.fillMaxSize(),
) {
    val featuresState = key(featureMap) {
        SchemeFeaturesState.build {
            featureMap.forEach(::addFeature)
        }
    }

    val viewPointOverride: SchemeViewPoint = remember(initialViewPoint, initialRectangle) {
        initialViewPoint
            ?: initialRectangle?.computeViewPoint()
            ?: featureMap.values.computeBoundingBox(1f)?.computeViewPoint()
            ?: SchemeViewPoint(SchemeCoordinates(0f, 0f))
    }

    SchemeView(viewPointOverride, featuresState, config, modifier)
}

/**
 * Draw a map using convenient parameters. If neither [initialViewPoint], noe [initialRectangle] is defined,
 * use map features to infer view region.
 * @param initialViewPoint The view point of the map using center and zoom. Is used if provided
 * @param initialRectangle The rectangle to be used for view point computation. Used if [initialViewPoint] is not defined.
 * @param buildFeatures - a builder for features
 */
@Composable
public fun SchemeView(
    initialViewPoint: SchemeViewPoint? = null,
    initialRectangle: SchemeRectangle? = null,
    config: SchemeViewConfig = SchemeViewConfig(),
    modifier: Modifier = Modifier.fillMaxSize(),
    buildFeatures: SchemeFeaturesState.() -> Unit = {},
) {
    val featureState = SchemeFeaturesState.remember(buildFeatures)

    val features = featureState.features()

    val viewPointOverride: SchemeViewPoint = remember(initialViewPoint, initialRectangle) {
        initialViewPoint
            ?: initialRectangle?.computeViewPoint()
            ?: features.values.computeBoundingBox(1f)?.computeViewPoint()
            ?: SchemeViewPoint(SchemeCoordinates(0f, 0f))
    }

//    val featureDrag = DragHandle.withPrimaryButton { _, start, end ->
//        val zoom = start.zoom
//        featureState.findAllWithAttribute(DraggableAttribute) { it }.forEach { id ->
//            val feature = features[id] as? DraggableMapFeature ?: return@forEach
//            val boundingBox = feature.getBoundingBox(zoom) ?: return@forEach
//            if (start.focus in boundingBox) {
//                featureState.addFeature(id, feature.withCoordinates(end.focus))
//                return@withPrimaryButton false
//            }
//        }
//        return@withPrimaryButton true
//    }
//
//
//    val newConfig = config.copy(
//        dragHandle = DragHandle.combine(featureDrag, config.dragHandle)
//    )

    SchemeView(
        initialViewPoint = viewPointOverride,
        featuresState = featureState,
        config = config,
        modifier = modifier,
    )
}
