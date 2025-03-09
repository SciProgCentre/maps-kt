package space.kscience.maps.coordinates

import kotlinx.serialization.Serializable
import kotlin.jvm.JvmInline

@Serializable
@JvmInline
public value class Distance internal constructor(public val kilometers: Double) : Comparable<Distance> {
    override fun compareTo(other: Distance): Int = this.kilometers.compareTo(other.kilometers)
}

public val Number.kilometers: Distance get() = Distance(toDouble())
public val Number.meters: Distance get() = Distance(toDouble() / 1000)

public operator fun Distance.div(other: Distance): Double = kilometers / other.kilometers

public operator fun Distance.plus(other: Distance): Distance = Distance(kilometers + other.kilometers)
public operator fun Distance.minus(other: Distance): Distance = Distance(kilometers - other.kilometers)

public operator fun Distance.times(number: Number): Distance = Distance(kilometers * number.toDouble())
public operator fun Distance.div(number: Number): Distance = Distance(kilometers / number.toDouble())

public val Distance.meters: Double get() = kilometers * 1000