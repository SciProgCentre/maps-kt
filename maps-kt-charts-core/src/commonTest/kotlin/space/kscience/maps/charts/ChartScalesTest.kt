package space.kscience.maps.charts

import kotlinx.datetime.TimeZone
import kotlin.math.sqrt
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Instant

class ChartScalesTest {
    @Test
    fun linearScaleIsIdentityAndProducesNiceTicks() {
        val scale = ChartScale.Linear

        assertEquals(-3.25, scale.transform(-3.25))
        assertEquals(8.5, scale.inverse(8.5))
        assertTrue(scale.contains(0.0))
        assertFalse(scale.contains(Double.POSITIVE_INFINITY))
        assertEquals(
            listOf(
                ChartTick(-2.0, "-2"),
                ChartTick(0.0, "0"),
                ChartTick(2.0, "2"),
                ChartTick(4.0, "4"),
                ChartTick(6.0, "6"),
            ),
            scale.ticks(ChartRange(-2.0, 7.0), 6),
        )
    }

    @Test
    fun logarithmicScaleValidatesDomainTransformsAndTicksPowers() {
        val scale = ChartScale.Log(base = 2.0)

        assertEquals(3.0, scale.transform(8.0), 1e-12)
        assertEquals(8.0, scale.inverse(3.0), 1e-12)
        assertTrue(scale.contains(0.5))
        assertFalse(scale.contains(0.0))
        assertFalse(scale.contains(Double.NaN))
        assertFailsWith<IllegalArgumentException> { ChartScale.Log(1.0) }
        assertFailsWith<IllegalArgumentException> { ChartScale.Log(Double.POSITIVE_INFINITY) }
        assertFailsWith<IllegalArgumentException> { scale.transform(-1.0) }

        val decimal = ChartScale.Log()
        assertEquals(
            listOf(
                ChartTick(1.0, "1"),
                ChartTick(10.0, "10"),
                ChartTick(100.0, "100"),
                ChartTick(1_000.0, "1000"),
            ),
            decimal.ticks(ChartRange(1.0, 1_000.0), 4),
        )
        assertEquals(
            listOf(ChartTick(2.0, "2"), ChartTick(3.0, "3")),
            decimal.ticks(ChartRange(2.0, 3.0), 4),
        )
        assertFailsWith<IllegalArgumentException> { decimal.ticks(ChartRange(0.0, 10.0), 4) }
    }

    @Test
    fun timeScaleProducesFixedUtcTicksAndOffsetLabels() {
        val range = ChartRange(0.0, 60_000.0)
        val expectedMillis = listOf(0.0, 30_000.0, 60_000.0)
        val utc = ChartScale.Time(TimeZone.UTC)

        assertEquals(expectedMillis, utc.ticks(range, 3).map { it.value })
        assertEquals(
            listOf("00:00:00.000", "00:00:30.000", "00:01:00.000"),
            utc.ticks(range, 3).map { it.label },
        )
        val offset = ChartScale.Time(TimeZone.of("+02:00"))
        assertEquals(
            listOf("02:00:00.000", "02:00:30.000", "02:01:00.000"),
            offset.ticks(range, 3).map { it.label },
        )
        assertEquals(12_345.0, utc.inverse(12_345.0))
        assertTrue(utc.contains(0.0))
        assertFalse(utc.contains(Double.NEGATIVE_INFINITY))
        assertEquals(listOf(30_000.0, 60_000.0), utc.ticks(ChartRange(5_000.0, 65_000.0), 3).map { it.value })
        assertEquals("00:30:00", utc.format(1_800_000.0, ChartRange(0.0, 3_600_000.0)))
    }

    @Test
    fun calendarTicksAlignToMonthsAndIncludeLeapDay() {
        val monthly = ChartScale.Time().ticks(ChartRange(1_705_276_800_000.0, 1_713_139_200_000.0), 5)

        assertEquals(
            listOf(1_706_745_600_000.0, 1_709_251_200_000.0, 1_711_929_600_000.0),
            monthly.map { it.value },
        )
        assertEquals(listOf("2024-02-01", "2024-03-01", "2024-04-01"), monthly.map { it.label })

        val leapDays = ChartScale.Time().ticks(ChartRange(1_709_132_400_000.0, 1_709_305_200_000.0), 3)
        assertEquals(listOf(1_709_164_800_000.0, 1_709_251_200_000.0), leapDays.map { it.value })
        assertEquals(listOf("2024-02-29 00:00", "2024-03-01 00:00"), leapDays.map { it.label })
    }

    @Test
    fun timeScaleRequiresAtLeastTwoTicks() {
        assertFailsWith<IllegalArgumentException> {
            ChartScale.Time().ticks(ChartRange(0.0, 1_000.0), 1)
        }
    }

    @Test
    fun expansionUsesTheScaleDomain() {
        assertEquals(ChartRange(-1.0, 11.0), ChartScale.Linear.expand(ChartRange(0.0, 10.0), 0.1))

        val logarithmic = ChartScale.Log().expand(ChartRange(1.0, 100.0), 0.5)
        assertEquals(0.1, logarithmic.min, 1e-12)
        assertEquals(1_000.0, logarithmic.max, 1e-9)

        val singleLogValue = ChartScale.Log().expand(ChartRange(10.0, 10.0), 0.0)
        assertEquals(sqrt(10.0), singleLogValue.min, 1e-12)
        assertEquals(10.0 * sqrt(10.0), singleLogValue.max, 1e-12)
        assertEquals(ChartRange(0.0, 2_000.0), ChartScale.Time().expand(ChartRange(1_000.0, 1_000.0), 0.0))
    }

    @Test
    fun calendarTicksAlignToYears() {
        val first = Instant.parse("2000-01-01T00:00:00Z").toChartValue()
        val last = Instant.parse("2020-01-01T00:00:00Z").toChartValue()
        val ticks = ChartScale.Time().ticks(ChartRange(first, last), 5)

        assertEquals(
            listOf("2000-01-01", "2005-01-01", "2010-01-01", "2015-01-01", "2020-01-01"),
            ticks.map { it.label },
        )
    }

    @Test
    fun calendarTicksCoverCenturyWideRanges() {
        val first = Instant.parse("1900-01-01T00:00:00Z").toChartValue()
        val last = Instant.parse("2000-01-01T00:00:00Z").toChartValue()
        val ticks = ChartScale.Time().ticks(ChartRange(first, last), 5)

        assertEquals(
            listOf("1900-01-01", "1925-01-01", "1950-01-01", "1975-01-01", "2000-01-01"),
            ticks.map { it.label },
        )
    }

    @Test
    fun logarithmicTicksStayInsideVisibleRange() {
        val scale = ChartScale.Log()

        assertEquals(listOf(10.0), scale.ticks(ChartRange(1.0000000000005, 10.0), 4).map { it.value })
        assertEquals(listOf(1.0), scale.ticks(ChartRange(1.0, 9.9999999999995), 4).map { it.value })
    }

    @Test
    fun logarithmicTicksHandleBasesCloseToOne() {
        val range = ChartRange(1e-200, 1e200)
        val ticks = ChartScale.Log(1.000000000001).ticks(range, 6)

        assertTrue(ticks.size in 2..6)
        assertTrue(ticks.all { it.value.isFinite() && it.value in range })
        assertTrue(ticks.zipWithNext().all { (left, right) -> left.value < right.value })
    }

    @Test
    fun numericLabelsDistinguishTicksOnNarrowRanges() {
        val ticks = ChartScale.Linear.ticks(ChartRange(1_000.0, 1_000.5), 6)

        assertEquals(listOf("1000", "1000.1", "1000.2", "1000.3", "1000.4", "1000.5"), ticks.map { it.label })
        assertEquals("1000.3", ChartScale.Linear.format(1_000.3, ChartRange(1_000.0, 1_000.5)))
    }

    @Test
    fun timeTicksCoverUnalignedCalendarRanges() {
        val first = Instant.parse("2024-01-02T12:00:00Z").toChartValue()
        val last = Instant.parse("2024-01-05T00:00:00Z").toChartValue()
        val ticks = ChartScale.Time().ticks(ChartRange(first, last), 2)

        assertEquals(listOf(first, last), ticks.map { it.value })
    }

    @Test
    fun timeScaleRejectsUnrepresentableEpochMilliseconds() {
        val scale = ChartScale.Time()

        assertFalse(scale.contains(Double.MAX_VALUE))
        assertFalse(scale.contains(-Double.MAX_VALUE))
        assertFailsWith<IllegalArgumentException> {
            scale.ticks(ChartRange(Double.MAX_VALUE, Double.MAX_VALUE), 6)
        }
        assertFailsWith<IllegalArgumentException> {
            scale.format(Double.NaN, ChartRange(0.0, 1_000.0))
        }
    }

    @Test
    fun timeLabelsPreserveIsoNegativeYears() {
        val first = Instant.parse("-0001-01-01T00:00:00Z").toChartValue()
        val last = Instant.parse("0001-01-01T00:00:00Z").toChartValue()

        assertEquals("-0001-01-01", ChartScale.Time().format(first, ChartRange(first, last)))
    }

    @Test
    fun fixedTimeTicksAlignBeforeEpochAndAcceptLargeTargetCount() {
        val scale = ChartScale.Time()

        assertEquals(
            listOf(-2.0, -1.0, 0.0, 1.0, 2.0),
            scale.ticks(ChartRange(-2.5, 2.5), Int.MAX_VALUE).map { it.value },
        )
    }

    @Test
    fun timeTicksHandleEpochMillisecondLimits() {
        val range = ChartRange(Long.MIN_VALUE.toDouble(), Long.MAX_VALUE.toDouble())
        val ticks = ChartScale.Time().ticks(range, 6)

        assertTrue(ticks.isNotEmpty())
        assertTrue(ticks.all { it.value in range })
        assertTrue(ticks.zipWithNext().all { (left, right) -> left.value < right.value })
    }
}
