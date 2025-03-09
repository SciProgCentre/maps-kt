package space.kscience.maps.features

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.PointerMatcher
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.PointerKeyboardModifiers
import space.kscience.attributes.*

public object ZAttribute : Attribute<Float>

public val Feature<*>.z: Float
    get() = attributes[ZAttribute] ?: 0f

public object DraggableAttribute : Attribute<DragHandle<Any>>

public object DragListenerAttribute : SetAttribute<DragListener<Any>>

/**
 * Click radius for point-like and line objects
 */
public object ClickRadius : Attribute<Float>

public object ClickListenerAttribute : SetAttribute<MouseListener<Any>>

public object HoverListenerAttribute : SetAttribute<MouseListener<Any>>

//public object TapListenerAttribute : SetAttribute<TapListener<Any>>

public object VisibleAttribute : Attribute<Boolean>

public object ColorAttribute : Attribute<Color>


public fun <T : Any, F : Feature<T>> FeatureRef<T, F>.color(color: Color): FeatureRef<T, F> =
    modifyAttribute(ColorAttribute, color)


public object ZoomRangeAttribute : Attribute<FloatRange>


public fun <T : Any, F : Feature<T>> FeatureRef<T, F>.zoomRange(range: FloatRange): FeatureRef<T, F> =
    modifyAttribute(ZoomRangeAttribute, range)


public object AlphaAttribute : Attribute<Float>

public fun <T : Any, F : Feature<T>> FeatureRef<T, F>.alpha(alpha: Float): FeatureRef<T, F> {
    require(alpha in 0f..1f) { "Alpha value must be between 0 and 1" }
    return modifyAttribute(AlphaAttribute, alpha)
}

public fun <T : Any, F : Feature<T>> FeatureRef<T, F>.modifyAttributes(
    modification: AttributesBuilder<F>.() -> Unit,
): FeatureRef<T, F> {
    @Suppress("UNCHECKED_CAST")
    store.feature(
        id,
        resolve().withAttributes { modified(modification) } as F
    )
    return this
}

public fun <T : Any, F : Feature<T>, V> FeatureRef<T, F>.modifyAttribute(
    key: Attribute<V>,
    value: V,
): FeatureRef<T, F> {
    @Suppress("UNCHECKED_CAST")
    store.feature(id, resolve().withAttributes { withAttribute(key, value) } as F)
    return this
}

/**
 * Add drag to this feature
 *
 * @param constraint optional drag constraint
 */
@Suppress("UNCHECKED_CAST")
public fun <T : Any, F : DraggableFeature<T>> FeatureRef<T, F>.draggable(
    constraint: ((T) -> T)? = null,
    listener: (PointerEvent.(from: ViewPoint<T>, to: ViewPoint<T>) -> Unit)? = null,
): FeatureRef<T, F> = with(store) {
    if (attributes[DraggableAttribute] == null) {
        val handle = DragHandle.withPrimaryButton<Any> { event, start, end ->
            val feature = features[id] as? DraggableFeature<T> ?: return@withPrimaryButton DragResult(end)
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
        modifyAttribute(DraggableAttribute, handle)
    }

    //Apply callback
    if (listener != null) {
        onDrag(listener)
    }
    return this@draggable
}


@Suppress("UNCHECKED_CAST")
public fun <T : Any, F : DraggableFeature<T>> FeatureRef<T, F>.onDrag(
    listener: PointerEvent.(from: ViewPoint<T>, to: ViewPoint<T>) -> Unit,
): FeatureRef<T, F> = modifyAttributes {
    DragListenerAttribute.add(
        DragListener { event, from, to -> event.listener(from as ViewPoint<T>, to as ViewPoint<T>) }
    )
}

@Suppress("UNCHECKED_CAST")
public fun <T : Any, F : DomainFeature<T>> FeatureRef<T, F>.onClick(
    onClick: PointerEvent.(click: ViewPoint<T>) -> Unit,
): FeatureRef<T, F> = modifyAttributes {
    ClickListenerAttribute.add(
        MouseListener { event, point ->
            event.onClick(point as ViewPoint<T>)
        }
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Suppress("UNCHECKED_CAST")
public fun <T : Any, F : DomainFeature<T>> FeatureRef<T, F>.onClick(
    pointerMatcher: PointerMatcher,
    keyboardModifiers: PointerKeyboardModifiers.() -> Boolean = { true },
    onClick: PointerEvent.(click: ViewPoint<T>) -> Unit,
): FeatureRef<T, F> = modifyAttributes {
    ClickListenerAttribute.add(
        MouseListener { event, point ->
            if (pointerMatcher.matches(event) && keyboardModifiers(event.keyboardModifiers)) {
                event.onClick(point as ViewPoint<T>)
            }
        }
    )
}

@Suppress("UNCHECKED_CAST")
public fun <T : Any, F : DomainFeature<T>> FeatureRef<T, F>.onHover(
    onClick: PointerEvent.(move: ViewPoint<T>) -> Unit,
): FeatureRef<T, F> = modifyAttributes {
    HoverListenerAttribute.add(
        MouseListener { event, point -> event.onClick(point as ViewPoint<T>) }
    )
}

//    @Suppress("UNCHECKED_CAST")
//    @OptIn(ExperimentalFoundationApi::class)
//    public fun <F : DomainFeature<T>> FeatureId<F>.onTap(
//        pointerMatcher: PointerMatcher = PointerMatcher.Primary,
//        keyboardFilter: PointerKeyboardModifiers.() -> Boolean = { true },
//        onTap: (point: ViewPoint<T>) -> Unit,
//    ): FeatureId<F> = modifyAttributes {
//        TapListenerAttribute.add(
//            TapListener(pointerMatcher, keyboardFilter) { point -> onTap(point as ViewPoint<T>) }
//        )
//    }


public object PathEffectAttribute : Attribute<PathEffect>

public fun <T : Any> FeatureRef<T, LineSegmentFeature<T>>.pathEffect(effect: PathEffect): FeatureRef<T, LineSegmentFeature<T>> =
    modifyAttribute(PathEffectAttribute, effect)

public object StrokeAttribute : Attribute<Float>

public fun <T : Any, F : LineSegmentFeature<T>> FeatureRef<T, F>.stroke(width: Float): FeatureRef<T, F> =
    modifyAttribute(StrokeAttribute, width)

public fun <T : Any, F : PointsFeature<T>> FeatureRef<T, F>.pointSize(width: Float): FeatureRef<T, F> =
    modifyAttribute(StrokeAttribute, width)