package space.kscience.maps.geojson

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import space.kscience.maps.coordinates.Gmc
import space.kscience.maps.features.Feature
import space.kscience.maps.features.FeatureBuilder
import space.kscience.maps.features.FeatureRef
import java.net.URL

/**
 * Add geojson features from url
 */
public fun FeatureBuilder<Gmc>.geoJson(
    geoJsonUrl: URL,
    id: String? = null,
): FeatureRef<Gmc, Feature<Gmc>> {
    val jsonString = geoJsonUrl.readText()
    val json = Json.parseToJsonElement(jsonString).jsonObject
    val geoJson = GeoJson(json)

    return geoJson(geoJson, id)
}