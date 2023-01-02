package center.sciprog.maps.geojson

import center.sciprog.maps.coordinates.Gmc
import center.sciprog.maps.features.Feature
import center.sciprog.maps.features.FeatureGroup
import center.sciprog.maps.features.FeatureId
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import java.net.URL

/**
 * Add geojson features from url
 */
public fun FeatureGroup<Gmc>.geoJson(
    geoJsonUrl: URL,
    id: String? = null,
): FeatureId<Feature<Gmc>> {
    val jsonString = geoJsonUrl.readText()
    val json = Json.parseToJsonElement(jsonString).jsonObject
    val geoJson = GeoJson(json)

    return geoJson(geoJson, id)
}