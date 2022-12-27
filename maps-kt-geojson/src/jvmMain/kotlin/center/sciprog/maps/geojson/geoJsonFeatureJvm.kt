package center.sciprog.maps.geojson

import center.sciprog.maps.coordinates.Gmc
import center.sciprog.maps.features.FeatureBuilder
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import java.net.URL

/**
 * Add geojson features from url
 */
public fun FeatureBuilder<Gmc>.geoJson(
    geoJsonUrl: URL,
    id: String? = null,
) {
    val jsonString = geoJsonUrl.readText()
    val json = Json.parseToJsonElement(jsonString).jsonObject
    val geoJson = GeoJson(json)

    geoJson(geoJson, id)
}