package center.sciprog.maps.features

import center.sciprog.attributes.Attributes
import center.sciprog.attributes.ZAttribute


public fun <T : Any> FeatureGroup<T>.draggableLine(
    aId: FeatureId<MarkerFeature<T>>,
    bId: FeatureId<MarkerFeature<T>>,
    id: String? = null,
): FeatureId<LineFeature<T>> {
    var lineId: FeatureId<LineFeature<T>>? = null

    fun drawLine(): FeatureId<LineFeature<T>> {
        //save attributes before update
        val attributes: Attributes? = lineId?.let(::get)?.attributes
        val currentId = line(
            get(aId).center,
            get(bId).center,
            lineId?.id ?: id
        )
        currentId.modifyAttributes {
            ZAttribute(-10f)
            if (attributes != null) from(attributes)
        }
        lineId = currentId
        return currentId
    }

    aId.draggable { _, _ ->
        drawLine()
    }

    bId.draggable { _, _ ->
        drawLine()
    }

    return drawLine()
}