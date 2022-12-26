package center.sciprog.maps.features

import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.ui.graphics.Color

public object ZAttribute : Feature.Attribute<Float>

public object SelectableAttribute : Feature.Attribute<(FeatureId<*>, SelectableFeature<*>) -> Unit>

public object VisibleAttribute : Feature.Attribute<Boolean>

public object ColorAttribute : Feature.Attribute<Color>

public class AttributeMap {
    public val map: MutableMap<Feature.Attribute<*>, Any> = mutableStateMapOf()

    public operator fun <T, A : Feature.Attribute<T>> set(
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
    public operator fun <T> get(attribute: Feature.Attribute<T>): T? = map[attribute] as? T

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

public var Feature<*>.z: Float
    get() = attributes[ZAttribute] ?: 0f
    set(value) {
        attributes[ZAttribute] = value
    }