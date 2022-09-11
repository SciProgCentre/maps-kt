package center.sciprog.maps.coordinates

import kotlin.test.Test
import kotlin.test.assertEquals

class AngleTest {
    @Test
    fun normalization(){
        assertEquals(30.degrees, 390.degrees.normalized())
        assertEquals(30.degrees, (-330).degrees.normalized())
    }
}