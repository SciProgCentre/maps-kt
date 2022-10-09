/*
 * Copyright 2018-2021 KMath contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package center.sciprog.maps.coordinates

import kotlin.jvm.JvmInline
import kotlin.math.PI
import kotlin.math.floor

// Taken from KMath dev version, to be used directly in the future


public sealed interface Angle : Comparable<Angle> {
    public val radians: Radians
    public val degrees: Degrees

    public operator fun plus(other: Angle): Angle
    public operator fun minus(other: Angle): Angle

    public operator fun times(other: Number): Angle
    public operator fun div(other: Number): Angle
    public operator fun div(other: Angle): Double
    public operator fun unaryMinus(): Angle

    public companion object {
        public val zero: Angle = 0.radians
        public val pi: Angle = PI.radians
        public val piTimes2: Angle = (2 * PI).radians
        public val piDiv2: Angle = (PI / 2).radians
    }
}

/**
 * Type safe radians
 */
@JvmInline
public value class Radians(public val value: Double) : Angle {
    override val radians: Radians
        get() = this
    override val degrees: Degrees
        get() = Degrees(value * 180 / PI)

    public override fun plus(other: Angle): Radians = Radians(value + other.radians.value)
    public override fun minus(other: Angle): Radians = Radians(value - other.radians.value)

    public override fun times(other: Number): Radians = Radians(value * other.toDouble())
    public override fun div(other: Number): Radians = Radians(value / other.toDouble())
    override fun div(other: Angle): Double = value / other.radians.value
    public override fun unaryMinus(): Radians = Radians(-value)

    override fun compareTo(other: Angle): Int = value.compareTo(other.radians.value)
}

public fun sin(angle: Angle): Double = kotlin.math.sin(angle.radians.value)
public fun cos(angle: Angle): Double = kotlin.math.cos(angle.radians.value)
public fun tan(angle: Angle): Double = kotlin.math.tan(angle.radians.value)

public val Number.radians: Radians get() = Radians(toDouble())

/**
 * Type safe degrees
 */
@JvmInline
public value class Degrees(public val value: Double) : Angle {
    override val radians: Radians
        get() = Radians(value * PI / 180)
    override val degrees: Degrees
        get() = this

    public override fun plus(other: Angle): Degrees = Degrees(value + other.degrees.value)
    public override fun minus(other: Angle): Degrees = Degrees(value - other.degrees.value)

    public override fun times(other: Number): Degrees = Degrees(value * other.toDouble())
    public override fun div(other: Number): Degrees = Degrees(value / other.toDouble())
    override fun div(other: Angle): Double = value / other.degrees.value
    public override fun unaryMinus(): Degrees = Degrees(-value)

    override fun compareTo(other: Angle): Int = value.compareTo(other.degrees.value)
}

public val Number.degrees: Degrees get() = Degrees(toDouble())

/**
 * Normalized angle 2 PI range symmetric around [center]. By default, uses (0, 2PI) range.
 */
public fun Angle.normalized(center: Angle = Angle.pi): Angle =
    this - Angle.piTimes2 * floor((radians.value + PI - center.radians.value) / PI/2)

public fun abs(angle: Angle): Angle = if (angle < Angle.zero) -angle else angle

public fun Radians.toFloat(): Float = value.toFloat()

public fun Degrees.toFloat(): Float = value.toFloat()
