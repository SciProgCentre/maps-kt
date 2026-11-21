package space.kscience.maps.charts

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Instant

class ChartPrimitivesTest {
    @Test
    fun packedColorsExposeComponentsAndReplaceOnlyAlpha() {
        val color = ChartColor.argb(0x12, 0x34, 0x56, 0x78)

        assertEquals(0x12345678u, color.argb)
        assertEquals(0x12, color.alpha)
        assertEquals(0x34, color.red)
        assertEquals(0x56, color.green)
        assertEquals(0x78, color.blue)
        assertEquals(0xFF010203u, ChartColor.rgb(1, 2, 3).argb)
        assertEquals(0x7F345678u, color.withAlpha(0.5f).argb)
        assertEquals(0x00345678u, color.withAlpha(0f).argb)
        assertEquals(0xFF345678u, color.withAlpha(1f).argb)
        assertEquals(0x12345678u, color.argb)
    }

    @Test
    fun colorsRejectOutOfRangeComponentsAndAlpha() {
        assertFailsWith<IllegalArgumentException> { ChartColor.argb(-1, 0, 0, 0) }
        assertFailsWith<IllegalArgumentException> { ChartColor.argb(256, 0, 0, 0) }
        assertFailsWith<IllegalArgumentException> { ChartColor.argb(0, -1, 0, 0) }
        assertFailsWith<IllegalArgumentException> { ChartColor.argb(0, 256, 0, 0) }
        assertFailsWith<IllegalArgumentException> { ChartColor.argb(0, 0, -1, 0) }
        assertFailsWith<IllegalArgumentException> { ChartColor.argb(0, 0, 256, 0) }
        assertFailsWith<IllegalArgumentException> { ChartColor.argb(0, 0, 0, -1) }
        assertFailsWith<IllegalArgumentException> { ChartColor.argb(0, 0, 0, 256) }
        assertFailsWith<IllegalArgumentException> { ChartColor.Black.withAlpha(-0.01f) }
        assertFailsWith<IllegalArgumentException> { ChartColor.Black.withAlpha(1.01f) }
        assertFailsWith<IllegalArgumentException> { ChartColor.Black.withAlpha(Float.NaN) }
    }

    @Test
    fun lengthsPointsAndRangesEnforceFiniteOrderedDomains() {
        assertEquals(2.5f, ChartLength(2.5f).value)
        assertFailsWith<IllegalArgumentException> { ChartLength(-1f) }
        assertFailsWith<IllegalArgumentException> { ChartLength(Float.NaN) }
        assertFailsWith<IllegalArgumentException> { ChartLength(Float.POSITIVE_INFINITY) }

        assertEquals(ChartPoint(2.0, 3.0), chartPoint(2, 3))
        assertFailsWith<IllegalArgumentException> { ChartPoint(Double.NaN, 0.0) }
        assertFailsWith<IllegalArgumentException> { ChartPoint(0.0, Double.NEGATIVE_INFINITY) }

        val range = ChartRange(-2.0, 4.0)
        assertEquals(6.0, range.span)
        assertTrue(-2.0 in range)
        assertTrue(4.0 in range)
        assertFalse(4.1 in range)
        assertEquals(ChartRange(-2.6, 4.6), range.expand(0.1))
        assertFailsWith<IllegalArgumentException> { ChartRange(Double.POSITIVE_INFINITY, 1.0) }
        assertFailsWith<IllegalArgumentException> { ChartRange(2.0, 1.0) }
    }

    @Test
    fun boundsHandleEmptyPointsAndWrappedRanges() {
        assertNull(ChartBounds.from(emptyList()))
        assertEquals(
            ChartBounds(ChartRange(-3.0, 5.0), ChartRange(-2.0, 7.0)),
            ChartBounds.from(listOf(ChartPoint(1.0, 7.0), ChartPoint(-3.0, -2.0), ChartPoint(5.0, 4.0))),
        )
        assertNull(ChartBounds.wrap(emptyList()))
        assertEquals(
            ChartBounds(ChartRange(-4.0, 9.0), ChartRange(-6.0, 8.0)),
            ChartBounds.wrap(
                listOf(
                    ChartBounds(ChartRange(-4.0, 2.0), ChartRange(1.0, 8.0)),
                    ChartBounds(ChartRange(3.0, 9.0), ChartRange(-6.0, 5.0)),
                ),
            ),
        )
    }

    @Test
    fun niceTicksIncludeZeroAndZeroSpanHasOneTick() {
        assertEquals(listOf(-2.0, 0.0, 2.0, 4.0, 6.0), ChartRange(-2.0, 7.0).niceTicks(6))
        assertEquals(listOf(4.0), ChartRange(4.0, 4.0).niceTicks())
        assertFailsWith<IllegalArgumentException> { ChartRange(0.0, 1.0).niceTicks(1) }
    }

    @Test
    fun instantHelpersUseEpochMilliseconds() {
        val instant = Instant.fromEpochMilliseconds(1_700_000_000_123L)

        assertEquals(1_700_000_000_123.0, instant.toChartValue())
        assertEquals(ChartPoint(1_700_000_000_123.0, 8.0), chartPoint(instant, 8))
        assertEquals(ChartPoint(8.0, 1_700_000_000_123.0), chartPoint(8, instant))
    }
}
