package center.sciprog.attributes

import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.serializer

public interface Attribute<T>

public abstract class SerializableAttribute<T>(
    public val serialId: String,
    public val serializer: KSerializer<T>,
) : Attribute<T> {
    override fun toString(): String = serialId
}

public interface AttributeWithDefault<T> : Attribute<T> {
    public val default: T
}

public interface SetAttribute<V> : Attribute<Set<V>>

public object NameAttribute : SerializableAttribute<String>("name", String.serializer())

