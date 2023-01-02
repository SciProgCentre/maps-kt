package center.sciprog.maps.features


public fun <T : Any> FeatureGroup<T>.draggableLine(
    aId: FeatureId<MarkerFeature<T>>,
    bId: FeatureId<MarkerFeature<T>>,
    id: String? = null,
): FeatureId<LineFeature<T>> {
    var lineId: FeatureId<LineFeature<T>>? = null

    fun drawLine(): FeatureId<LineFeature<T>> = line(
        get(aId).center,
        get(bId).center,
        lineId?.id ?: id
    ).also {
        lineId = it
    }

    aId.draggable { _, _ ->
        drawLine()
    }

    bId.draggable { _, _ ->
        drawLine()
    }

    return drawLine()
}