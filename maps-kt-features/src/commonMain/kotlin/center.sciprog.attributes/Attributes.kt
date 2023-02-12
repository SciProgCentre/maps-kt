package center.sciprog.attributes

import center.sciprog.maps.features.Feature
import center.sciprog.maps.features.ZAttribute
import kotlin.jvm.JvmInline

@JvmInline
public value class Attributes internal constructor(public val content: Map<out Attribute<*>, Any>) {

    public val keys: Set<Attribute<*>> get() = content.keys

    @Suppress("UNCHECKED_CAST")
    public operator fun <T> get(attribute: Attribute<T>): T? = content[attribute] as? T

    override fun toString(): String = "Attributes(value=${content.entries})"

    public companion object {
        public val EMPTY: Attributes = Attributes(emptyMap())
    }
}

public fun Attributes.isEmpty(): Boolean = content.isEmpty()

public fun <T> Attributes.getOrDefault(attribute: AttributeWithDefault<T>): T = get(attribute) ?: attribute.default

public fun <T, A : Attribute<T>> Attributes.withAttribute(
    attribute: A,
    attrValue: T?,
): Attributes = Attributes(
    if (attrValue == null) {
        content - attribute
    } else {
        content + (attribute to attrValue)
    }
)

/**
 * Add an element to a [SetAttribute]
 */
public fun <T, A : SetAttribute<T>> Attributes.withAttributeElement(
    attribute: A,
    attrValue: T,
): Attributes {
    val currentSet: Set<T> = get(attribute) ?: emptySet()
    return Attributes(
        content + (attribute to (currentSet + attrValue))
    )
}

/**
 * Remove an element from [SetAttribute]
 */
public fun <T, A : SetAttribute<T>> Attributes.withoutAttributeElement(
    attribute: A,
    attrValue: T,
): Attributes {
    val currentSet: Set<T> = get(attribute) ?: emptySet()
    return Attributes(
        content + (attribute to (currentSet - attrValue))
    )
}

public fun <T : Any, A : Attribute<T>> Attributes(
    attribute: A,
    attrValue: T,
): Attributes = Attributes(mapOf(attribute to attrValue))

public operator fun Attributes.plus(other: Attributes): Attributes = Attributes(content + other.content)

public val Feature<*>.z: Float
    get() = attributes[ZAttribute] ?: 0f
//    set(value) {
//        attributes[ZAttribute] = value
//    }