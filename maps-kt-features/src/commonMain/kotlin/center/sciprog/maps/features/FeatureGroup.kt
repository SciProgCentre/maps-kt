package center.sciprog.maps.features

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PointMode
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import kotlin.jvm.JvmInline

@JvmInline
public value class FeatureId<out F : Feature<*>>(public val id: String)

/**
 * A group of other features
 */
public data class FeatureGroup<T : Any>(
    override val space: CoordinateSpace<T>,
    public val featureMap: SnapshotStateMap<String, Feature<T>> = mutableStateMapOf(),
    override val attributes: Attributes = Attributes.EMPTY,
) : CoordinateSpace<T> by space, Feature<T> {

    @Suppress("UNCHECKED_CAST")
    public operator fun <F : Feature<T>> get(id: FeatureId<F>): F =
        featureMap[id.id]?.let { it as F } ?: error("Feature with id=$id not found")

    private var uidCounter = 0

    private fun generateUID(feature: Feature<T>?): String = if (feature == null) {
        "@group[${uidCounter++}]"
    } else {
        "@${feature::class.simpleName}[${uidCounter++}]"
    }

    public fun <F : Feature<T>> feature(id: String?, feature: F): FeatureId<F> {
        val safeId = id ?: generateUID(feature)
        featureMap[safeId] = feature
        return FeatureId(safeId)
    }

    public fun <F : Feature<T>> feature(id: FeatureId<F>, feature: F): FeatureId<F> = feature(id.id, feature)

    public val features: Collection<Feature<T>> get() = featureMap.values.sortedByDescending { it.z }

    public fun visit(visitor: FeatureGroup<T>.(id: FeatureId<Feature<T>>, feature: Feature<T>) -> Unit) {
        featureMap.forEach { (key, feature) ->
            if (feature is FeatureGroup<T>) {
                feature.visit(visitor)
            } else {
                visitor(this, FeatureId(key), feature)
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    public fun <A> getAttribute(id: FeatureId<Feature<T>>, key: Attribute<A>): A? =
        get(id).attributes[key]

    /**
     * Process all features with a given attribute from the one with highest [z] to lowest
     */
    public inline fun <A> forEachWithAttribute(
        key: Attribute<A>,
        block: (id: FeatureId<*>, feature: Feature<T>, attributeValue: A) -> Unit,
    ) {
        featureMap.entries.sortedByDescending { it.value.z }.forEach { (id, feature) ->
            feature.attributes[key]?.let {
                block(FeatureId<Feature<T>>(id), feature, it)
            }
        }
    }

    public fun <F : Feature<T>, V> FeatureId<F>.modifyAttributes(modify: Attributes.() -> Attributes) {
        feature(this, get(this).withAttributes(modify))
    }

    public fun <F : Feature<T>> FeatureId<F>.withAttributes(modify: Attributes.() -> Attributes): FeatureId<F> {
        feature(this, get(this).withAttributes(modify))
        return this
    }

    public fun <F : Feature<T>, V> FeatureId<F>.withAttribute(key: Attribute<V>, value: V?): FeatureId<F> {
        feature(this, get(this).withAttributes { withAttribute(key, value) })
        return this
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
        listener: (PointerEvent.(from: ViewPoint<T>, to: ViewPoint<T>) -> Unit)? = null,
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
            this.withAttribute(DraggableAttribute, handle)
        }

        //Apply callback
        if (listener != null) {
            onDrag(listener)
        }
    }


    @Suppress("UNCHECKED_CAST")
    public fun FeatureId<DraggableFeature<T>>.onDrag(
        listener: PointerEvent.(from: ViewPoint<T>, to: ViewPoint<T>) -> Unit,
    ) {
        withAttribute(
            DragListenerAttribute,
            (getAttribute(this, DragListenerAttribute) ?: emptySet()) +
                    DragListener { event, from, to ->
                        event.listener(from as ViewPoint<T>, to as ViewPoint<T>)
                    }
        )
    }

    @Suppress("UNCHECKED_CAST")
    public fun <F : DomainFeature<T>> FeatureId<F>.onClick(
        onClick: PointerEvent.(click: ViewPoint<T>) -> Unit,
    ) {
        withAttribute(
            ClickListenerAttribute,
            (getAttribute(this, ClickListenerAttribute) ?: emptySet()) +
                    MouseListener { event, point -> event.onClick(point as ViewPoint<T>) }
        )
    }

    @Suppress("UNCHECKED_CAST")
    public fun <F : DomainFeature<T>> FeatureId<F>.onHover(
        onClick: PointerEvent.(move: ViewPoint<T>) -> Unit,
    ) {
        withAttribute(
            HoverListenerAttribute,
            (getAttribute(this, HoverListenerAttribute) ?: emptySet()) +
                    MouseListener { event, point -> event.onClick(point as ViewPoint<T>) }
        )
    }

//    /**
//     * Cyclic update of a feature. Called infinitely until canceled.
//     */
//    public fun <F : Feature<T>> FeatureId<F>.updated(
//        scope: CoroutineScope,
//        update: suspend (F) -> F,
//    ): Job = scope.launch {
//        while (isActive) {
//            feature(this@updated, update(get(this@updated)))
//        }
//    }

    public fun <F : Feature<T>> FeatureId<F>.color(color: Color): FeatureId<F> =
        withAttribute(ColorAttribute, color)

    public fun <F : Feature<T>> FeatureId<F>.zoomRange(range: FloatRange): FeatureId<F> =
        withAttribute(ZoomRangeAttribute, range)

    override fun getBoundingBox(zoom: Float): Rectangle<T>? = with(space) {
        featureMap.values.mapNotNull { it.getBoundingBox(zoom) }.wrapRectangles()
    }

    override fun withAttributes(modify: Attributes.() -> Attributes): Feature<T> = copy(attributes = modify(attributes))

    public companion object {

        /**
         * Build, but do not remember map feature state
         */
        public fun <T : Any> build(
            coordinateSpace: CoordinateSpace<T>,
            builder: FeatureGroup<T>.() -> Unit = {},
        ): FeatureGroup<T> = FeatureGroup(coordinateSpace).apply(builder)

        /**
         * Build and remember map feature state
         */
        @Composable
        public fun <T : Any> remember(
            coordinateSpace: CoordinateSpace<T>,
            builder: FeatureGroup<T>.() -> Unit = {},
        ): FeatureGroup<T> = remember(builder) {
            build(coordinateSpace, builder)
        }

    }
}

public fun <T : Any> FeatureGroup<T>.circle(
    center: T,
    size: Dp = 5.dp,
    id: String? = null,
): FeatureId<CircleFeature<T>> = feature(
    id, CircleFeature(space, center, size)
)

public fun <T : Any> FeatureGroup<T>.rectangle(
    centerCoordinates: T,
    size: DpSize = DpSize(5.dp, 5.dp),
    id: String? = null,
): FeatureId<RectangleFeature<T>> = feature(
    id, RectangleFeature(space, centerCoordinates, size)
)

public fun <T : Any> FeatureGroup<T>.draw(
    position: T,
    id: String? = null,
    draw: DrawScope.() -> Unit,
): FeatureId<DrawFeature<T>> = feature(
    id,
    DrawFeature(space, position, drawFeature = draw)
)

public fun <T : Any> FeatureGroup<T>.line(
    aCoordinates: T,
    bCoordinates: T,
    id: String? = null,
): FeatureId<LineFeature<T>> = feature(
    id,
    LineFeature(space, aCoordinates, bCoordinates)
)

public fun <T : Any> FeatureGroup<T>.arc(
    oval: Rectangle<T>,
    startAngle: Float,
    arcLength: Float,
    id: String? = null,
): FeatureId<ArcFeature<T>> = feature(
    id,
    ArcFeature(space, oval, startAngle, arcLength)
)

public fun <T : Any> FeatureGroup<T>.points(
    points: List<T>,
    stroke: Float = 2f,
    pointMode: PointMode = PointMode.Points,
    attributes: Attributes = Attributes.EMPTY,
    id: String? = null,
): FeatureId<PointsFeature<T>> = feature(
    id,
    PointsFeature(space, points, stroke, pointMode, attributes)
)

public fun <T : Any> FeatureGroup<T>.polygon(
    points: List<T>,
    id: String? = null,
): FeatureId<PolygonFeature<T>> = feature(
    id,
    PolygonFeature(space, points)
)

public fun <T : Any> FeatureGroup<T>.image(
    position: T,
    image: ImageVector,
    size: DpSize = DpSize(image.defaultWidth, image.defaultHeight),
    id: String? = null,
): FeatureId<VectorImageFeature<T>> =
    feature(
        id,
        VectorImageFeature(
            space,
            position,
            size,
            image,
        )
    )

public fun <T : Any> FeatureGroup<T>.group(
    id: String? = null,
    builder: FeatureGroup<T>.() -> Unit,
): FeatureId<FeatureGroup<T>> {
    val collection = FeatureGroup(space).apply(builder)
    val feature = FeatureGroup(space, collection.featureMap)
    return feature(id, feature)
}

public fun <T : Any> FeatureGroup<T>.scalableImage(
    box: Rectangle<T>,
    id: String? = null,
    painter: @Composable () -> Painter,
): FeatureId<ScalableImageFeature<T>> = feature(
    id,
    ScalableImageFeature<T>(space, box, painter = painter)
)

public fun <T : Any> FeatureGroup<T>.text(
    position: T,
    text: String,
    font: FeatureFont.() -> Unit = { size = 16f },
    id: String? = null,
): FeatureId<TextFeature<T>> = feature(
    id,
    TextFeature(space, position, text, fontConfig = font)
)
