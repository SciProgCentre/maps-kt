package space.kscience.maps.geojson

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonObject


public object GeoJsonSerializer : KSerializer<GeoJson> {

    private val serializer = JsonObject.serializer()
    override val descriptor: SerialDescriptor
        get() = serializer.descriptor

    override fun deserialize(decoder: Decoder): GeoJson = GeoJson(serializer.deserialize(decoder))

    override fun serialize(encoder: Encoder, value: GeoJson) {
        serializer.serialize(encoder, value.json)
    }
}