package space.kscience.maps.charts

import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.log
import kotlin.math.pow
import kotlin.time.Instant

/** A tick position in the data domain and its display label. */
public data class ChartTick(public val value: Double, public val label: String)

/** Maps data coordinates to an axis domain independently of any rendering backend. */
public sealed interface ChartScale {
    public fun transform(value: Double): Double
    public fun inverse(value: Double): Double
    public fun contains(value: Double): Boolean

    /** Selects ticks for a visible range using an approximate target count of at least two. */
    public fun ticks(range: ChartRange, maxTickCount: Int): List<ChartTick>

    /** Pads a range in the transformed domain. */
    public fun expand(range: ChartRange, relativePadding: Double): ChartRange

    public data object Linear : ChartScale {
        override fun transform(value: Double): Double = value
        override fun inverse(value: Double): Double = value
        override fun contains(value: Double): Boolean = value.isFinite()
        override fun ticks(range: ChartRange, maxTickCount: Int): List<ChartTick> =
            range.niceTicks(maxTickCount).map { ChartTick(it, it.formatChartNumber()) }

        override fun expand(range: ChartRange, relativePadding: Double): ChartRange = range.expand(relativePadding)
    }

    /** A logarithmic scale with a finite base greater than one and strictly positive coordinates. */
    public data class Log(public val base: Double = 10.0) : ChartScale {
        init {
            require(base.isFinite() && base > 1.0) { "Logarithm base must be finite and greater than one" }
        }

        override fun transform(value: Double): Double {
            require(contains(value)) { "Logarithmic scale values must be positive" }
            return log(value, base)
        }

        override fun inverse(value: Double): Double = base.pow(value)
        override fun contains(value: Double): Boolean = value.isFinite() && value > 0.0

        override fun ticks(range: ChartRange, maxTickCount: Int): List<ChartTick> {
            require(range.min > 0.0) { "Logarithmic range must be positive" }
            require(maxTickCount >= 2) { "At least two ticks are required" }
            val firstPower = ceil(log(range.min, base) - 1e-12)
            val lastPower = floor(log(range.max, base) + 1e-12)
            if (firstPower > lastPower) {
                return listOf(range.min, range.max).distinct().map { ChartTick(it, it.formatChartNumber()) }
            }
            val powerCount = lastPower - firstPower + 1
            val stride = ceil(powerCount / maxTickCount).coerceAtLeast(1.0)
            val count = ceil(powerCount / stride).toInt().coerceAtMost(maxTickCount)
            return List(count) { index ->
                val value = base.pow(firstPower + index * stride)
                ChartTick(value, value.formatChartNumber())
            }.filter { it.value in range }.ifEmpty {
                listOf(range.min, range.max).distinct().map { ChartTick(it, it.formatChartNumber()) }
            }
        }

        override fun expand(range: ChartRange, relativePadding: Double): ChartRange {
            require(range.min > 0.0) { "Logarithmic range must be positive" }
            val transformed = ChartRange(transform(range.min), transform(range.max))
            val expanded = transformed.expand(relativePadding, 0.5)
            return ChartRange(inverse(expanded.min), inverse(expanded.max))
        }
    }

    /** Epoch-millisecond coordinates with calendar-aware ticks and labels in [timeZone]. */
    public data class Time(public val timeZone: TimeZone = TimeZone.UTC) : ChartScale {
        override fun transform(value: Double): Double = value
        override fun inverse(value: Double): Double = value
        override fun contains(value: Double): Boolean = value.isFinite()
        override fun expand(range: ChartRange, relativePadding: Double): ChartRange =
            range.expand(relativePadding, 1_000.0)

        override fun ticks(range: ChartRange, maxTickCount: Int): List<ChartTick> {
            require(maxTickCount >= 2) { "At least two ticks are required" }
            val step = chooseTimeStep(range.span, maxTickCount)
            return when (step.unit) {
                null -> fixedTimeTicks(range, step.amount, maxTickCount, timeZone)
                else -> calendarTimeTicks(range, step.amount.toInt(), step.unit, maxTickCount, timeZone)
            }
        }
    }
}

private data class TimeStep(val amount: Long, val unit: DateTimeUnit.DateBased? = null)

private val timeSteps: List<TimeStep> = listOf(
    1L, 2L, 5L, 10L, 20L, 50L, 100L, 200L, 500L,
    1_000L, 2_000L, 5_000L, 10_000L, 15_000L, 30_000L,
    60_000L, 120_000L, 300_000L, 600_000L, 900_000L, 1_800_000L,
    3_600_000L, 7_200_000L, 10_800_000L, 21_600_000L, 43_200_000L,
).map(::TimeStep) + listOf(
    TimeStep(1, DateTimeUnit.DAY),
    TimeStep(2, DateTimeUnit.DAY),
    TimeStep(7, DateTimeUnit.DAY),
    TimeStep(1, DateTimeUnit.MONTH),
    TimeStep(3, DateTimeUnit.MONTH),
    TimeStep(6, DateTimeUnit.MONTH),
    TimeStep(1, DateTimeUnit.YEAR),
    TimeStep(2, DateTimeUnit.YEAR),
    TimeStep(5, DateTimeUnit.YEAR),
)

private fun chooseTimeStep(span: Double, count: Int): TimeStep {
    val target = span / (count - 1).coerceAtLeast(1)
    return timeSteps.firstOrNull { step ->
        val approximateMillis = when (step.unit) {
            null -> step.amount.toDouble()
            DateTimeUnit.DAY -> step.amount * 86_400_000.0
            DateTimeUnit.MONTH -> step.amount * 30.4375 * 86_400_000.0
            DateTimeUnit.YEAR -> step.amount * 365.25 * 86_400_000.0
            else -> error("Unsupported calendar unit")
        }
        approximateMillis >= target
    } ?: TimeStep(
        ceil(target / (365.25 * 86_400_000.0)).toLong().coerceIn(1L, Int.MAX_VALUE.toLong()),
        DateTimeUnit.YEAR,
    )
}

private fun fixedTimeTicks(
    range: ChartRange,
    stepMillis: Long,
    maxTickCount: Int,
    timeZone: TimeZone,
): List<ChartTick> {
    var value = floor(range.min / stepMillis).toLong() * stepMillis
    if (value < range.min) value += stepMillis
    val ticks = mutableListOf<ChartTick>()
    while (value <= range.max && ticks.size <= maxTickCount + 1) {
        ticks += ChartTick(value.toDouble(), formatTime(value, stepMillis.toDouble(), timeZone))
        value += stepMillis
    }
    return ticks
}

private fun calendarTimeTicks(
    range: ChartRange,
    amount: Int,
    unit: DateTimeUnit.DateBased,
    maxTickCount: Int,
    timeZone: TimeZone,
): List<ChartTick> {
    val local = Instant.fromEpochMilliseconds(range.min.toLong()).toLocalDateTime(timeZone)
    val aligned = when (unit) {
        DateTimeUnit.DAY -> LocalDateTime(local.year, local.month.ordinal + 1, local.day, 0, 0)
        DateTimeUnit.MONTH -> LocalDateTime(local.year, local.month.ordinal + 1, 1, 0, 0)
        DateTimeUnit.YEAR -> LocalDateTime(local.year, 1, 1, 0, 0)
        else -> error("Unsupported calendar unit")
    }
    var instant = aligned.toInstant(timeZone)
    while (instant.toEpochMilliseconds() < range.min) {
        instant = instant.plus(amount, unit, timeZone)
    }
    val ticks = mutableListOf<ChartTick>()
    while (instant.toEpochMilliseconds() <= range.max && ticks.size <= maxTickCount + 1) {
        val millis = instant.toEpochMilliseconds()
        ticks += ChartTick(millis.toDouble(), formatTime(millis, range.span, timeZone))
        instant = instant.plus(amount, unit, timeZone)
    }
    return ticks
}

/** Formats a coordinate using the visible span to choose numeric or time precision. */
public fun ChartScale.format(value: Double, visibleRange: ChartRange): String = when (this) {
    ChartScale.Linear -> value.formatChartNumber()
    is ChartScale.Log -> value.formatChartNumber()
    is ChartScale.Time -> formatTime(value.toLong(), visibleRange.span, timeZone)
}

private fun formatTime(epochMillis: Long, spanMillis: Double, timeZone: TimeZone): String {
    val value = Instant.fromEpochMilliseconds(epochMillis).toLocalDateTime(timeZone)
    val date = "${value.year.toString().padStart(4, '0')}-${(value.month.ordinal + 1).twoDigits()}-${value.day.twoDigits()}"
    val time = "${value.hour.twoDigits()}:${value.minute.twoDigits()}:${value.second.twoDigits()}"
    return when {
        spanMillis < 60_000.0 -> "$time.${value.nanosecond.div(1_000_000).toString().padStart(3, '0')}"
        spanMillis < 86_400_000.0 -> time
        spanMillis < 31 * 86_400_000.0 -> "$date ${value.hour.twoDigits()}:${value.minute.twoDigits()}"
        else -> date
    }
}

private fun Int.twoDigits(): String = toString().padStart(2, '0')
