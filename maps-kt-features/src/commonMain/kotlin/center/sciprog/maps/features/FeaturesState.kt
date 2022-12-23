package center.sciprog.maps.features

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PointMode
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import center.sciprog.maps.features.Feature.Companion.defaultZoomRange
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

@JvmInline
public value class FeatureId<out MapFeature>(public val id: String)

public class FeaturesState<T : Any>(public val coordinateSpace: CoordinateSpace<T>) :
    CoordinateSpace<T> by coordinateSpace {

    @PublishedApi
    internal val featureMap: MutableMap<String, Feature<T>> = mutableStateMapOf()

    public val features: Map<FeatureId<*>, Feature<T>>
        get() = featureMap.mapKeys { FeatureId<Feature<T>>(it.key) }

    @Suppress("UNCHECKED_CAST")
    public fun <F : Feature<T>> getFeature(id: FeatureId<F>): F = featureMap[id.id] as F

    private fun generateID(feature: Feature<T>): String = "@feature[${feature.hashCode().toUInt()}]"

    public fun <F : Feature<T>> feature(id: String?, feature: F): FeatureId<F> {
        val safeId = id ?: generateID(feature)
        featureMap[safeId] = feature
        return FeatureId(safeId)
    }

    public fun <F : Feature<T>> feature(id: FeatureId<F>?, feature: F): FeatureId<F> = feature(id?.id, feature)


    public fun <F : Feature<T>, V> setAttribute(id: FeatureId<F>, key: Feature.Attribute<V>, value: V?) {
        getFeature(id).attributes.setAttribute(key, value)
    }

    //TODO use context receiver for that
    public fun FeatureId<DraggableFeature<T>>.draggable(
        //TODO add constraints
        callback: DragHandle<T> = DragHandle.bypass(),
    ) {
        val handle = DragHandle.withPrimaryButton<T> { event, start, end ->
            val feature = featureMap[id] as? DraggableFeature ?: return@withPrimaryButton true
            val boundingBox = feature.getBoundingBox(start.zoom) ?: return@withPrimaryButton true
            if (start.focus in boundingBox) {
                feature(id, feature.withCoordinates(end.focus))
                callback.handle(event, start, end)
                false
            } else {
                true
            }
        }
        setAttribute(this, DraggableAttribute, handle)
    }

    /**
     * Cyclic update of a feature. Called infinitely until canceled.
     */
    public fun <F : Feature<T>> FeatureId<F>.updated(
        scope: CoroutineScope,
        update: suspend (F) -> F,
    ): Job = scope.launch {
        while (isActive) {
            feature(this@updated, update(getFeature(this@updated)))
        }
    }

    @Suppress("UNCHECKED_CAST")
    public fun <F : SelectableFeature<T>> FeatureId<F>.selectable(
        onSelect: (FeatureId<F>, F) -> Unit,
    ) {
        setAttribute(this, SelectableAttribute) { id, feature -> onSelect(id as FeatureId<F>, feature as F) }
    }


    @Suppress("UNCHECKED_CAST")
    public fun <A> getAttribute(id: FeatureId<Feature<T>>, key: Feature.Attribute<A>): A? =
        getFeature(id).attributes[key]


//    @Suppress("UNCHECKED_CAST")
//    public fun <T> findAllWithAttribute(key: Attribute<T>, condition: (T) -> Boolean): Set<FeatureId> {
//        return attributes.filterValues {
//            condition(it[key] as T)
//        }.keys
//    }

    public inline fun <A> forEachWithAttribute(
        key: Feature.Attribute<A>,
        block: (id: FeatureId<*>, attributeValue: A) -> Unit,
    ) {
        featureMap.forEach { (id, feature) ->
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
            builder: FeaturesState<T>.() -> Unit = {},
        ): FeaturesState<T> = FeaturesState(coordinateSpace).apply(builder)

        /**
         * Build and remember map feature state
         */
        @Composable
        public fun <T : Any> remember(
            coordinateSpace: CoordinateSpace<T>,
            builder: FeaturesState<T>.() -> Unit = {},
        ): FeaturesState<T> = remember(builder) {
            build(coordinateSpace, builder)
        }

    }
}

public fun <T : Any> FeaturesState<T>.circle(
    center: T,
    zoomRange: DoubleRange = defaultZoomRange,
    size: Dp = 5.dp,
    color: Color = Color.Red,
    id: String? = null,
): FeatureId<CircleFeature<T>> = feature(
    id, CircleFeature(coordinateSpace, center, zoomRange, size, color)
)

public fun <T : Any> FeaturesState<T>.rectangle(
    centerCoordinates: T,
    zoomRange: DoubleRange = defaultZoomRange,
    size: DpSize = DpSize(5.dp, 5.dp),
    color: Color = Color.Red,
    id: String? = null,
): FeatureId<RectangleFeature<T>> = feature(
    id, RectangleFeature(coordinateSpace, centerCoordinates, zoomRange, size, color)
)

public fun <T : Any> FeaturesState<T>.line(
    aCoordinates: T,
    bCoordinates: T,
    zoomRange: DoubleRange = defaultZoomRange,
    color: Color = Color.Red,
    id: String? = null,
): FeatureId<LineFeature<T>> = feature(
    id,
    LineFeature(coordinateSpace, aCoordinates, bCoordinates, zoomRange, color)
)

public fun <T : Any> FeaturesState<T>.arc(
    oval: Rectangle<T>,
    startAngle: Float,
    arcLength: Float,
    zoomRange: DoubleRange = defaultZoomRange,
    color: Color = Color.Red,
    id: String? = null,
): FeatureId<ArcFeature<T>> = feature(
    id,
    ArcFeature(coordinateSpace, oval, startAngle, arcLength, zoomRange, color)
)

public fun <T : Any> FeaturesState<T>.points(
    points: List<T>,
    zoomRange: DoubleRange = defaultZoomRange,
    stroke: Float = 2f,
    color: Color = Color.Red,
    pointMode: PointMode = PointMode.Points,
    id: String? = null,
): FeatureId<PointsFeature<T>> =
    feature(id, PointsFeature(coordinateSpace, points, zoomRange, stroke, color, pointMode))

public fun <T : Any> FeaturesState<T>.image(
    position: T,
    image: ImageVector,
    zoomRange: DoubleRange = defaultZoomRange,
    id: String? = null,
): FeatureId<VectorImageFeature<T>> =
    feature(
        id,
        VectorImageFeature(
            coordinateSpace,
            position,
            DpSize(image.defaultWidth, image.defaultHeight),
            image,
            zoomRange
        )
    )

public fun <T : Any> FeaturesState<T>.group(
    zoomRange: DoubleRange = defaultZoomRange,
    id: String? = null,
    builder: FeaturesState<T>.() -> Unit,
): FeatureId<FeatureGroup<T>> {
    val map = FeaturesState(coordinateSpace).apply(builder).features
    val feature = FeatureGroup(coordinateSpace, map, zoomRange)
    return feature(id, feature)
}

public fun <T : Any> FeaturesState<T>.text(
    position: T,
    text: String,
    zoomRange: DoubleRange = defaultZoomRange,
    color: Color = Color.Red,
    font: FeatureFont.() -> Unit = { size = 16f },
    id: String? = null,
): FeatureId<TextFeature<T>> = feature(
    id,
    TextFeature(coordinateSpace, position, text, zoomRange, color, fontConfig = font)
)
