@file:Suppress("UNCHECKED_CAST")

package space.kscience

import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.*
import space.kscience.attributes.Attribute
import space.kscience.attributes.Attributes

public class AttributesSerializer(
    private val serializableAttributes: Set<SerializableAttribute<*>>,
) : KSerializer<Attributes> {
    private val jsonSerializer = JsonObject.serializer()
    override val descriptor: SerialDescriptor get() = jsonSerializer.descriptor

    override fun deserialize(decoder: Decoder): Attributes {
        val jsonElement = jsonSerializer.deserialize(decoder)
        val attributeMap: Map<SerializableAttribute<*>, Any> = jsonElement.entries.associate { (key, element) ->
            val attr = serializableAttributes.find { it.serialId == key }
                ?: error("Attribute serializer for key $key not found")

            val json = if (decoder is JsonDecoder) {
                decoder.json
            } else {
                Json { serializersModule = decoder.serializersModule }
            }
            val value = json.decodeFromJsonElement(attr.serializer, element) ?: error("Null values are not allowed")

            attr to value
        }
        return object : Attributes {
            override val content: Map<out Attribute<*>, Any?> = attributeMap
            override fun toString(): String = "Attributes(value=${content.entries})"
            override fun equals(other: Any?): Boolean = other is Attributes && Attributes.equals(this, other)
        }
    }

    override fun serialize(encoder: Encoder, value: Attributes) {
        val json = buildJsonObject {
            value.content.forEach { (key: Attribute<*>, value: Any?) ->
                if (key !in serializableAttributes) error("An attribute key '$key' is not in the list of allowed attributes for this serializer")
                val serializableKey = key as SerializableAttribute

                val json = if (encoder is JsonEncoder) {
                    encoder.json
                } else {
                    Json { serializersModule = encoder.serializersModule }
                }

                put(
                    serializableKey.serialId,
                    json.encodeToJsonElement(serializableKey.serializer as KSerializer<Any?>, value)
                )
            }
        }
        jsonSerializer.serialize(encoder, json)
    }
}

public abstract class SerializableAttribute<T>(
    public val serialId: String,
    public val serializer: KSerializer<T>,
) : Attribute<T> {
    override fun toString(): String = serialId
}

public object NameAttribute : SerializableAttribute<String>("name", String.serializer())

public fun Attributes.Companion.equals(a1: Attributes, a2: Attributes): Boolean =
    a1.keys == a2.keys && a1.keys.all { a1[it] == a2[it] }