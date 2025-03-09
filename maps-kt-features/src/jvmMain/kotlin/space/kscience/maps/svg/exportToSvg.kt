package space.kscience.maps.svg

import androidx.compose.ui.graphics.painter.Painter
import org.jfree.svg.SVGGraphics2D
import space.kscience.attributes.Attributes
import space.kscience.attributes.plus
import space.kscience.maps.features.*


public fun <T : Any> FeatureDrawScope<T>.features(featureSet: FeatureSet<T>) {
    featureSet.features.entries.sortedBy { it.value.z }
        .filter { state.viewPoint.zoom in it.value.zoomRange }
        .forEach { (id, feature) ->
            val attributesCache = mutableMapOf<List<String>, Attributes>()

            fun computeGroupAttributes(path: List<String>): Attributes = attributesCache.getOrPut(path) {
                if (path.isEmpty()) return Attributes.EMPTY
                else if (path.size == 1) {
                    featureSet.features[path.first()]?.attributes ?: Attributes.EMPTY
                } else {
                    computeGroupAttributes(path.dropLast(1)) +
                            (featureSet.features[path.first()]?.attributes ?: Attributes.EMPTY)
                }
            }

            val path = id.split("/")
            drawFeature(feature, computeGroupAttributes(path.dropLast(1)))
        }
}

public fun <T : Any> FeatureSet<T>.generateSvg(
    canvasState: CanvasState<T>,
    painterCache: Map<PainterFeature<T>, Painter>,
    id: String? = null,
): String {
    val svgGraphics2D: SVGGraphics2D = SVGGraphics2D(
        canvasState.canvasSize.width.value.toDouble(),
        canvasState.canvasSize.height.value.toDouble()
    )
    val svgScope = SvgDrawScope(canvasState, svgGraphics2D, painterCache)

    svgScope.features(this)

    return svgGraphics2D.getSVGElement(id)
}