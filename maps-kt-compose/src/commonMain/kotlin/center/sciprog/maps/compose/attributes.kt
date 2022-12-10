package center.sciprog.maps.compose

import androidx.compose.ui.graphics.Color

public object DraggableAttribute : MapFeature.Attribute<DragHandle>
public object SelectableAttribute : MapFeature.Attribute<(FeatureId<*>, SelectableMapFeature) -> Unit>
public object VisibleAttribute : MapFeature.Attribute<Boolean>

public object ColorAttribute: MapFeature.Attribute<Color>

@JvmInline
public value class AttributeMap internal constructor(private val map: Map<MapFeature.Attribute<*>, *>) {

    public fun <T, A : MapFeature.Attribute<T>> withAttribute(
        attribute: A,
        value: T,
    ): AttributeMap = AttributeMap(map + (attribute to value))

    @Suppress("UNCHECKED_CAST")
    public operator fun <T> get(attribute: MapFeature.Attribute<T>): T? = map[attribute] as? T
}

public fun AttributeMap(): AttributeMap = AttributeMap(emptyMap<MapFeature.Attribute<*>, Any?>())