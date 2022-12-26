package center.sciprog.maps.geojson

import center.sciprog.maps.coordinates.Gmc
import center.sciprog.maps.geojson.GeoJsonGeometry.Companion.COORDINATES_KEY
import kotlinx.serialization.json.*
import kotlin.jvm.JvmInline

public sealed interface GeoJsonGeometry : GeoJson {
    public companion object {
        public const val COORDINATES_KEY: String = "coordinates"
    }
}

public fun GeoJsonGeometry(json: JsonObject): GeoJsonGeometry {
    return when (val type = json[GeoJson.TYPE_KEY]?.jsonPrimitive?.content ?: error("Not a GeoJson object")) {
        "Point" -> GeoJsonPoint(json)
        "MultiPoint" -> GeoJsonMultiPoint(json)
        "LineString" -> GeoJsonLineString(json)
        "MultiLineString" -> GeoJsonMultiLineString(json)
        "Polygon" -> GeoJsonPolygon(json)
        "MultiPolygon" -> GeoJsonMultiPolygon(json)
        "GeometryCollection" -> GeoJsonGeometryCollection(json)
        else -> error("Type '$type' is not recognised as a geometry type")
    }
}

internal fun JsonElement.toGmc() = jsonArray.run {
    Gmc.ofDegrees(get(1).jsonPrimitive.double, get(0).jsonPrimitive.double)
}

@JvmInline
public value class GeoJsonPoint(override val json: JsonObject) : GeoJsonGeometry {
    init {
        require(type == "Point") { "Not a GeoJson Point geometry" }
    }

    public val coordinates: Gmc
        get() = json[COORDINATES_KEY]?.toGmc()
            ?: error("Coordinates are not provided")
}

@JvmInline
public value class GeoJsonMultiPoint(override val json: JsonObject) : GeoJsonGeometry {
    init {
        require(type == "MultiPoint") { "Not a GeoJson MultiPoint geometry" }
    }

    public val coordinates: List<Gmc>
        get() = json[COORDINATES_KEY]?.jsonArray
            ?.map { it.toGmc() }
            ?: error("Coordinates are not provided")
}

@JvmInline
public value class GeoJsonLineString(override val json: JsonObject) : GeoJsonGeometry {
    init {
        require(type == "LineString") { "Not a GeoJson LineString geometry" }
    }

    public val coordinates: List<Gmc>
        get() = json[COORDINATES_KEY]?.jsonArray
            ?.map { it.toGmc() }
            ?: error("Coordinates are not provided")
}

@JvmInline
public value class GeoJsonMultiLineString(override val json: JsonObject) : GeoJsonGeometry {
    init {
        require(type == "MultiLineString") { "Not a GeoJson MultiLineString geometry" }
    }

    public val coordinates: List<List<Gmc>>
        get() = json[COORDINATES_KEY]?.jsonArray?.map { lineJson ->
            lineJson.jsonArray.map {
                it.toGmc()
            }
        } ?: error("Coordinates are not provided")
}

@JvmInline
public value class GeoJsonPolygon(override val json: JsonObject) : GeoJsonGeometry {
    init {
        require(type == "Polygon") { "Not a GeoJson Polygon geometry" }
    }

    public val coordinates: List<Gmc>
        get() = json[COORDINATES_KEY]?.jsonArray
            ?.map { it.toGmc() }
            ?: error("Coordinates are not provided")
}

@JvmInline
public value class GeoJsonMultiPolygon(override val json: JsonObject) : GeoJsonGeometry {
    init {
        require(type == "MultiPolygon") { "Not a GeoJson MultiPolygon geometry" }
    }

    public val coordinates: List<List<Gmc>>
        get() = json[COORDINATES_KEY]?.jsonArray?.map { lineJson ->
            lineJson.jsonArray.map {
                it.toGmc()
            }
        } ?: error("Coordinates are not provided")
}

@JvmInline
public value class GeoJsonGeometryCollection(override val json: JsonObject) : GeoJsonGeometry {
    init {
        require(type == "GeometryCollection") { "Not a GeoJson GeometryCollection geometry" }
    }

    public val geometries: List<GeoJsonGeometry> get() = json.jsonArray.map { GeoJsonGeometry(it.jsonObject) }
}