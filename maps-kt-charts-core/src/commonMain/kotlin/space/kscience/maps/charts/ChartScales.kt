package space.kscience.maps.charts

import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.log
import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.round
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
            numericTicks(range.niceTicks(maxTickCount))

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
                return numericTicks(listOf(range.min, range.max).distinct())
            }
            val powerCount = lastPower - firstPower + 1
            val stride = ceil(powerCount / maxTickCount).coerceAtLeast(1.0)
            val count = ceil(powerCount / stride).toInt().coerceAtMost(maxTickCount)
            val values = List(count) { index -> base.pow(firstPower + index * stride) }.filter { it in range }
            return numericTicks(values.ifEmpty { listOf(range.min, range.max).distinct() })
        }

        override fun expand(range: ChartRange, relativePadding: Double): ChartRange {
            require(range.min > 0.0) { "Logarithmic range must be positive" }
            val transformed = ChartRange(transform(range.min), transform(range.max))
            val expanded = transformed.expand(relativePadding, 0.5)
            return ChartRange(inverse(expanded.min), inverse(expanded.max))
        }
    }

    /** Epoch-millisecond coordinates in the Long range, with calendar ticks and millisecond-precision labels. */
    public data class Time(public val timeZone: TimeZone = TimeZone.UTC) : ChartScale {
        override fun transform(value: Double): Double = value
        override fun inverse(value: Double): Double = value
        override fun contains(value: Double): Boolean =
            value.isFinite() && value >= Long.MIN_VALUE.toDouble() && value <= Long.MAX_VALUE.toDouble()
        override fun expand(range: ChartRange, relativePadding: Double): ChartRange =
            range.expand(relativePadding, 1_000.0)

        override fun ticks(range: ChartRange, maxTickCount: Int): List<ChartTick> {
            require(maxTickCount >= 2) { "At least two ticks are required" }
            require(contains(range.min) && contains(range.max)) { "Time range must fit epoch milliseconds in Long" }
            val step = chooseTimeStep(range.span, maxTickCount)
            val ticks = when (step.unit) {
                null -> fixedTimeTicks(range, step.amount, maxTickCount, timeZone)
                else -> calendarTimeTicks(range, step.amount.toInt(), step.unit, maxTickCount, timeZone)
            }
            return ticks.ifEmpty {
                listOf(range.min, range.max).distinct().map { ChartTick(it, formatTime(it.toLong(), range.span, timeZone)) }
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
    var value = range.min.toLong()
    val remainder = value % stepMillis
    val delta = if (remainder <= 0L) -remainder else stepMillis - remainder
    if (value > Long.MAX_VALUE - delta) return emptyList()
    value += delta
    if (value < range.min) {
        if (value > Long.MAX_VALUE - stepMillis) return emptyList()
        value += stepMillis
    }
    val ticks = mutableListOf<ChartTick>()
    while (value <= range.max && ticks.size.toLong() <= maxTickCount.toLong() + 1) {
        val coordinate = value.toDouble()
        if (ticks.isEmpty() || coordinate > ticks.last().value) {
            ticks += ChartTick(coordinate, formatTime(value, stepMillis.toDouble(), timeZone))
        }
        if (value > Long.MAX_VALUE - stepMillis) break
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
    var date = when (unit) {
        DateTimeUnit.DAY -> local.date
        DateTimeUnit.MONTH -> LocalDate(local.year, local.month, 1)
        DateTimeUnit.YEAR -> LocalDate(local.year, 1, 1)
        else -> error("Unsupported calendar unit")
    }
    var instant = date.atStartOfDayIn(timeZone)
    while (instant.toEpochMilliseconds() < range.min) {
        date = date.plus(amount, unit)
        instant = date.atStartOfDayIn(timeZone)
    }
    val ticks = mutableListOf<ChartTick>()
    val end = Instant.fromEpochMilliseconds(range.max.toLong())
    while (instant <= end && ticks.size.toLong() <= maxTickCount.toLong() + 1) {
        val millis = instant.toEpochMilliseconds()
        if (ticks.isEmpty() || millis.toDouble() > ticks.last().value) {
            ticks += ChartTick(millis.toDouble(), formatTime(millis, range.span, timeZone))
        }
        date = date.plus(amount, unit)
        instant = date.atStartOfDayIn(timeZone)
    }
    return ticks
}

/** Formats a coordinate using the visible span to choose numeric or time precision. */
public fun ChartScale.format(value: Double, visibleRange: ChartRange): String {
    require(contains(value)) { "Coordinate is outside the scale domain" }
    require(contains(visibleRange.min) && contains(visibleRange.max)) { "Visible range is outside the scale domain" }
    return when (this) {
        ChartScale.Linear, is ChartScale.Log -> formatNumeric(value, visibleRange.span / 5.0)
        is ChartScale.Time -> formatTime(value.toLong(), visibleRange.span, timeZone)
    }
}

private fun numericTicks(values: List<Double>): List<ChartTick> {
    var step = Double.POSITIVE_INFINITY
    for (index in 1 until values.size) step = minOf(step, values[index] - values[index - 1])
    val ticks = values.map { ChartTick(it, formatNumeric(it, step)) }
    for (index in 1 until ticks.size) {
        if (ticks[index].label == ticks[index - 1].label) {
            return values.map { ChartTick(it, it.toString().removeSuffix(".0")) }
        }
    }
    return ticks
}

private fun formatNumeric(value: Double, step: Double): String {
    if (value == 0.0) return "0"
    if (!step.isFinite() || step <= 0.0 || abs(value) >= 1_000_000.0 || abs(value) < 0.001) {
        return value.toString().removeSuffix(".0")
    }
    val decimals = (-floor(log10(step))).coerceIn(0.0, 15.0).toInt()
    val factor = 10.0.pow(decimals)
    return (round(value * factor) / factor).toString().removeSuffix(".0")
}

private fun formatTime(epochMillis: Long, spanMillis: Double, timeZone: TimeZone): String {
    val value = Instant.fromEpochMilliseconds(epochMillis).toLocalDateTime(timeZone)
    val date = value.date.toString()
    val time = "${value.hour.twoDigits()}:${value.minute.twoDigits()}:${value.second.twoDigits()}"
    return when {
        spanMillis < 60_000.0 -> "$time.${value.nanosecond.div(1_000_000).toString().padStart(3, '0')}"
        spanMillis < 86_400_000.0 -> time
        spanMillis < 31 * 86_400_000.0 -> "$date ${value.hour.twoDigits()}:${value.minute.twoDigits()}"
        else -> date
    }
}

private fun Int.twoDigits(): String = toString().padStart(2, '0')
