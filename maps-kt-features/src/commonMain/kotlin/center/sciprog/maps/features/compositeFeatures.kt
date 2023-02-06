package center.sciprog.maps.features

import center.sciprog.attributes.Attributes


public fun <T : Any> FeatureGroup<T>.draggableLine(
    aId: FeatureRef<T, MarkerFeature<T>>,
    bId: FeatureRef<T, MarkerFeature<T>>,
    id: String? = null,
): FeatureRef<T, LineFeature<T>> {
    var lineId: FeatureRef<T, LineFeature<T>>? = null

    fun drawLine(): FeatureRef<T, LineFeature<T>> {
        //save attributes before update
        val attributes: Attributes? = lineId?.attributes
        val currentId = line(
            aId.resolve().center,
            bId.resolve().center,
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