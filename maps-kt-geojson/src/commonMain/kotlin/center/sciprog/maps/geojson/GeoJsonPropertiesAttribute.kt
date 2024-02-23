package center.sciprog.maps.geojson

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.serializer
import space.kscience.SerializableAttribute

public object GeoJsonPropertiesAttribute : SerializableAttribute<JsonObject>("properties", serializer())