package center.sciprog.maps.features

import androidx.compose.runtime.Stable
import kotlin.jvm.JvmInline

@Stable
@JvmInline
public value class Attributes internal constructor(internal val map: Map<Attribute<*>, Any>) {
    @Suppress("UNCHECKED_CAST")
    public operator fun <T> get(attribute: Attribute<T>): T? = map[attribute] as? T

    public fun <T> Attribute<T>.invoke(value: T?): Attributes = withAttribute(this, value)

    override fun toString(): String = "AttributeMap(value=${map.entries})"

    public companion object {
        public val EMPTY: Attributes = Attributes(emptyMap())
    }
}

public fun <T, A : Attribute<T>> Attributes.withAttribute(
    attribute: A,
    attrValue: T?,
): Attributes = Attributes(
    if (attrValue == null) {
        map - attribute
    } else {
        map + (attribute to attrValue)
    }
)

public fun <T : Any, A : Attribute<T>> Attributes(
    attribute: A,
    attrValue: T,
): Attributes = Attributes(mapOf(attribute to attrValue))

public operator fun Attributes.plus(other: Attributes): Attributes = Attributes(map + other.map)

public val Feature<*>.z: Float
    get() = attributes[ZAttribute] ?: 0f
//    set(value) {
//        attributes[ZAttribute] = value
//    }