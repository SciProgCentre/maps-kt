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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.*
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
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
    val inferViewBoxFromFeatures: Boolean = false,
    val onClick: SchemeViewPoint.() -> Unit = {},
    val onViewChange: SchemeViewPoint.() -> Unit = {},
    val onSelect: (SchemeCoordinateBox) -> Unit = {},
    val zoomOnSelect: Boolean = true,
)

@Composable
public fun SchemeView(
    computeViewPoint: (canvasSize: DpSize) -> SchemeViewPoint,
    features: Map<FeatureId, SchemeFeature>,
    config: SchemeViewConfig = SchemeViewConfig(),
    modifier: Modifier = Modifier.fillMaxSize(),
) {

    var canvasSize by remember { mutableStateOf(DpSize(512.dp, 512.dp)) }

    var viewPointInternal: SchemeViewPoint? by remember {
        mutableStateOf(null)
    }

    val viewPoint: SchemeViewPoint by derivedStateOf {
        viewPointInternal ?: if (config.inferViewBoxFromFeatures) {
            features.values.computeBoundingBox(1f)?.let { box ->
                val scale = min(
                    canvasSize.width.value / box.width,
                    canvasSize.height.value / box.height
                )
                SchemeViewPoint(box.center, scale)
            } ?: computeViewPoint(canvasSize)
        } else {
            computeViewPoint(canvasSize)
        }
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
                                val box = SchemeCoordinateBox(
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

                                    config.onViewChange(newViewPoint)
                                    viewPointInternal = newViewPoint
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
                                config.onViewChange(newViewPoint)
                                viewPointInternal = newViewPoint
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
        config.onViewChange(newViewPoint)
        viewPointInternal = newViewPoint
    }.fillMaxSize()

    Canvas(canvasModifier) {
        fun SchemeCoordinates.toOffset(): Offset = Offset(
            (canvasSize.width / 2 + (x.dp - viewPoint.focus.x.dp) * viewPoint.scale).toPx(),
            (canvasSize.height / 2 + (viewPoint.focus.y.dp - y.dp) * viewPoint.scale).toPx()
        )


        fun DrawScope.drawFeature(scale: Float, feature: SchemeFeature) {
            when (feature) {
                is SchemeBackgroundFeature -> {
                    val offset = SchemeCoordinates(feature.position.left, feature.position.top).toOffset()

                    val backgroundSize = DpSize(
                        (feature.position.width * scale).dp,
                        (feature.position.height * scale).dp
                    ).toSize()

                    translate(offset.x, offset.y) {
                        with(feature.painter) {
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
                is SchemeBitmapFeature -> drawImage(feature.image, feature.position.toOffset())
                is SchemeImageFeature -> {
                    val offset = feature.position.toOffset()
                    val imageSize = feature.size.toSize()
                    translate(offset.x - imageSize.width / 2, offset.y - imageSize.height / 2) {
                        with(feature.painter) {
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
            features.values.filterIsInstance<SchemeBackgroundFeature>().forEach { background ->
                drawFeature(viewPoint.scale, background)
            }
            features.values.filter {
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

@Composable
fun SchemeView(
    initialViewPoint: SchemeViewPoint,
    features: Map<FeatureId, SchemeFeature> = emptyMap(),
    config: SchemeViewConfig = SchemeViewConfig(),
    modifier: Modifier = Modifier.fillMaxSize(),
    buildFeatures: @Composable (FeatureBuilder.() -> Unit) = {},
) {
    val featuresBuilder = SchemeFeatureBuilder(features)
    featuresBuilder.buildFeatures()
    SchemeView(
        { initialViewPoint },
        featuresBuilder.build(),
        config,
        modifier
    )
}
