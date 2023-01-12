package center.sciprog.attributes

import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import kotlin.test.Test
import kotlin.test.assertEquals

internal class AttributesSerializationTest {

    internal object TestAttribute : SerializableAttribute<Map<String, String>>("test", serializer())

    @Test
    fun restore() {
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