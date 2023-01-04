package center.sciprog.maps.features


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
        if (attributes != null) currentId.modifyAttributes { attributes.withAttribute(ZAttribute, -10f) }
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