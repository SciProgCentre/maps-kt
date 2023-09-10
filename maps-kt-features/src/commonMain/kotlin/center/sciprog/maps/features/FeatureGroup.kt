package center.sciprog.maps.features

import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import center.sciprog.attributes.Attribute
import center.sciprog.attributes.Attributes
import space.kscience.kmath.geometry.Angle
import space.kscience.kmath.nd.*
import space.kscience.kmath.structures.Buffer

//@JvmInline
//public value class FeatureId<out F : Feature<*>>(public val id: String)

public class FeatureRef<T : Any, out F : Feature<T>>(public val id: String, public val parent: FeatureGroup<T>)

@Suppress("UNCHECKED_CAST")
public fun <T : Any, F : Feature<T>> FeatureRef<T, F>.resolve(): F =
    parent.featureMap[id]?.let { it as F } ?: error("Feature with id=$id not found")

public val <T : Any, F : Feature<T>> FeatureRef<T, F>.attributes: Attributes get() = resolve().attributes

/**
 * A group of other features
 */
public data class FeatureGroup<T : Any>(
    override val space: CoordinateSpace<T>,
    public val featureMap: SnapshotStateMap<String, Feature<T>> = mutableStateMapOf(),
) : CoordinateSpace<T> by space, Feature<T> {

    private val attributesState: MutableState<Attributes> = mutableStateOf(Attributes.EMPTY)

    override val attributes: Attributes get() = attributesState.value

//
//    @Suppress("UNCHECKED_CAST")
//    public operator fun <F : Feature<T>> get(id: FeatureId<F>): F =
//        featureMap[id.id]?.let { it as F } ?: error("Feature with id=$id not found")

    private var uidCounter = 0

    private fun generateUID(feature: Feature<T>?): String = if (feature == null) {
        "@group[${uidCounter++}]"
    } else {
        "@${feature::class.simpleName}[${uidCounter++}]"
    }

    public fun <F : Feature<T>> feature(id: String?, feature: F): FeatureRef<T, F> {
        val safeId = id ?: generateUID(feature)
        featureMap[safeId] = feature
        return FeatureRef(safeId, this)
    }

    public fun removeFeature(id: String) {
        featureMap.remove(id)
    }

//    public fun <F : Feature<T>> feature(id: FeatureId<F>, feature: F): FeatureId<F> = feature(id.id, feature)

    public val features: Collection<Feature<T>> get() = featureMap.values.sortedByDescending { it.z }


    //
//    @Suppress("UNCHECKED_CAST")
//    public fun <A> getAttribute(id: FeatureId<Feature<T>>, key: Attribute<A>): A? =
//        get(id).attributes[key]


    override fun getBoundingBox(zoom: Float): Rectangle<T>? = with(space) {
        featureMap.values.mapNotNull { it.getBoundingBox(zoom) }.wrapRectangles()
    }

    override fun withAttributes(modify: Attributes.() -> Attributes): Feature<T> {
        attributesState.value = attributes.modify()
        return this
    }


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
        ): FeatureGroup<T> = remember {
            build(coordinateSpace, builder)
        }

    }
}

/**
 * Recursively search for feature until function returns true
 */
public fun <T : Any> FeatureGroup<T>.forEachUntil(visitor: FeatureGroup<T>.(id: String, feature: Feature<T>) -> Boolean) {
    featureMap.entries.sortedByDescending { it.value.z }.forEach { (key, feature) ->
        if (feature is FeatureGroup<T>) {
            feature.forEachUntil(visitor)
        } else {
            if (!visitor(this, key, feature)) return@forEachUntil
        }
    }
}

/**
 * Recursively visit all features in this group
 */
public fun <T : Any> FeatureGroup<T>.forEach(
    visitor: FeatureGroup<T>.(id: String, feature: Feature<T>) -> Unit,
): Unit = forEachUntil { id, feature ->
    visitor(id, feature)
    true
}

/**
 * Process all features with a given attribute from the one with highest [z] to lowest
 */
public fun <T : Any, A> FeatureGroup<T>.forEachWithAttribute(
    key: Attribute<A>,
    block: FeatureGroup<T>.(id: String, feature: Feature<T>, attributeValue: A) -> Unit,
) {
    forEach { id, feature ->
        feature.attributes[key]?.let {
            block(id, feature, it)
        }
    }
}

public fun <T : Any, A> FeatureGroup<T>.forEachWithAttributeUntil(
    key: Attribute<A>,
    block: FeatureGroup<T>.(id: String, feature: Feature<T>, attributeValue: A) -> Boolean,
) {
    forEachUntil { id, feature ->
        feature.attributes[key]?.let {
            block(id, feature, it)
        } ?: true
    }
}

public inline fun <T : Any, reified F : Feature<T>> FeatureGroup<T>.forEachWithType(
    crossinline block: (FeatureRef<T, F>) -> Unit,
) {
    forEach { id, feature ->
        if (feature is F) block(FeatureRef(id, this))
    }
}

public inline fun <T : Any, reified F : Feature<T>> FeatureGroup<T>.forEachWithTypeUntil(
    crossinline block: (FeatureRef<T, F>) -> Boolean,
) {
    forEachUntil { id, feature ->
        if (feature is F) block(FeatureRef(id, this)) else true
    }
}

public fun <T : Any> FeatureGroup<T>.circle(
    center: T,
    size: Dp = 5.dp,
    attributes: Attributes = Attributes.EMPTY,
    id: String? = null,
): FeatureRef<T, CircleFeature<T>> = feature(
    id, CircleFeature(space, center, size, attributes)
)

public fun <T : Any> FeatureGroup<T>.rectangle(
    centerCoordinates: T,
    size: DpSize = DpSize(5.dp, 5.dp),
    attributes: Attributes = Attributes.EMPTY,
    id: String? = null,
): FeatureRef<T, RectangleFeature<T>> = feature(
    id, RectangleFeature(space, centerCoordinates, size, attributes)
)

public fun <T : Any> FeatureGroup<T>.draw(
    position: T,
    attributes: Attributes = Attributes.EMPTY,
    id: String? = null,
    draw: DrawScope.() -> Unit,
): FeatureRef<T, DrawFeature<T>> = feature(
    id,
    DrawFeature(space, position, drawFeature = draw, attributes = attributes)
)

public fun <T : Any> FeatureGroup<T>.line(
    aCoordinates: T,
    bCoordinates: T,
    attributes: Attributes = Attributes.EMPTY,
    id: String? = null,
): FeatureRef<T, LineFeature<T>> = feature(
    id,
    LineFeature(space, aCoordinates, bCoordinates, attributes)
)

public fun <T : Any> FeatureGroup<T>.arc(
    oval: Rectangle<T>,
    startAngle: Angle,
    arcLength: Angle,
    attributes: Attributes = Attributes.EMPTY,
    id: String? = null,
): FeatureRef<T, ArcFeature<T>> = feature(
    id,
    ArcFeature(space, oval, startAngle, arcLength, attributes)
)

public fun <T : Any> FeatureGroup<T>.points(
    points: List<T>,
    attributes: Attributes = Attributes.EMPTY,
    id: String? = null,
): FeatureRef<T, PointsFeature<T>> = feature(
    id,
    PointsFeature(space, points, attributes)
)

public fun <T : Any> FeatureGroup<T>.multiLine(
    points: List<T>,
    attributes: Attributes = Attributes.EMPTY,
    id: String? = null,
): FeatureRef<T, MultiLineFeature<T>> = feature(
    id,
    MultiLineFeature(space, points, attributes)
)

public fun <T : Any> FeatureGroup<T>.polygon(
    points: List<T>,
    attributes: Attributes = Attributes.EMPTY,
    id: String? = null,
): FeatureRef<T, PolygonFeature<T>> = feature(
    id,
    PolygonFeature(space, points, attributes)
)

public fun <T : Any> FeatureGroup<T>.icon(
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

public fun <T : Any> FeatureGroup<T>.group(
    id: String? = null,
    builder: FeatureGroup<T>.() -> Unit,
): FeatureRef<T, FeatureGroup<T>> {
    val collection = FeatureGroup(space).apply(builder)
    val feature = FeatureGroup(space, collection.featureMap)
    return feature(id, feature)
}

public fun <T : Any> FeatureGroup<T>.scalableImage(
    box: Rectangle<T>,
    attributes: Attributes = Attributes.EMPTY,
    id: String? = null,
    painter: @Composable () -> Painter,
): FeatureRef<T, ScalableImageFeature<T>> = feature(
    id,
    ScalableImageFeature<T>(space, box, painter = painter, attributes = attributes)
)

public fun <T : Any> FeatureGroup<T>.text(
    position: T,
    text: String,
    font: FeatureFont.() -> Unit = { size = 16f },
    attributes: Attributes = Attributes.EMPTY,
    id: String? = null,
): FeatureRef<T, TextFeature<T>> = feature(
    id,
    TextFeature(space, position, text, fontConfig = font, attributes = attributes)
)

public fun <T> StructureND(shape: ShapeND, initializer: (IntArray) -> T): StructureND<T> {
    val strides = Strides(shape)
    return BufferND(strides, Buffer.boxing(strides.linearSize) { initializer(strides.index(it)) })
}

public fun <T> Structure2D(rows: Int, columns: Int, initializer: (IntArray) -> T): Structure2D<T> {
    val strides = Strides(ShapeND(rows, columns))
    return BufferND(strides, Buffer.boxing(strides.linearSize) { initializer(strides.index(it)) }).as2D()
}

public fun <T : Any> FeatureGroup<T>.pixelMap(
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
        group.featureMap.forEach { (id, feature) ->
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