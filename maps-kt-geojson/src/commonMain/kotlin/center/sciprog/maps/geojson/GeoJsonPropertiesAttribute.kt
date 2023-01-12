package center.sciprog.maps.geojson

import center.sciprog.attributes.SerializableAttribute
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.serializer

public object GeoJsonPropertiesAttribute : SerializableAttribute<JsonObject>("properties", serializer())