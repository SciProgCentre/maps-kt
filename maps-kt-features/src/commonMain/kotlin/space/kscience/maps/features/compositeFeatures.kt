package space.kscience.maps.features

import space.kscience.attributes.Attributes
import kotlin.jvm.JvmName


public fun <T : Any> FeatureBuilder<T>.draggableLine(
    aId: FeatureRef<T, MarkerFeature<T>>,
    bId: FeatureRef<T, MarkerFeature<T>>,
    id: String? = null,
): FeatureRef<T, LineFeature<T>> {
    val lineId = id ?: FeatureStore.generateFeatureId<LineFeature<*>>()

    fun drawLine(): FeatureRef<T, LineFeature<T>> = updateFeature(lineId) { old ->
        LineFeature(
            space,
            aId.resolve().center,
            bId.resolve().center,
            old?.attributes ?: Attributes(ZAttribute, -10f)
        )
    }

    aId.draggable { _, _ ->
        drawLine()
    }

    bId.draggable { _, _ ->
        drawLine()
    }

    return drawLine()
}

public fun <T : Any> FeatureBuilder<T>.draggableMultiLine(
    points: List<FeatureRef<T, MarkerFeature<T>>>,
    id: String? = null,
): FeatureRef<T, MultiLineFeature<T>> {
    val polygonId = id ?: FeatureStore.generateFeatureId("multiline")

    fun drawLines(): FeatureRef<T, MultiLineFeature<T>> = updateFeature(polygonId) { old ->
        MultiLineFeature(
            space,
            points.map { it.resolve().center },
            old?.attributes ?: Attributes(ZAttribute, -10f)
        )
    }

    points.forEach {
        it.draggable { _, _ ->
            drawLines()
        }
    }

    return drawLines()
}

@JvmName("draggableMultiLineFromPoints")
public fun <T : Any> FeatureBuilder<T>.draggableMultiLine(
    points: List<T>,
    id: String? = null,
): FeatureRef<T, MultiLineFeature<T>> {
    val pointRefs = points.map {
        circle(it)
    }
    return draggableMultiLine(pointRefs, id)
}