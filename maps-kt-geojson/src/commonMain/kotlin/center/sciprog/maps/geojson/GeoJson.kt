package center.sciprog.maps.geojson

import center.sciprog.maps.geojson.GeoJson.Companion.PROPERTIES_KEY
import center.sciprog.maps.geojson.GeoJson.Companion.TYPE_KEY
import center.sciprog.maps.geojson.GeoJsonFeatureCollection.Companion.FEATURES_KEY
import kotlinx.serialization.json.*
import kotlin.jvm.JvmInline

/**
 * A utility class to work with GeoJson (https://geojson.org/)
 */
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

    public fun getProperty(propertyName: String): JsonElement? = properties?.get(propertyName)

    public fun getString(propertyName: String): String? = getProperty(propertyName)?.jsonPrimitive?.contentOrNull

    public companion object{
        public const val GEOMETRY_KEY: String = "geometry"
    }
}

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
 * Combine a collection of features to a new [GeoJsonFeatureCollection]
 */
public fun GeoJsonFeatureCollection(features: Collection<GeoJsonFeature>): GeoJsonFeatureCollection =
    GeoJsonFeatureCollection(
        buildJsonObject {
            put(TYPE_KEY, "FeatureCollection")
            put(FEATURES_KEY, JsonArray(features.map { it.json }))
        }
    )