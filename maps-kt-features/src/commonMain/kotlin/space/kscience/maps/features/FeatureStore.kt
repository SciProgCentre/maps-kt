package space.kscience.maps.features

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.benasher44.uuid.Uuid
import com.benasher44.uuid.uuid4
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.jetbrains.skia.Font
import space.kscience.attributes.Attribute
import space.kscience.attributes.Attributes
import space.kscience.kmath.geometry.Angle
import space.kscience.kmath.nd.*
import space.kscience.kmath.structures.Buffer
import space.kscience.maps.features.FeatureStore.Companion.generateFeatureId

//@JvmInline
//public value class FeatureId<out F : Feature<*>>(public val id: String)

/**
 * A reference to a feature inside a [FeatureStore]
 */
public class FeatureRef<T : Any, out F : Feature<T>> internal constructor(
    internal val store: FeatureStore<T>,
    internal val id: String,
) {
    override fun toString(): String = "FeatureRef($id)"
}

@Suppress("UNCHECKED_CAST")
public fun <T : Any, F : Feature<T>> FeatureRef<T, F>.resolve(): F =
    store.features[id]?.let { it as F } ?: error("Feature with ref $this not found")

public val <T : Any, F : Feature<T>> FeatureRef<T, F>.attributes: Attributes get() = resolve().attributes

public fun Uuid.toIndex(): String = leastSignificantBits.toString(16)

public interface FeatureBuilder<T : Any> {
    public val space: CoordinateSpace<T>

    /**
     * Add or replace feature. If [id] is null, then a unique id is generated
     */
    public fun <F : Feature<T>> feature(id: String?, feature: F): FeatureRef<T, F>

    public fun putFeatures(features: Map<String, Feature<T>?>)

    /**
     * Update existing feature if it is present and is of type [F]
     */
    public fun <F : Feature<T>> updateFeature(id: String, block: (F?) -> F): FeatureRef<T, F>

    public fun group(
        id: String? = null,
        attributes: Attributes = Attributes.EMPTY,
        builder: FeatureGroup<T>.() -> Unit,
    ): FeatureRef<T, FeatureGroup<T>>

    public fun removeFeature(id: String)
}

public interface FeatureSet<T : Any> {
    public val features: Map<String, Feature<T>>

    /**
     * Create a reference
     */
    public fun <F : Feature<T>> ref(id: String): FeatureRef<T, F>
}


public class FeatureStore<T : Any>(
    override val space: CoordinateSpace<T>,
) : CoordinateSpace<T> by space, FeatureBuilder<T>, FeatureSet<T> {
    private val _featureFlow = MutableStateFlow<Map<String, Feature<T>>>(emptyMap())

    public val featureFlow: StateFlow<Map<String, Feature<T>>> get() = _featureFlow

    override val features: Map<String, Feature<T>> get() = featureFlow.value

    override fun <F : Feature<T>> feature(id: String?, feature: F): FeatureRef<T, F> {
        val safeId = id ?: generateFeatureId(feature)
        _featureFlow.value += (safeId to feature)
        return FeatureRef(this, safeId)
    }

    public override fun putFeatures(features: Map<String, Feature<T>?>) {
        _featureFlow.value = _featureFlow.value.toMutableMap().apply {
            features.forEach { (key, value) ->
                if (value == null) {
                    remove(key)
                } else {
                    put(key, value)
                }
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    override fun <F : Feature<T>> updateFeature(id: String, block: (F?) -> F): FeatureRef<T, F> =
        feature(id, block(features[id] as? F))

    override fun group(
        id: String?,
        attributes: Attributes,
        builder: FeatureGroup<T>.() -> Unit,
    ): FeatureRef<T, FeatureGroup<T>> {
        val safeId: String = id ?: generateFeatureId<FeatureGroup<*>>()
        return feature(safeId, FeatureGroup(this, safeId, attributes).apply(builder))
    }

    override fun removeFeature(id: String) {
        _featureFlow.value -= id
    }

    override fun <F : Feature<T>> ref(id: String): FeatureRef<T, F> = FeatureRef(this, id)

    public fun getBoundingBox(zoom: Float = Float.MAX_VALUE): Rectangle<T>? = with(space) {
        features.values.mapNotNull { it.getBoundingBox(zoom) }.wrapRectangles()
    }


    public companion object {

        internal fun generateFeatureId(prefix: String): String =
            "$prefix[${uuid4().toIndex()}]"

        internal fun generateFeatureId(feature: Feature<*>): String =
            generateFeatureId(feature::class.simpleName ?: "undefined")

        internal inline fun <reified F : Feature<*>> generateFeatureId(): String =
            generateFeatureId(F::class.simpleName ?: "undefined")

        /**
         * Build, but do not remember map feature state
         */
        public fun <T : Any> build(
            coordinateSpace: CoordinateSpace<T>,
            builder: FeatureStore<T>.() -> Unit = {},
        ): FeatureStore<T> = FeatureStore(coordinateSpace).apply(builder)

        /**
         * Build and remember map feature state
         */
        @Composable
        public fun <T : Any> remember(
            coordinateSpace: CoordinateSpace<T>,
            builder: FeatureStore<T>.() -> Unit = {},
        ): FeatureStore<T> = remember {
            build(coordinateSpace, builder)
        }
    }
}

/**
 * A group of other features
 */
public data class FeatureGroup<T : Any> internal constructor(
    val store: FeatureStore<T>,
    val groupId: String,
    override val attributes: Attributes,
) : CoordinateSpace<T> by store.space, Feature<T>, FeatureBuilder<T>, FeatureSet<T> {

    override val space: CoordinateSpace<T> get() = store.space

    override fun withAttributes(modify: Attributes.() -> Attributes): FeatureGroup<T> =
        FeatureGroup(store, groupId, modify(attributes))

    override fun <F : Feature<T>> feature(id: String?, feature: F): FeatureRef<T, F> =
        store.feature("$groupId/${id ?: generateFeatureId(feature)}", feature)

    public override fun putFeatures(features: Map<String, Feature<T>?>) {
        store.putFeatures(features.mapKeys { "$groupId/${it.key}" })
    }

    override fun <F : Feature<T>> updateFeature(id: String, block: (F?) -> F): FeatureRef<T, F> =
        store.updateFeature("$groupId/$id", block)


    override fun group(
        id: String?,
        attributes: Attributes,
        builder: FeatureGroup<T>.() -> Unit,
    ): FeatureRef<T, FeatureGroup<T>> {
        val safeId = id ?: generateFeatureId<FeatureGroup<*>>()
        return feature(safeId, FeatureGroup(store, "$groupId/$safeId", attributes).apply(builder))
    }

    override fun removeFeature(id: String) {
        store.removeFeature("$groupId/$id")
    }

    override val features: Map<String, Feature<T>>
        get() = store.featureFlow.value
            .filterKeys { it.startsWith("$groupId/") }
            .mapKeys { it.key.removePrefix("$groupId/") }
            .toMap()

    override fun getBoundingBox(zoom: Float): Rectangle<T>? = with(space) {
        features.values.mapNotNull { it.getBoundingBox(zoom) }.wrapRectangles()
    }

    override fun <F : Feature<T>> ref(id: String): FeatureRef<T, F> = FeatureRef(store, "$groupId/$id")
}

/**
 * Recursively search for feature until function returns true
 */
public fun <T : Any> FeatureSet<T>.forEachUntil(block: FeatureSet<T>.(ref: FeatureRef<T, *>, feature: Feature<T>) -> Boolean) {
    features.entries.sortedByDescending { it.value.z }.forEach { (key, feature) ->
        if (!block(ref<Feature<T>>(key), feature)) return@forEachUntil
    }
}

/**
 * Process all features with a given attribute from the one with highest [z] to lowest
 */
public inline fun <T : Any, A> FeatureSet<T>.forEachWithAttribute(
    key: Attribute<A>,
    block: FeatureSet<T>.(ref: FeatureRef<T, *>, feature: Feature<T>, attribute: A) -> Unit,
) {
    features.forEach { (id, feature) ->
        feature.attributes[key]?.let {
            block(ref<Feature<T>>(id), feature, it)
        }
    }
}

public inline fun <T : Any, A> FeatureSet<T>.forEachWithAttributeUntil(
    key: Attribute<A>,
    block: FeatureSet<T>.(ref: FeatureRef<T, *>, feature: Feature<T>, attribute: A) -> Boolean,
) {
    features.forEach { (id, feature) ->
        feature.attributes[key]?.let {
            if (!block(ref<Feature<T>>(id), feature, it)) return@forEachWithAttributeUntil
        }
    }
}

public inline fun <T : Any, reified F : Feature<T>> FeatureSet<T>.forEachWithType(
    crossinline block: FeatureSet<T>.(ref: FeatureRef<T, F>, feature: F) -> Unit,
) {
    features.forEach { (id, feature) ->
        if (feature is F) block(ref(id), feature)
    }
}

public fun <T : Any> FeatureBuilder<T>.circle(
    center: T,
    size: Dp = 5.dp,
    attributes: Attributes = Attributes.EMPTY,
    id: String? = null,
): FeatureRef<T, CircleFeature<T>> = feature(
    id, CircleFeature(space, center, size, attributes)
)

public fun <T : Any> FeatureBuilder<T>.rectangle(
    centerCoordinates: T,
    size: DpSize = DpSize(5.dp, 5.dp),
    attributes: Attributes = Attributes.EMPTY,
    id: String? = null,
): FeatureRef<T, RectangleFeature<T>> = feature(
    id, RectangleFeature(space, centerCoordinates, size, attributes)
)

public fun <T : Any> FeatureBuilder<T>.draw(
    position: T,
    attributes: Attributes = Attributes.EMPTY,
    id: String? = null,
    draw: DrawScope.() -> Unit,
): FeatureRef<T, DrawFeature<T>> = feature(
    id,
    DrawFeature(space, position, drawFeature = draw, attributes = attributes)
)

public fun <T : Any> FeatureBuilder<T>.line(
    aCoordinates: T,
    bCoordinates: T,
    attributes: Attributes = Attributes.EMPTY,
    id: String? = null,
): FeatureRef<T, LineFeature<T>> = feature(
    id,
    LineFeature(space, aCoordinates, bCoordinates, attributes)
)

public fun <T : Any> FeatureBuilder<T>.arc(
    oval: Rectangle<T>,
    startAngle: Angle,
    arcLength: Angle,
    attributes: Attributes = Attributes.EMPTY,
    id: String? = null,
): FeatureRef<T, ArcFeature<T>> = feature(
    id,
    ArcFeature(space, oval, startAngle, arcLength, attributes)
)

public fun <T : Any> FeatureBuilder<T>.points(
    points: List<T>,
    attributes: Attributes = Attributes.EMPTY,
    id: String? = null,
): FeatureRef<T, PointsFeature<T>> = feature(
    id,
    PointsFeature(space, points, attributes)
)

public fun <T : Any> FeatureBuilder<T>.multiLine(
    points: List<T>,
    attributes: Attributes = Attributes.EMPTY,
    id: String? = null,
): FeatureRef<T, MultiLineFeature<T>> = feature(
    id,
    MultiLineFeature(space, points, attributes)
)

public fun <T : Any> FeatureBuilder<T>.polygon(
    points: List<T>,
    attributes: Attributes = Attributes.EMPTY,
    id: String? = null,
): FeatureRef<T, PolygonFeature<T>> = feature(
    id,
    PolygonFeature(space, points, attributes)
)

public fun <T : Any> FeatureBuilder<T>.icon(
    position: T,
    image: ImageVector,
    size: DpSize = DpSize(image.defaultWidth, image.defaultHeight),
    attributes: Attributes = Attributes.EMPTY,
    id: String? = null,
): FeatureRef<T, VectorIconFeature<T>> = feature(
    id,
    VectorIconFeature(
        space,
        position,
        size,
        image,
        attributes
    )
)

public fun <T : Any> FeatureBuilder<T>.scalableImage(
    box: Rectangle<T>,
    attributes: Attributes = Attributes.EMPTY,
    id: String? = null,
    painter: @Composable () -> Painter,
): FeatureRef<T, ScalableImageFeature<T>> = feature(
    id,
    ScalableImageFeature<T>(space, box, painter = painter, attributes = attributes)
)

public fun <T : Any> FeatureBuilder<T>.text(
    position: T,
    text: String,
    font: Font.() -> Unit = { size = 16f },
    attributes: Attributes = Attributes.EMPTY,
    id: String? = null,
): FeatureRef<T, TextFeature<T>> = feature(
    id,
    TextFeature(space, position, text, fontConfig = font, attributes = attributes)
)

//public fun <T> StructureND(shape: ShapeND, initializer: (IntArray) -> T): StructureND<T> {
//    val strides = Strides(shape)
//    return BufferND(strides, Buffer(strides.linearSize) { initializer(strides.index(it)) })
//}

public inline fun <reified T> Structure2D(rows: Int, columns: Int, initializer: (IntArray) -> T): Structure2D<T> {
    val strides = Strides(ShapeND(rows, columns))
    return BufferND(strides, Buffer(strides.linearSize) { initializer(strides.index(it)) }).as2D()
}

public fun <T : Any> FeatureStore<T>.pixelMap(
    rectangle: Rectangle<T>,
    pixelMap: Structure2D<Color?>,
    attributes: Attributes = Attributes.EMPTY,
    id: String? = null,
): FeatureRef<T, PixelMapFeature<T>> = feature(
    id,
    PixelMapFeature(space, rectangle, pixelMap, attributes = attributes)
)

/**
 * Create a pretty tree-like representation of this feature group
 */
public fun FeatureGroup<*>.toPrettyString(): String {
    fun StringBuilder.printGroup(id: String, group: FeatureGroup<*>, prefix: String) {
        appendLine("${prefix}* [group] $id")
        group.features.forEach { (id, feature) ->
            if (feature is FeatureGroup<*>) {
                printGroup(id, feature, "  ")
            } else {
                appendLine("$prefix  * [${feature::class.simpleName}] $id ")
            }
        }
    }
    return buildString {
        printGroup("root", this@toPrettyString, "")
    }
}