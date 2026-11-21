package space.kscience.maps.charts

import kotlinx.datetime.TimeZone
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant

class ChartTimeZoneTest {
    @Test
    fun midnightGapDoesNotShiftFollowingDays() {
        val expected = listOf(
            "2018-11-04T03:00:00Z",
            "2018-11-05T02:00:00Z",
            "2018-11-06T02:00:00Z",
            "2018-11-07T02:00:00Z",
        ).map { Instant.parse(it).toChartValue() }
        val ticks = ChartScale.Time(TimeZone.of("America/Sao_Paulo"))
            .ticks(ChartRange(expected.first(), expected.last()), 4)

        assertEquals(expected, ticks.map { it.value })
    }

    @Test
    fun skippedCalendarDateDoesNotDuplicateTicks() {
        val expected = listOf(
            "2011-12-29T10:00:00Z",
            "2011-12-30T10:00:00Z",
            "2011-12-31T10:00:00Z",
            "2012-01-01T10:00:00Z",
        ).map { Instant.parse(it).toChartValue() }
        val ticks = ChartScale.Time(TimeZone.of("Pacific/Apia"))
            .ticks(ChartRange(expected.first(), expected.last()), 5)

        assertEquals(expected, ticks.map { it.value })
    }
}
