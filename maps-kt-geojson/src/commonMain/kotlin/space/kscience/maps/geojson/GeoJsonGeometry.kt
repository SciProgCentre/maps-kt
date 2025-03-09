package space.kscience.maps.geojson

import kotlinx.serialization.json.*
import space.kscience.maps.coordinates.Gmc
import space.kscience.maps.coordinates.meters
import space.kscience.maps.geojson.GeoJsonGeometry.Companion.COORDINATES_KEY
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
    Gmc.ofDegrees(
        get(1).jsonPrimitive.double,
        get(0).jsonPrimitive.double,
        getOrNull(2)?.jsonPrimitive?.doubleOrNull?.meters
    )
}

internal fun Gmc.toJsonArray(): JsonArray = buildJsonArray {
    add(longitude.toDegrees().value)
    add(latitude.toDegrees().value)
    elevation?.let {
        add(it.meters)
    }
}

private fun List<Gmc>.listToJsonArray(): JsonArray = buildJsonArray {
    forEach {
        add(it.toJsonArray())
    }
}

private fun List<List<Gmc>>.listOfListsToJsonArray(): JsonArray = buildJsonArray {
    forEach {
        add(it.listToJsonArray())
    }
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

public fun GeoJsonPoint(
    coordinates: Gmc,
    modification: JsonObjectBuilder.() -> Unit = {},
): GeoJsonPoint = GeoJsonPoint(
    buildJsonObject {
        put(GeoJson.TYPE_KEY, "Point")
        put(COORDINATES_KEY, coordinates.toJsonArray())
        modification()
    }
)

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

public fun GeoJsonMultiPoint(
    coordinates: List<Gmc>,
    modification: JsonObjectBuilder.() -> Unit = {},
): GeoJsonMultiPoint = GeoJsonMultiPoint(
    buildJsonObject {
        put(GeoJson.TYPE_KEY, "MultiPoint")
        put(COORDINATES_KEY, coordinates.listToJsonArray())
        modification()
    }
)

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

public fun GeoJsonLineString(
    coordinates: List<Gmc>,
    modification: JsonObjectBuilder.() -> Unit = {},
): GeoJsonLineString = GeoJsonLineString(
    buildJsonObject {
        put(GeoJson.TYPE_KEY, "LineString")
        put(COORDINATES_KEY, coordinates.listToJsonArray())
        modification()
    }
)

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

public fun GeoJsonMultiLineString(
    coordinates: List<List<Gmc>>,
    modification: JsonObjectBuilder.() -> Unit = {},
): GeoJsonMultiLineString = GeoJsonMultiLineString(
    buildJsonObject {
        put(GeoJson.TYPE_KEY, "MultiLineString")
        put(COORDINATES_KEY, coordinates.listOfListsToJsonArray())
        modification()
    }
)

@JvmInline
public value class GeoJsonPolygon(override val json: JsonObject) : GeoJsonGeometry {
    init {
        require(type == "Polygon") { "Not a GeoJson Polygon geometry" }
    }

    public val coordinates: List<List<Gmc>>
        get() = json[COORDINATES_KEY]?.jsonArray?.map { polygon ->
            polygon.jsonArray.map { point ->
                point.toGmc()
            }
        } ?: error("Coordinates are not provided")
}

public fun GeoJsonPolygon(
    coordinates: List<List<Gmc>>,
    modification: JsonObjectBuilder.() -> Unit
): GeoJsonPolygon = GeoJsonPolygon(
    buildJsonObject {
        put(GeoJson.TYPE_KEY, "Polygon")
        put(COORDINATES_KEY, coordinates.listOfListsToJsonArray())
        modification()
    }
)

@JvmInline
public value class GeoJsonMultiPolygon(override val json: JsonObject) : GeoJsonGeometry {
    init {
        require(type == "MultiPolygon") { "Not a GeoJson MultiPolygon geometry" }
    }

    public val coordinates: List<List<List<Gmc>>>
        get() = json[COORDINATES_KEY]?.jsonArray?.map { allPolygons ->
            allPolygons.jsonArray.map { polygon ->
                polygon.jsonArray.map { point ->
                    point.toGmc()
                }
            }
        } ?: error("Coordinates are not provided")
}

public fun GeoJsonMultiPolygon(
    coordinates: List<List<List<Gmc>>>,
    modification: JsonObjectBuilder.() -> Unit
): GeoJsonMultiPolygon = GeoJsonMultiPolygon(
    buildJsonObject {
        put(GeoJson.TYPE_KEY, "MultiPolygon")
        put(COORDINATES_KEY, buildJsonArray { coordinates.forEach { add(it.listOfListsToJsonArray()) } })
        modification()
    }
)

@JvmInline
public value class GeoJsonGeometryCollection(override val json: JsonObject) : GeoJsonGeometry {
    init {
        require(type == "GeometryCollection") { "Not a GeoJson GeometryCollection geometry" }
    }

    public val geometries: List<GeoJsonGeometry>
        get() = json["geometries"]?.jsonArray?.map { GeoJsonGeometry(it.jsonObject) } ?: emptyList()
}

public fun GeoJsonGeometryCollection(
    geometries: List<GeoJsonGeometry>,
    modification: JsonObjectBuilder.() -> Unit
): GeoJsonGeometryCollection =
    GeoJsonGeometryCollection(
        buildJsonObject {
            put(GeoJson.TYPE_KEY, "GeometryCollection")
            put("geometries", buildJsonArray {
                geometries.forEach {
                    add(it.json)
                }
            })
            modification()
        }
    )