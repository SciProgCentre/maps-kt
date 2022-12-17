package center.sciprog.maps.compose

import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.ui.graphics.Color

public object DraggableAttribute : MapFeature.Attribute<DragHandle>
public object SelectableAttribute : MapFeature.Attribute<(FeatureId<*>, SelectableMapFeature) -> Unit>
public object VisibleAttribute : MapFeature.Attribute<Boolean>

public object ColorAttribute : MapFeature.Attribute<Color>

public class AttributeMap {
    public val map: MutableMap<MapFeature.Attribute<*>, Any> = mutableStateMapOf()

    public fun <T, A : MapFeature.Attribute<T>> setAttribute(
        attribute: A,
        attrValue: T?,
    ) {
        if (attrValue == null) {
            map.remove(attribute)
        } else {
            map[attribute] = attrValue
        }
    }

    @Suppress("UNCHECKED_CAST")
    public operator fun <T> get(attribute: MapFeature.Attribute<T>): T? = map[attribute] as? T

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as AttributeMap

        if (map != other.map) return false

        return true
    }

    override fun hashCode(): Int = map.hashCode()

    override fun toString(): String = "AttributeMap(value=${map.entries})"
}