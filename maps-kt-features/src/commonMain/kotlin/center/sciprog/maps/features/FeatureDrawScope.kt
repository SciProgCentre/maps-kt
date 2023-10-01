package center.sciprog.maps.features

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.DrawScopeMarker
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.DpRect
import center.sciprog.attributes.Attributes
import io.github.oshai.kotlinlogging.KotlinLogging

/**
 * An extension of [DrawScope] to include map-specific features
 */
@DrawScopeMarker
public abstract class FeatureDrawScope<T : Any>(
    public val state: CanvasState<T>,
) : DrawScope {
    public fun Offset.toCoordinates(): T = with(state) {
        toCoordinates(this@toCoordinates, this@FeatureDrawScope)
    }

    public open fun T.toOffset(): Offset = with(state) {
        toOffset(this@toOffset, this@FeatureDrawScope)
    }

    public fun Rectangle<T>.toDpRect(): DpRect = with(state) { toDpRect() }

    public abstract fun painterFor(feature: PainterFeature<T>): Painter

    public abstract fun drawText(text: String, position: Offset, attributes: Attributes)
}

/**
 * Default implementation of FeatureDrawScope to be used in Compose (both schemes and Maps)
 */
@DrawScopeMarker
public class ComposeFeatureDrawScope<T : Any>(
    drawScope: DrawScope,
    state: CanvasState<T>,
    private val painterCache: Map<PainterFeature<T>, Painter>,
    private val textMeasurer: TextMeasurer?,
) : FeatureDrawScope<T>(state), DrawScope by drawScope {
    override fun drawText(text: String, position: Offset, attributes: Attributes) {
        try {
            drawText(textMeasurer?: error("Text measurer not defined"), text, position)
        } catch (ex: Exception) {
            logger.error(ex) { "Failed to measure text" }
        }
    }

    override fun painterFor(feature: PainterFeature<T>): Painter =
        painterCache[feature] ?: error("Can't resolve painter for $feature")

    public companion object{
        private val logger = KotlinLogging.logger("ComposeFeatureDrawScope")
    }
}


/**
 * Create a canvas with extended functionality (e.g., drawing text)
 */
@Composable
public fun <T : Any> FeatureCanvas(
    state: CanvasState<T>,
    features: FeatureGroup<T>,
    modifier: Modifier = Modifier,
    draw: FeatureDrawScope<T>.() -> Unit = {},
) {
    val textMeasurer = rememberTextMeasurer(0)

    val painterCache: Map<PainterFeature<T>, Painter> = features.features.flatMap {
        if (it is FeatureGroup) it.features else listOf(it)
    }.filterIsInstance<PainterFeature<T>>().associateWith { it.getPainter() }

    Canvas(modifier) {
        if (state.canvasSize != size.toDpSize()) {
            state.canvasSize = size.toDpSize()
        }
        ComposeFeatureDrawScope(this, state, painterCache, textMeasurer).apply(draw).apply {
            clipRect {
                features.featureMap.values.sortedBy { it.z }
                    .filter { state.viewPoint.zoom in it.zoomRange }
                    .forEach { feature ->
                        this@apply.drawFeature(feature)
                    }
            }
        }
        state.selectRect?.let { dpRect ->
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
