package center.sciprog.attributes

import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import kotlin.test.Test
import kotlin.test.assertEquals

internal class AttributesSerializationTest {

    internal class TestAttributeContainer(val attributes: Attributes)

//    internal object TestContainerAttribute: SerializableAttribute<TestAttributeContainer>("container", se)

    internal object TestAttribute : SerializableAttribute<Map<String, String>>("test", serializer())

    @Test
    fun restore() {
//
//        val serializersModule = SerializersModule {
//            contextual(AttributesSerializer(setOf()))
//        }
        val serializer = AttributesSerializer(setOf(NameAttribute, TestAttribute))


        val attributes = Attributes {
            NameAttribute("myTest")
            TestAttribute(mapOf("a" to "aa", "b" to "bb"))
        }

        val serialized = Json.encodeToString(serializer, attributes)
        println(serialized)

        val restored = Json.decodeFromString(serializer, serialized)

        assertEquals(attributes, restored)
    }
}