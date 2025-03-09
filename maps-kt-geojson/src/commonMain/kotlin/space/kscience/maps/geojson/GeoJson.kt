package space.kscience.maps.geojson

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.*
import space.kscience.maps.geojson.GeoJson.Companion.PROPERTIES_KEY
import space.kscience.maps.geojson.GeoJson.Companion.TYPE_KEY
import space.kscience.maps.geojson.GeoJsonFeatureCollection.Companion.FEATURES_KEY
import kotlin.jvm.JvmInline

/**
 * A utility class to work with GeoJson (https://geojson.org/)
 */
@Serializable(GeoJsonSerializer::class)
public sealed interface GeoJson {
    public val json: JsonObject
    public val type: String get() = json[TYPE_KEY]?.jsonPrimitive?.content ?: error("Not a GeoJson")

    public companion object {
        public const val TYPE_KEY: String = "type"
        public const val PROPERTIES_KEY: String = "properties"
    }
}

@JvmInline
public value class GeoJsonFeature(override val json: JsonObject) : GeoJson {
    init {
        require(type == "Feature") { "Not a GeoJson Feature" }
    }

    public val properties: JsonObject? get() = json[PROPERTIES_KEY]?.jsonObject

    public val geometry: GeoJsonGeometry? get() = json[GEOMETRY_KEY]?.jsonObject?.let { GeoJsonGeometry(it) }

    public companion object {
        public const val GEOMETRY_KEY: String = "geometry"
    }
}

/**
 * A builder function for [GeoJsonFeature]
 */
public fun GeoJsonFeature(
    geometry: GeoJsonGeometry?,
    properties: JsonObject? = null,
    builder: JsonObjectBuilder.() -> Unit = {},
): GeoJsonFeature = GeoJsonFeature(
    buildJsonObject {
        put(TYPE_KEY, "Feature")
        geometry?.json?.let { put(GeoJsonFeature.GEOMETRY_KEY, it) }
        properties?.let { put(PROPERTIES_KEY, it) }

        builder()
    }
)

public fun GeoJsonFeature.getProperty(key: String): JsonElement? = json[key] ?: properties?.get(key)

@JvmInline
public value class GeoJsonFeatureCollection(override val json: JsonObject) : GeoJson, Iterable<GeoJsonFeature> {
    init {
        require(type == "FeatureCollection") { "Not a GeoJson FeatureCollection" }
    }

    public val properties: JsonObject? get() = json[PROPERTIES_KEY]?.jsonObject

    public val features: List<GeoJsonFeature>
        get() = json[FEATURES_KEY]?.jsonArray?.map {
            GeoJsonFeature(it.jsonObject)
        } ?: error("Features not defined in GeoJson features collection")

    override fun iterator(): Iterator<GeoJsonFeature> = features.iterator()

    public companion object {

        public const val FEATURES_KEY: String = "features"
        public fun parse(string: String): GeoJsonFeatureCollection = GeoJsonFeatureCollection(
            Json.parseToJsonElement(string).jsonObject
        )
    }
}

/**
 * A builder for [GeoJsonFeatureCollection]
 */
public fun GeoJsonFeatureCollection(
    features: List<GeoJsonFeature>,
    properties: JsonObject? = null,
    builder: JsonObjectBuilder.() -> Unit = {},
): GeoJsonFeatureCollection = GeoJsonFeatureCollection(
    buildJsonObject {
        put(TYPE_KEY, "FeatureCollection")
        putJsonArray(FEATURES_KEY) {
            features.forEach {
                add(it.json)
            }
        }
        properties?.let { put(PROPERTIES_KEY, it) }
        builder()
    }
)


/**
 * Generic Json to GeoJson converter
 */
public fun GeoJson(json: JsonObject): GeoJson =
    when (json[TYPE_KEY]?.jsonPrimitive?.contentOrNull ?: error("Not a GeoJson")) {
        "Feature" -> GeoJsonFeature(json)
        "FeatureCollection" -> GeoJsonFeatureCollection(json)
        else -> GeoJsonGeometry(json)
    }