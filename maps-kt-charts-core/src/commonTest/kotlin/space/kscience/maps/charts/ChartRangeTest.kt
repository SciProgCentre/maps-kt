package space.kscience.maps.charts

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
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

    @Test
    fun ticksStayInsideClosedInterval() {
        val range = ChartRange(0.1, 0.3)
        val ticks = range.niceTicks(3)

        assertEquals(3, ticks.size)
        assertTrue(ticks.all { it in range })
        assertEquals(0.3, ticks.last())
    }

    @Test
    fun ticksAreDistinctNearLargeOffset() {
        val range = ChartRange(1e16, 1e16 + 2.0)

        assertEquals(listOf(range.min, range.max), range.niceTicks(6))
    }

    @Test
    fun ticksHandleSubnormalAndOverflowingSpans() {
        assertEquals(listOf(0.0, Double.MIN_VALUE), ChartRange(0.0, Double.MIN_VALUE).niceTicks(6))
        assertEquals(
            listOf(-Double.MAX_VALUE, 0.0, Double.MAX_VALUE),
            ChartRange(-Double.MAX_VALUE, Double.MAX_VALUE).niceTicks(3),
        )
    }

    @Test
    fun paddingMustBeFiniteEvenWhenUnused() {
        assertFailsWith<IllegalArgumentException> {
            ChartRange(1.0, 1.0).expand(relativePadding = Double.POSITIVE_INFINITY)
        }
        assertFailsWith<IllegalArgumentException> {
            ChartRange(0.0, 1.0).expand(absolutePadding = Double.POSITIVE_INFINITY)
        }
    }

    @Test
    fun zeroPaddingHandlesOverflowingSpan() {
        val range = ChartRange(-Double.MAX_VALUE, Double.MAX_VALUE)

        assertEquals(range, range.expand(0.0))
    }
}
