package center.sciprog.attributes

import kotlinx.serialization.Contextual
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.contextual
import kotlinx.serialization.protobuf.ProtoBuf
import kotlinx.serialization.serializer
import space.kscience.AttributesSerializer
import space.kscience.NameAttribute
import space.kscience.SerializableAttribute
import space.kscience.attributes.Attributes
import space.kscience.equals
import kotlin.test.Ignore
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

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

    val serializer = AttributesSerializer(setOf(NameAttribute, TestAttribute, ContainerAttribute))

    @Test
    fun restoreFromJson() {

        val json = Json {
            serializersModule = SerializersModule {
                contextual(serializer)
            }
        }

        val attributes = Attributes<Any> {
            NameAttribute("myTest")
            TestAttribute(mapOf("a" to "aa", "b" to "bb"))
            ContainerAttribute(
                Container(
                    Attributes<Any> {
                        TestAttribute(mapOf("a" to "aa", "b" to "bb"))
                    }
                )
            )
        }


        val serialized: String = json.encodeToString(serializer, attributes)
        println(serialized)

        val restored: Attributes = json.decodeFromString(serializer, serialized)

        assertTrue { Attributes.equals(attributes, restored) }
    }

    @OptIn(ExperimentalSerializationApi::class)
    @Test
    @Ignore
    fun restoreFromProtoBuf() {
        val protoBuf = ProtoBuf {
            serializersModule = SerializersModule {
                contextual(serializer)
            }
        }

        val attributes = Attributes<Any> {
            NameAttribute("myTest")
            TestAttribute(mapOf("a" to "aa", "b" to "bb"))
            ContainerAttribute(
                Container(
                    Attributes<Any> {
                        TestAttribute(mapOf("a" to "aa", "b" to "bb"))
                    }
                )
            )
        }

        val serialized = protoBuf.encodeToByteArray(serializer, attributes)

        val restored: Attributes = protoBuf.decodeFromByteArray(serializer, serialized)

        assertEquals(attributes, restored)
    }
}