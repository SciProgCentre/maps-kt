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
): FeatureId<*> = when (geometry) {
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
            points(
                it,
                color = color,
                pointMode = PointMode.Polygon
            )
        }
    }

    is GeoJsonPoint -> circle(geometry.coordinates, color = color, id = id)
    is GeoJsonPolygon -> points(
        geometry.coordinates,
        color = color,
        pointMode = PointMode.Polygon
    )

    is GeoJsonGeometryCollection -> group(id = id) {
        geometry.geometries.forEach {
            geoJsonGeometry(it)
        }
    }
}

public fun FeatureBuilder<Gmc>.geoJsonFeature(
    geoJson: GeoJsonFeature,
    color: Color = defaultColor,
    id: String? = null,
) {
    val geometry = geoJson.geometry ?: return
    val idOverride = geoJson.properties?.get("id")?.jsonPrimitive?.contentOrNull ?: id
    val colorOverride = geoJson.properties?.get("color")?.jsonPrimitive?.intOrNull?.let { Color(it) } ?: color
    geoJsonGeometry(geometry, colorOverride, idOverride)
}

public fun FeatureBuilder<Gmc>.geoJson(
    geoJson: GeoJsonFeatureCollection,
    id: String? = null,
): FeatureId<FeatureGroup<Gmc>> = group(id = id) {
    geoJson.features.forEach {
        geoJsonFeature(it)
    }
}