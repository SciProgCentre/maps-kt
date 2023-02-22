package center.sciprog.maps.features

import center.sciprog.attributes.Attributes


public fun <T : Any> FeatureGroup<T>.draggableLine(
    aId: FeatureRef<T, MarkerFeature<T>>,
    bId: FeatureRef<T, MarkerFeature<T>>,
    id: String? = null,
): FeatureRef<T, LineFeature<T>> {
    var lineId: FeatureRef<T, LineFeature<T>>? = null

    fun drawLine(): FeatureRef<T, LineFeature<T>> {
        val currentId = feature(
            lineId?.id ?: id,
            LineFeature(
                space,
                aId.resolve().center,
                bId.resolve().center,
                Attributes {
                    ZAttribute(-10f)
                    lineId?.attributes?.let { from(it) }
                }
            )
        )
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

public fun <T : Any> FeatureGroup<T>.draggableMultiLine(
    points: List<FeatureRef<T, MarkerFeature<T>>>,
    id: String? = null,
): FeatureRef<T, MultiLineFeature<T>> {
    var polygonId: FeatureRef<T, MultiLineFeature<T>>? = null

    fun drawLines(): FeatureRef<T, MultiLineFeature<T>> {
        val currentId = feature(
            polygonId?.id ?: id,
            MultiLineFeature(
                space,
                points.map { it.resolve().center },
                Attributes {
                    ZAttribute(-10f)
                    polygonId?.attributes?.let { from(it) }
                }
            )
        )
        polygonId = currentId
        return currentId
    }

    points.forEach {
        it.draggable { _, _ ->
            drawLines()
        }
    }

    return drawLines()
}