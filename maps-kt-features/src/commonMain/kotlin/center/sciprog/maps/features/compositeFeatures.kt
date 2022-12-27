package center.sciprog.maps.features

import androidx.compose.ui.graphics.Color


public fun <T : Any> FeatureCollection<T>.draggableLine(
    aId: FeatureId<MarkerFeature<T>>,
    bId: FeatureId<MarkerFeature<T>>,
    zoomRange: FloatRange = defaultZoomRange,
    color: Color = Color.Red,
    id: String? = null,
): FeatureId<LineFeature<T>> {
    var lineId: FeatureId<LineFeature<T>>? = null

    fun drawLine(): FeatureId<LineFeature<T>> = line(
        get(aId).center,
        get(bId).center,
        zoomRange,
        color,
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