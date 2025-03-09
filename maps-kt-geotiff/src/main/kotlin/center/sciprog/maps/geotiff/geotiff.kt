package center.sciprog.maps.geotiff

import space.kscience.maps.coordinates.Gmc
import space.kscience.maps.features.Feature
import space.kscience.maps.features.FeatureGroup
import space.kscience.maps.features.FeatureRef
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import org.geotools.gce.geotiff.GeoTiffReader
import org.geotools.util.factory.Hints
import java.io.File
import java.net.URL


public fun FeatureGroup<Gmc>.geoJson(
    geoTiffUrl: URL,
    id: String? = null,
): FeatureRef<Gmc, Feature<Gmc>> {
    val reader = GeoTiffReader
    val jsonString = geoJsonUrl.readText()
    val json = Json.parseToJsonElement(jsonString).jsonObject
    val geoJson = GeoJson(json)

    return geoJson(geoJson, id)
}

