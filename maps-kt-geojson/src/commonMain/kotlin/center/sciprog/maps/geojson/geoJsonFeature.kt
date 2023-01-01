package center.sciprog.maps.geojson

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PointMode
import center.sciprog.maps.coordinates.Gmc
import center.sciprog.maps.features.*
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive


/**
 * Add a single Json geometry to a feature builder
 */
public fun FeatureBuilder<Gmc>.geoJsonGeometry(
    geometry: GeoJsonGeometry,
    color: Color = defaultColor,
    id: String? = null,
): FeatureId<Feature<Gmc>> = when (geometry) {
    is GeoJsonLineString -> points(
        geometry.coordinates,
        color = color,
        pointMode = PointMode.Lines
    )

    is GeoJsonMultiLineString -> group(id = id) {
        geometry.coordinates.forEach {
            points(
                it,
                color = color,
                pointMode = PointMode.Lines
            )
        }
    }

    is GeoJsonMultiPoint -> points(
        geometry.coordinates,
        color = color,
        pointMode = PointMode.Points
    )

    is GeoJsonMultiPolygon -> group(id = id) {
        geometry.coordinates.forEach {
            polygon(
                it.first(),
                color = color,
            )
        }
    }

    is GeoJsonPoint -> circle(geometry.coordinates, color = color, id = id)
    is GeoJsonPolygon -> polygon(
        geometry.coordinates.first(),
        color = color,
    )

    is GeoJsonGeometryCollection -> group(id = id) {
        geometry.geometries.forEach {
            geoJsonGeometry(it)
        }
    }
}.apply {
    withAttribute(AlphaAttribute, 0.5f)
}

public fun FeatureBuilder<Gmc>.geoJsonFeature(
    geoJson: GeoJsonFeature,
    color: Color = defaultColor,
    id: String? = null,
): FeatureId<Feature<Gmc>>? {
    val geometry = geoJson.geometry ?: return null
    val idOverride = geoJson.properties?.get("id")?.jsonPrimitive?.contentOrNull ?: id
    val colorOverride = geoJson.properties?.get("color")?.jsonPrimitive?.intOrNull?.let { Color(it) } ?: color
    return geoJsonGeometry(geometry, colorOverride, idOverride)
}

public fun FeatureBuilder<Gmc>.geoJson(
    geoJson: GeoJson,
    id: String? = null,
): FeatureId<Feature<Gmc>>? = when (geoJson) {
    is GeoJsonFeature -> geoJsonFeature(geoJson, id = id)
    is GeoJsonFeatureCollection -> group(id = id) {
        geoJson.features.forEach {
            geoJsonFeature(it)
        }
    }

    is GeoJsonGeometry -> geoJsonGeometry(geoJson, id = id)
}