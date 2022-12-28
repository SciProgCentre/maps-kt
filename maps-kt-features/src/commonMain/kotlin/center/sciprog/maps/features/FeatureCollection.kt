package center.sciprog.maps.features

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PointMode
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.jvm.JvmInline

@JvmInline
public value class FeatureId<out F : Feature<*>>(public val id: String)

public interface FeatureBuilder<T : Any> {

    public val coordinateSpace: CoordinateSpace<T>

    public fun <F : Feature<T>> feature(id: String?, feature: F): FeatureId<F>

    public fun <F : Feature<T>, V> setAttribute(id: FeatureId<F>, key: Feature.Attribute<V>, value: V?)

    public val defaultColor: Color get() = Color.Red

    public val defaultZoomRange: FloatRange get() = 0f..Float.POSITIVE_INFINITY
}

public fun <T : Any, F : Feature<T>> FeatureBuilder<T>.feature(id: FeatureId<F>, feature: F): FeatureId<F> =
    feature(id.id, feature)

public class FeatureCollection<T : Any>(
    override val coordinateSpace: CoordinateSpace<T>,
) : CoordinateSpace<T> by coordinateSpace, FeatureBuilder<T> {

    @PublishedApi
    internal val featureMap: MutableMap<String, Feature<T>> = mutableStateMapOf()

    public val features: Map<FeatureId<*>, Feature<T>>
        get() = featureMap.mapKeys { FeatureId<Feature<T>>(it.key) }

    @Suppress("UNCHECKED_CAST")
    public operator fun <F : Feature<T>> get(id: FeatureId<F>): F =
        featureMap[id.id]?.let { it as F } ?: error("Feature with id=$id not found")

    private fun generateID(feature: Feature<T>): String = "@feature[${feature.hashCode().toUInt()}]"

    override fun <F : Feature<T>> feature(id: String?, feature: F): FeatureId<F> {
        val safeId = id ?: generateID(feature)
        featureMap[safeId] = feature
        return FeatureId(safeId)
    }

    public fun <F : Feature<T>> feature(id: FeatureId<F>, feature: F): FeatureId<F> = feature(id.id, feature)


    override fun <F : Feature<T>, V> setAttribute(id: FeatureId<F>, key: Feature.Attribute<V>, value: V?) {
        get(id).attributes[key] = value
    }

    @Suppress("UNCHECKED_CAST")
    public fun FeatureId<DraggableFeature<T>>.onDrag(
        listener: PointerEvent.(from: ViewPoint<T>, to: ViewPoint<T>) -> Unit,
    ) {
        with(get(this)) {
            attributes[DragListenerAttribute] =
                (attributes[DragListenerAttribute] ?: emptySet()) + DragListener { event, from, to ->
                    event.listener(from as ViewPoint<T>, to as ViewPoint<T>)
                }
        }
    }


    /**
     * Add drag to this feature
     *
     * @param constraint optional drag constraint
     *
     * TODO use context receiver for that
     */
    @Suppress("UNCHECKED_CAST")
    public fun FeatureId<DraggableFeature<T>>.draggable(
        constraint: ((T) -> T)? = null,
        listener: (PointerEvent.(from: ViewPoint<T>, to: ViewPoint<T>) -> Unit)? = null
    ) {
        if (getAttribute(this, DraggableAttribute) == null) {
            val handle = DragHandle.withPrimaryButton<Any> { event, start, end ->
                val feature = featureMap[id] as? DraggableFeature<T> ?: return@withPrimaryButton DragResult(end)
                start as ViewPoint<T>
                end as ViewPoint<T>
                if (start in feature) {
                    val finalPosition = constraint?.invoke(end.focus) ?: end.focus
                    feature(id, feature.withCoordinates(finalPosition))
                    feature.attributes[DragListenerAttribute]?.forEach {
                        it.handle(event, start, ViewPoint(finalPosition, end.zoom))
                    }
                    DragResult(ViewPoint(finalPosition, end.zoom), false)
                } else {
                    DragResult(end, true)
                }
            }
            setAttribute(this, DraggableAttribute, handle)
        }

        //Apply callback
        if (listener != null) {
            onDrag(listener)
        }
    }

    /**
     * Cyclic update of a feature. Called infinitely until canceled.
     */
    public fun <F : Feature<T>> FeatureId<F>.updated(
        scope: CoroutineScope,
        update: suspend (F) -> F,
    ): Job = scope.launch {
        while (isActive) {
            feature(this@updated, update(get(this@updated)))
        }
    }

    @Suppress("UNCHECKED_CAST")
    public fun <F : ClickableFeature<T>> FeatureId<F>.onClick(
        onClick: PointerEvent.(click: ViewPoint<T>) -> Unit,
    ) {
        with(get(this)) {
            attributes[ClickableListenerAttribute] =
                (attributes[ClickableListenerAttribute] ?: emptySet()) + ClickListener { event, point ->
                    event.onClick(point as ViewPoint<T>)
                }
        }
    }


    @Suppress("UNCHECKED_CAST")
    public fun <A> getAttribute(id: FeatureId<Feature<T>>, key: Feature.Attribute<A>): A? =
        get(id).attributes[key]

    /**
     * Process all features with a given attribute from the one with highest [z] to lowest
     */
    public inline fun <A> forEachWithAttribute(
        key: Feature.Attribute<A>,
        block: (id: FeatureId<*>, attributeValue: A) -> Unit,
    ) {
        featureMap.entries.sortedByDescending { it.value.z }.forEach { (id, feature) ->
            feature.attributes[key]?.let {
                block(FeatureId<Feature<T>>(id), it)
            }
        }
    }

    public companion object {

        /**
         * Build, but do not remember map feature state
         */
        public fun <T : Any> build(
            coordinateSpace: CoordinateSpace<T>,
            builder: FeatureCollection<T>.() -> Unit = {},
        ): FeatureCollection<T> = FeatureCollection(coordinateSpace).apply(builder)

        /**
         * Build and remember map feature state
         */
        @Composable
        public fun <T : Any> remember(
            coordinateSpace: CoordinateSpace<T>,
            builder: FeatureCollection<T>.() -> Unit = {},
        ): FeatureCollection<T> = remember(builder) {
            build(coordinateSpace, builder)
        }

    }
}

public fun <T : Any> FeatureBuilder<T>.circle(
    center: T,
    zoomRange: FloatRange = defaultZoomRange,
    size: Dp = 5.dp,
    color: Color = defaultColor,
    id: String? = null,
): FeatureId<CircleFeature<T>> = feature(
    id, CircleFeature(coordinateSpace, center, zoomRange, size, color)
)

public fun <T : Any> FeatureBuilder<T>.rectangle(
    centerCoordinates: T,
    zoomRange: FloatRange = defaultZoomRange,
    size: DpSize = DpSize(5.dp, 5.dp),
    color: Color = defaultColor,
    id: String? = null,
): FeatureId<RectangleFeature<T>> = feature(
    id, RectangleFeature(coordinateSpace, centerCoordinates, zoomRange, size, color)
)

public fun <T : Any> FeatureBuilder<T>.draw(
    position: T,
    zoomRange: FloatRange = defaultZoomRange,
    id: String? = null,
    draw: DrawScope.() -> Unit,
): FeatureId<DrawFeature<T>> = feature(
    id,
    DrawFeature(coordinateSpace, position, zoomRange, drawFeature = draw)
)

public fun <T : Any> FeatureBuilder<T>.line(
    aCoordinates: T,
    bCoordinates: T,
    zoomRange: FloatRange = defaultZoomRange,
    color: Color = defaultColor,
    id: String? = null,
): FeatureId<LineFeature<T>> = feature(
    id,
    LineFeature(coordinateSpace, aCoordinates, bCoordinates, zoomRange, color)
)

public fun <T : Any> FeatureBuilder<T>.arc(
    oval: Rectangle<T>,
    startAngle: Float,
    arcLength: Float,
    zoomRange: FloatRange = defaultZoomRange,
    color: Color = defaultColor,
    id: String? = null,
): FeatureId<ArcFeature<T>> = feature(
    id,
    ArcFeature(coordinateSpace, oval, startAngle, arcLength, zoomRange, color)
)

public fun <T : Any> FeatureBuilder<T>.points(
    points: List<T>,
    zoomRange: FloatRange = defaultZoomRange,
    stroke: Float = 2f,
    color: Color = defaultColor,
    pointMode: PointMode = PointMode.Points,
    id: String? = null,
): FeatureId<PointsFeature<T>> =
    feature(id, PointsFeature(coordinateSpace, points, zoomRange, stroke, color, pointMode))

public fun <T : Any> FeatureBuilder<T>.image(
    position: T,
    image: ImageVector,
    zoomRange: FloatRange = defaultZoomRange,
    size: DpSize = DpSize(image.defaultWidth, image.defaultHeight),
    id: String? = null,
): FeatureId<VectorImageFeature<T>> =
    feature(
        id,
        VectorImageFeature(
            coordinateSpace,
            position,
            size,
            image,
            zoomRange
        )
    )

public fun <T : Any> FeatureBuilder<T>.group(
    zoomRange: FloatRange = defaultZoomRange,
    id: String? = null,
    builder: FeatureCollection<T>.() -> Unit,
): FeatureId<FeatureGroup<T>> {
    val map = FeatureCollection(coordinateSpace).apply(builder).features
    val feature = FeatureGroup(coordinateSpace, map, zoomRange)
    return feature(id, feature)
}

public fun <T : Any> FeatureBuilder<T>.scalableImage(
    box: Rectangle<T>,
    zoomRange: FloatRange = defaultZoomRange,
    id: String? = null,
    painter: @Composable () -> Painter,
): FeatureId<ScalableImageFeature<T>> = feature(
    id,
    ScalableImageFeature<T>(coordinateSpace, box, zoomRange, painter = painter)
)

public fun <T : Any> FeatureBuilder<T>.text(
    position: T,
    text: String,
    zoomRange: FloatRange = defaultZoomRange,
    color: Color = defaultColor,
    font: FeatureFont.() -> Unit = { size = 16f },
    id: String? = null,
): FeatureId<TextFeature<T>> = feature(
    id,
    TextFeature(coordinateSpace, position, text, zoomRange, color, fontConfig = font)
)
