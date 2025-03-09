/*
 * Copyright 2018-2022 KMath contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
@file:UseSerializers(Float64Space2D.VectorSerializer::class)

package space.kscience.trajectory

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import space.kscience.kmath.geometry.*
import space.kscience.kmath.geometry.euclidean2d.Float64Space2D
import space.kscience.kmath.structures.Float64
import kotlin.math.atan2

/**
 * Combination of [Vector] and its view angle (clockwise from positive y-axis direction)
 */
@Serializable(Pose2DSerializer::class)
public interface Pose2D : Vector2D<Float64> {
    public val coordinates: Vector2D<Float64>
    public val bearing: Angle

    /**
     * Reverse the direction of this pose to the opposite, keeping other parameters the same
     */
    public fun reversed(): Pose2D

    public companion object {
        public fun bearingToVector(bearing: Angle): Vector2D<Double> =
            Float64Space2D.vector(cos(bearing), sin(bearing))

        public fun vectorToBearing(vector2D: Vector2D<Float64>): Angle {
            require(vector2D.x != 0.0 || vector2D.y != 0.0) { "Can't get bearing of zero vector" }
            return atan2(vector2D.y, vector2D.x).radians
        }
    }
}


@Serializable
public class PhaseVector2D(
    override val coordinates: Vector2D<Float64>,
    public val velocity: Vector2D<Float64>,
) : Pose2D, Vector2D<Float64> by coordinates {
    override val bearing: Angle get() = atan2(velocity.x, velocity.y).radians

    override fun reversed(): Pose2D = with(Float64Space2D) { PhaseVector2D(coordinates, -velocity) }
}

@Serializable
@SerialName("DubinsPose2D")
private class Pose2DImpl(
    override val coordinates: Vector2D<Float64>,
    override val bearing: Angle,
) : Pose2D, Vector2D<Float64> by coordinates {

    override fun reversed(): Pose2D = Pose2DImpl(coordinates, bearing.plus(Angle.pi).normalized())


    override fun toString(): String = "Pose2D(x=$x, y=$y, bearing=$bearing)"
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as Pose2DImpl

        if (coordinates != other.coordinates) return false
        return bearing == other.bearing
    }

    override fun hashCode(): Int {
        var result = coordinates.hashCode()
        result = 31 * result + bearing.hashCode()
        return result
    }
}

public object Pose2DSerializer : KSerializer<Pose2D> {
    private val proxySerializer = Pose2DImpl.serializer()

    override val descriptor: SerialDescriptor
        get() = proxySerializer.descriptor

    override fun deserialize(decoder: Decoder): Pose2D {
        return decoder.decodeSerializableValue(proxySerializer)
    }

    override fun serialize(encoder: Encoder, value: Pose2D) {
        val pose = value as? Pose2DImpl ?: Pose2DImpl(value.coordinates, value.bearing)
        encoder.encodeSerializableValue(proxySerializer, pose)
    }
}

public fun Pose2D(coordinate: Vector2D<Float64>, bearing: Angle): Pose2D =
    Pose2DImpl(coordinate, bearing)

public fun Pose2D(point: Vector2D<Float64>, direction: Vector2D<Float64>): Pose2D =
    Pose2D(point, Pose2D.vectorToBearing(direction))

public fun Pose2D(x: Number, y: Number, bearing: Angle): Pose2D =
    Pose2DImpl(Float64Space2D.vector(x, y), bearing)