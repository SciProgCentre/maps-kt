package space.kscience.maps.features

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
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
import io.github.oshai.kotlinlogging.KLogger
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.sample
import space.kscience.attributes.Attributes
import space.kscience.attributes.plus
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

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

    public companion object{
        public val logger: KLogger =  KotlinLogging.logger("FeatureDrawScope")
    }
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
            //TODO don't draw text that is not on screen
            drawText(textMeasurer ?: error("Text measurer not defined"), text, position)
        } catch (ex: Exception) {
            logger.error(ex) { "Failed to measure text" }
        }
    }

    override fun painterFor(feature: PainterFeature<T>): Painter =
        painterCache[feature] ?: error("Can't resolve painter for $feature")

    public companion object {
        private val logger = KotlinLogging.logger("ComposeFeatureDrawScope")
    }
}

@Composable
public fun <T: Any> FeatureSet<T>.pointerCache(): Map<PainterFeature<T>, Painter> = key(features) {
    features.values.filterIsInstance<PainterFeature<T>>().associateWith { it.getPainter() }
}


/**
 * Create a canvas with extended functionality (e.g., drawing text)
 *
 * @param draw additional custom content drawn underneath features
 */
@OptIn(FlowPreview::class)
@Composable
public fun <T : Any> FeatureCanvas(
    state: CanvasState<T>,
    featureFlow: StateFlow<Map<String, Feature<T>>>,
    modifier: Modifier = Modifier,
    sampleDuration: Duration = 20.milliseconds,
    draw: FeatureDrawScope<T>.() -> Unit = {},
) {
    val textMeasurer = rememberTextMeasurer(0)

    val features: Map<String, Feature<T>> by featureFlow.sample(sampleDuration).collectAsState(featureFlow.value)

    val painterCache = features.values
        .filterIsInstance<PainterFeature<T>>()
        .associateWith { it.getPainter() }


    Canvas(modifier) {
        if (state.canvasSize != size.toDpSize()) {
            state.canvasSize = size.toDpSize()
        }
        clipRect {
            ComposeFeatureDrawScope(this, state, painterCache, textMeasurer).apply(draw).apply {

                val attributesCache = mutableMapOf<List<String>, Attributes>()

                fun computeGroupAttributes(path: List<String>): Attributes = attributesCache.getOrPut(path) {
                    if (path.isEmpty()) return Attributes.EMPTY
                    else if (path.size == 1) {
                        features[path.first()]?.attributes ?: Attributes.EMPTY
                    } else {
                        computeGroupAttributes(path.dropLast(1)) + (features[path.first()]?.attributes
                            ?: Attributes.EMPTY)
                    }
                }

                features.entries.sortedBy { it.value.z }
                    .filter { state.viewPoint.zoom in it.value.zoomRange }
                    .forEach { (id, feature) ->
                        val path = id.split("/")
                        drawFeature(feature, computeGroupAttributes(path.dropLast(1)))
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
