package space.kscience.maps.charts

import kotlin.jvm.JvmInline
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.round
import kotlin.time.Instant

/** A backend-independent color packed as alpha, red, green and blue bytes. */
@JvmInline
public value class ChartColor(public val argb: UInt) {
    public val alpha: Int get() = ((argb shr 24) and 0xFFu).toInt()
    public val red: Int get() = ((argb shr 16) and 0xFFu).toInt()
    public val green: Int get() = ((argb shr 8) and 0xFFu).toInt()
    public val blue: Int get() = (argb and 0xFFu).toInt()

    /** Replaces the alpha channel with a value in the inclusive range from zero to one. */
    public fun withAlpha(alpha: Float): ChartColor {
        require(alpha in 0f..1f) { "Alpha must be between 0 and 1" }
        val alphaByte = (alpha * 255f).toInt().coerceIn(0, 255).toUInt()
        return ChartColor((argb and 0x00FFFFFFu) or (alphaByte shl 24))
    }

    public companion object {
        public val Transparent: ChartColor = ChartColor(0x00000000u)
        public val Black: ChartColor = ChartColor(0xFF000000u)
        public val White: ChartColor = ChartColor(0xFFFFFFFFu)

        public fun rgb(red: Int, green: Int, blue: Int): ChartColor = argb(255, red, green, blue)

        /** Creates a color from channel values in the inclusive range from zero to 255. */
        public fun argb(alpha: Int, red: Int, green: Int, blue: Int): ChartColor {
            require(alpha in 0..255 && red in 0..255 && green in 0..255 && blue in 0..255) {
                "ARGB components must be between 0 and 255"
            }
            return ChartColor(
                (alpha.toUInt() shl 24) or
                    (red.toUInt() shl 16) or
                    (green.toUInt() shl 8) or
                    blue.toUInt()
            )
        }
    }
}

/** A finite, non-negative length in logical pixels, equivalent to Compose Dp or a CSS pixel. */
@JvmInline
public value class ChartLength(public val value: Float) {
    init {
        require(value.isFinite() && value >= 0f) { "Chart length must be finite and non-negative" }
    }
}

/** A point with finite coordinates in the data domain. */
public data class ChartPoint(
    public val x: Double,
    public val y: Double,
) {
    init {
        require(x.isFinite()) { "Chart point x must be finite" }
        require(y.isFinite()) { "Chart point y must be finite" }
    }
}

public fun ChartPoint(x: Number, y: Number): ChartPoint = ChartPoint(x.toDouble(), y.toDouble())

public fun chartPoint(x: Number, y: Number): ChartPoint = ChartPoint(x, y)

/** Creates a point with an X coordinate expressed in epoch milliseconds. */
public fun chartPoint(x: Instant, y: Number): ChartPoint = ChartPoint(x.toEpochMilliseconds(), y)

/** Creates a point with a Y coordinate expressed in epoch milliseconds. */
public fun chartPoint(x: Number, y: Instant): ChartPoint = ChartPoint(x, y.toEpochMilliseconds())

/** Converts an instant to the epoch-millisecond coordinate used by time scales. */
public fun Instant.toChartValue(): Double = toEpochMilliseconds().toDouble()

/** A closed interval with finite, ordered endpoints. Equal endpoints are allowed. */
public data class ChartRange(
    public val min: Double,
    public val max: Double,
) {
    init {
        require(min.isFinite()) { "Chart range min must be finite" }
        require(max.isFinite()) { "Chart range max must be finite" }
        require(min <= max) { "Chart range min must not exceed max" }
    }

    public val span: Double get() = max - min

    public operator fun contains(value: Double): Boolean = value in min..max

    /** Pads both ends; [absolutePadding] is used when the range has zero span. */
    public fun expand(relativePadding: Double = 0.05, absolutePadding: Double = 1.0): ChartRange {
        require(relativePadding.isFinite() && relativePadding >= 0.0) { "Relative padding must be finite and non-negative" }
        require(absolutePadding.isFinite() && absolutePadding > 0.0) { "Absolute padding must be finite and positive" }
        if (relativePadding == 0.0 && span != 0.0) return this
        val padding = if (span == 0.0) absolutePadding else span * relativePadding
        return ChartRange(min - padding, max + padding)
    }

    /** Selects decimal ticks using a 1, 2 or 5 step multiplier and an approximate target count. */
    public fun niceTicks(maxTickCount: Int = 6): List<Double> {
        require(maxTickCount >= 2) { "At least two ticks are required" }
        if (span == 0.0) return listOf(min)

        val divisions = (maxTickCount - 1).toDouble()
        val rawStep = if (span.isFinite()) span / divisions else max / divisions - min / divisions
        if (!rawStep.isFinite() || rawStep <= 0.0) return listOf(min, max)
        val step = niceStep(rawStep)
        val tolerance = step * 1e-9
        val start = (floor(min / step) * step).takeIf { it.isFinite() } ?: (ceil(min / step) * step)
        val ticks = mutableListOf<Double>()
        var value = start
        while (value.isFinite() && value <= max + tolerance && ticks.size.toLong() <= maxTickCount.toLong() + 1) {
            val tick = normalizeTick(value, step)
            if (tick >= min - tolerance && tick <= max + tolerance) {
                val bounded = tick.coerceIn(min, max)
                if (ticks.isEmpty() || bounded > ticks.last()) ticks += bounded
            }
            val next = value + step
            if (next <= value) return listOf(min, max)
            value = next
        }
        return ticks.ifEmpty { listOf(min, max) }
    }

    public companion object {
        private fun niceStep(rawStep: Double): Double {
            val exponent = floor(log10(rawStep))
            val base = 10.0.pow(exponent)
            if (base == 0.0) return rawStep
            val fraction = rawStep / base
            val niceFraction = when {
                fraction <= 1.0 -> 1.0
                fraction <= 2.0 -> 2.0
                fraction <= 5.0 -> 5.0
                else -> 10.0
            }
            return (niceFraction * base).takeIf { it.isFinite() } ?: rawStep
        }

        private fun normalizeTick(value: Double, step: Double): Double =
            if (abs(value) < step * 1e-9) 0.0 else value
    }
}

/** Axis-aligned bounds in the data domain. */
public data class ChartBounds(
    public val x: ChartRange,
    public val y: ChartRange,
) {
    public companion object {
        public val DEFAULT: ChartBounds = ChartBounds(ChartRange(0.0, 1.0), ChartRange(0.0, 1.0))

        /** Finds the bounds of all points, or returns null for an empty input. */
        public fun from(points: Iterable<ChartPoint>): ChartBounds? {
            val iterator = points.iterator()
            if (!iterator.hasNext()) return null
            val first = iterator.next()
            var minX = first.x
            var maxX = first.x
            var minY = first.y
            var maxY = first.y
            while (iterator.hasNext()) {
                val point = iterator.next()
                minX = kotlin.math.min(minX, point.x)
                maxX = kotlin.math.max(maxX, point.x)
                minY = kotlin.math.min(minY, point.y)
                maxY = kotlin.math.max(maxY, point.y)
            }
            return ChartBounds(ChartRange(minX, maxX), ChartRange(minY, maxY))
        }

        /** Finds the union of all bounds, or returns null for an empty input. */
        public fun wrap(bounds: Iterable<ChartBounds>): ChartBounds? {
            val iterator = bounds.iterator()
            if (!iterator.hasNext()) return null
            val first = iterator.next()
            var minX = first.x.min
            var maxX = first.x.max
            var minY = first.y.min
            var maxY = first.y.max
            while (iterator.hasNext()) {
                val next = iterator.next()
                minX = kotlin.math.min(minX, next.x.min)
                maxX = kotlin.math.max(maxX, next.x.max)
                minY = kotlin.math.min(minY, next.y.min)
                maxY = kotlin.math.max(maxY, next.y.max)
            }
            return ChartBounds(ChartRange(minX, maxX), ChartRange(minY, maxY))
        }
    }
}

/** Formats a coordinate with magnitude-dependent decimal precision for axis labels. */
public fun Double.formatChartNumber(): String {
    if (this == 0.0) return "0"
    val absolute = abs(this)
    if (absolute >= 1_000_000.0 || absolute < 0.001) return toString()
    val decimals = when {
        absolute >= 100.0 -> 0
        absolute >= 10.0 -> 1
        absolute >= 1.0 -> 2
        else -> 3
    }
    val factor = 10.0.pow(decimals)
    return (round(this * factor) / factor).toString().removeSuffix(".0")
}
