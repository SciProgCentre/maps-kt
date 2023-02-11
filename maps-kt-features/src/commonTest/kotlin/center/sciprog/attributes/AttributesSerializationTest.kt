package center.sciprog.attributes

import kotlinx.serialization.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.contextual
import kotlinx.serialization.protobuf.ProtoBuf
import kotlin.test.Ignore
import kotlin.test.Test
import kotlin.test.assertEquals

internal class AttributesSerializationTest {

    @Serializable
    internal class Container(@Contextual val attributes: Attributes) {
        override fun equals(other: Any?): Boolean = (other as? Container)?.attributes?.equals(attributes) ?: false
        override fun hashCode(): Int = attributes.hashCode()

        override fun toString(): String = attributes.toString()
    }

    internal object ContainerAttribute : SerializableAttribute<Container>("container", serializer()) {
        override fun toString(): String = "container"

    }

    internal object TestAttribute : SerializableAttribute<Map<String, String>>("test", serializer()) {
        override fun toString(): String = "test"
    }

    @Test
    fun restoreFromJson() {
        val json = Json {
            serializersModule = SerializersModule {
                contextual(AttributesSerializer(setOf(NameAttribute, TestAttribute, ContainerAttribute)))
            }
        }

        val attributes = Attributes {
            NameAttribute("myTest")
            TestAttribute(mapOf("a" to "aa", "b" to "bb"))
            ContainerAttribute(
                Container(
                    Attributes {
                        TestAttribute(mapOf("a" to "aa", "b" to "bb"))
                    }
                )
            )
        }

        val serialized: String = json.encodeToString(attributes)
        println(serialized)

        val restored: Attributes = json.decodeFromString(serialized)

        assertEquals(attributes, restored)
    }

    @OptIn(ExperimentalSerializationApi::class)
    @Test
    @Ignore
    fun restoreFromProtoBuf() {
        val protoBuf = ProtoBuf {
            serializersModule = SerializersModule {
                contextual(AttributesSerializer(setOf(NameAttribute, TestAttribute, ContainerAttribute)))
            }
        }

        val attributes = Attributes {
            NameAttribute("myTest")
            TestAttribute(mapOf("a" to "aa", "b" to "bb"))
            ContainerAttribute(
                Container(
                    Attributes {
                        TestAttribute(mapOf("a" to "aa", "b" to "bb"))
                    }
                )
            )
        }

        val serialized = protoBuf.encodeToByteArray(attributes)

        val restored: Attributes = protoBuf.decodeFromByteArray(serialized)

        assertEquals(attributes, restored)
    }
}