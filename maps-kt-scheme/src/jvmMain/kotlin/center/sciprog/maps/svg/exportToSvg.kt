package center.sciprog.maps.svg

import androidx.compose.runtime.Composable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.painter.Painter
import center.sciprog.maps.scheme.*
import org.jfree.svg.SVGGraphics2D
import org.jfree.svg.SVGUtils
import java.awt.Font.PLAIN
import kotlin.math.PI
import kotlin.math.abs


class FeatureStateSnapshot(
    val features: Map<FeatureId, SchemeFeature>,
    val painterCache: Map<PainterFeature, Painter>,
)

@Composable
fun SchemeFeaturesState.snapshot(): FeatureStateSnapshot =
    FeatureStateSnapshot(
        features(),
        features().values.filterIsInstance<PainterFeature>().associateWith { it.painter() })


fun FeatureStateSnapshot.exportToSvg(
    viewPoint: SchemeViewPoint,
    width: Double,
    height: Double,
    path: java.nio.file.Path,
) {

    fun SchemeCoordinates.toOffset(): Offset = Offset(
        (width / 2 + (x - viewPoint.focus.x) * viewPoint.scale).toFloat(),
        (height / 2 + (viewPoint.focus.y - y) * viewPoint.scale).toFloat()
    )


    fun SvgDrawScope.drawFeature(scale: Float, feature: SchemeFeature) {
        when (feature) {
            is SchemeBackgroundFeature -> {
                val offset = SchemeCoordinates(feature.rectangle.left, feature.rectangle.top).toOffset()
                val backgroundSize = Size(
                    (feature.rectangle.width * scale),
                    (feature.rectangle.height * scale)
                )

                translate(offset.x, offset.y) {
                    with(painterCache[feature]!!) {
                        draw(backgroundSize)
                    }
                }
            }

            is SchemeFeatureSelector -> drawFeature(scale, feature.selector(scale))
            is SchemeCircleFeature -> drawCircle(
                feature.color,
                feature.size,
                center = feature.center.toOffset()
            )

            is SchemeLineFeature -> drawLine(feature.color, feature.a.toOffset(), feature.b.toOffset())
            is SchemeArcFeature -> {
                val topLeft = feature.oval.leftTop.toOffset()
                val bottomRight = feature.oval.rightBottom.toOffset()

                val size = Size(abs(topLeft.x - bottomRight.x), abs(topLeft.y - bottomRight.y))

                drawArc(
                    color = feature.color,
                    startAngle = (feature.startAngle * 180 / PI).toFloat(),
                    sweepAngle = (feature.arcLength * 180 / PI).toFloat(),
                    useCenter = false,
                    topLeft = topLeft,
                    size = size,
                    style = Stroke()
                )
            }

            is SchemeBitmapFeature -> drawImage(feature.image, feature.position.toOffset())

            is SchemeImageFeature -> {
                val offset = feature.position.toOffset()
                val imageSize = feature.size.toSize()
                translate(offset.x - imageSize.width / 2, offset.y - imageSize.height / 2) {
                    with(painterCache[feature]!!) {
                        draw(imageSize)
                    }
                }
            }

            is SchemeTextFeature -> drawIntoCanvas { canvas ->
                val offset = feature.position.toOffset()
                drawText(
                    feature.text,
                    offset.x + 5,
                    offset.y - 5,
                    java.awt.Font(null, PLAIN, 16),
                    feature.color
                )
            }

            is SchemeDrawFeature -> {
                val offset = feature.position.toOffset()
                translate(offset.x, offset.y) {
                    feature.drawFeature(this)
                }
            }

            is SchemeFeatureGroup -> {
                feature.children.values.forEach {
                    drawFeature(scale, it)
                }
            }
        }
    }

    val svgGraphics2D: SVGGraphics2D = SVGGraphics2D(width, height)
    val svgScope = SvgDrawScope(svgGraphics2D, Size(width.toFloat(), height.toFloat()))

    svgScope.apply {
        features.values.filterIsInstance<SchemeBackgroundFeature>().forEach { background ->
            drawFeature(viewPoint.scale, background)
        }
        features.values.filter {
            it !is SchemeBackgroundFeature && viewPoint.scale in it.scaleRange
        }.forEach { feature ->
            drawFeature(viewPoint.scale, feature)
        }
    }

    SVGUtils.writeToSVG(path.toFile(), svgGraphics2D.svgElement)
}