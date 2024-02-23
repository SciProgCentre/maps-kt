package center.sciprog.maps.geojson

import androidx.compose.ui.graphics.Color
import center.sciprog.maps.coordinates.Gmc
import center.sciprog.maps.features.*
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive
import space.kscience.NameAttribute


/**
 * Add a single Json geometry to a feature builder
 */
public fun FeatureGroup<Gmc>.geoJsonGeometry(
    geometry: GeoJsonGeometry,
    id: String? = null,
): FeatureRef<Gmc, Feature<Gmc>> = when (geometry) {
    is GeoJsonLineString -> multiLine(
        geometry.coordinates,
    )

    is GeoJsonMultiLineString -> group(id = id) {
        geometry.coordinates.forEach {
            multiLine(it)
        }
    }

    is GeoJsonMultiPoint -> points(
        geometry.coordinates,
    )

    is GeoJsonMultiPolygon -> group(id = id) {
        geometry.coordinates.forEach {
            polygon(
                it.first(),
            )
        }
    }

    is GeoJsonPoint -> circle(geometry.coordinates, id = id)
    is GeoJsonPolygon -> polygon(
        geometry.coordinates.first(),
    )

    is GeoJsonGeometryCollection -> group(id = id) {
        geometry.geometries.forEach {
            geoJsonGeometry(it)
        }
    }
}

public fun FeatureGroup<Gmc>.geoJsonFeature(
    geoJson: GeoJsonFeature,
    id: String? = null,
): FeatureRef<Gmc, Feature<Gmc>> {
    val geometry = geoJson.geometry ?: return group {}
    val idOverride = id ?: geoJson.getProperty("id")?.jsonPrimitive?.contentOrNull

    return geoJsonGeometry(geometry, idOverride).modifyAttributes {
        geoJson.properties?.let {
            GeoJsonPropertiesAttribute(it)
        }

        geoJson.getProperty("name")?.jsonPrimitive?.contentOrNull?.let {
            NameAttribute(it)
        }

        geoJson.getProperty("color")?.jsonPrimitive?.intOrNull?.let {
            ColorAttribute(Color(it))
        }
    }
}

public fun FeatureGroup<Gmc>.geoJson(
    geoJson: GeoJson,
    id: String? = null,
): FeatureRef<Gmc, Feature<Gmc>> = when (geoJson) {
    is GeoJsonFeature -> geoJsonFeature(geoJson, id = id)
    is GeoJsonFeatureCollection -> group(id = id) {
        geoJson.features.forEach {
            geoJsonFeature(it)
        }
    }

    is GeoJsonGeometry -> geoJsonGeometry(geoJson, id = id)
}