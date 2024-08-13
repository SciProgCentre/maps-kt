package space.kscience.maps.geojson

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.serializer
import space.kscience.attributes.SerializableAttribute

public object GeoJsonPropertiesAttribute : SerializableAttribute<JsonObject>("properties", serializer())