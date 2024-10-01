package space.kscience.maps.svg

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.painter.Painter
import org.jfree.svg.SVGGraphics2D
import space.kscience.attributes.Attributes
import space.kscience.attributes.plus
import space.kscience.maps.features.*


public class FeatureSetSnapshot<T : Any>(
    public val features: Map<String, Feature<T>>,
    internal val painterCache: Map<PainterFeature<T>, Painter>,
)

@Composable
public fun <T : Any> FeatureSet<T>.snapshot(): FeatureSetSnapshot<T> = FeatureSetSnapshot(
    features,
    features.values.filterIsInstance<PainterFeature<T>>().associateWith { it.getPainter() }
)


public fun <T : Any> FeatureSetSnapshot<T>.generateSvg(
    canvasState: CanvasState<T>,
    id: String? = null,
): String {
    val svgGraphics2D: SVGGraphics2D = SVGGraphics2D(
        canvasState.canvasSize.width.value.toDouble(),
        canvasState.canvasSize.height.value.toDouble()
    )
    val svgScope = SvgDrawScope(canvasState, svgGraphics2D, painterCache)

    svgScope.apply {
        features.entries.sortedBy { it.value.z }
            .filter { state.viewPoint.zoom in it.value.zoomRange }
            .forEach { (id, feature) ->
                val attributesCache = mutableMapOf<List<String>, Attributes>()

                fun computeGroupAttributes(path: List<String>): Attributes = attributesCache.getOrPut(path) {
                    if (path.isEmpty()) return Attributes.EMPTY
                    else if (path.size == 1) {
                        features[path.first()]?.attributes ?: Attributes.EMPTY
                    } else {
                        computeGroupAttributes(path.dropLast(1)) + (features[path.first()]?.attributes
                            ?: Attributes.EMPTY)
                    }
                }

                val path = id.split("/")
                drawFeature(feature, computeGroupAttributes(path.dropLast(1)))
            }
    }
    return svgGraphics2D.getSVGElement(id)
}