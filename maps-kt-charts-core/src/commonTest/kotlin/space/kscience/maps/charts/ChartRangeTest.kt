package space.kscience.maps.charts

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ChartRangeTest {
    @Test
    fun ticksPreserveSmallCoordinates() {
        val ticks = ChartRange(-2e-15, 2e-15).niceTicks(5)
        val expected = listOf(-2e-15, -1e-15, 0.0, 1e-15, 2e-15)

        assertEquals(expected.size, ticks.size)
        expected.zip(ticks).forEach { (expectedTick, tick) ->
            assertEquals(expectedTick, tick, 1e-28)
        }
    }

    @Test
    fun decimalTicksNormalizeRoundingErrorAtZero() {
        val ticks = ChartRange(-0.31, 0.31).niceTicks(8)

        assertTrue(0.0 in ticks)
    }
}
