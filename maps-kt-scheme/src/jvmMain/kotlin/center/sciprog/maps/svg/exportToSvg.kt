package center.sciprog.maps.svg

import androidx.compose.runtime.Composable
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.painter.Painter
import center.sciprog.maps.features.*
import center.sciprog.maps.scheme.XY
import center.sciprog.maps.scheme.XYCanvasState
import org.jfree.svg.SVGGraphics2D
import org.jfree.svg.SVGUtils


public class FeatureStateSnapshot<T : Any>(
    public val features: Map<String, Feature<T>>,
    internal val painterCache: Map<PainterFeature<T>, Painter>,
)

@Composable
public fun <T : Any> FeatureGroup<T>.snapshot(): FeatureStateSnapshot<T> = FeatureStateSnapshot(
    featureMap,
    features.flatMap {
        if (it is FeatureGroup) it.features else listOf(it)
    }.filterIsInstance<PainterFeature<T>>().associateWith { it.getPainter() }
)


public fun FeatureStateSnapshot<XY>.generateSvg(
    viewPoint: ViewPoint<XY>,
    width: Double,
    height: Double,
    id: String? = null,
): String {

//    fun XY.toOffset(): Offset = Offset(
//        (width / 2 + (x - viewPoint.focus.x) * viewPoint.zoom).toFloat(),
//        (height / 2 + (viewPoint.focus.y - y) * viewPoint.zoom).toFloat()
//    )
//
//
//    fun SvgDrawScope.drawFeature(scale: Float, feature: Feature<XY>) {
//
//        val color = feature.color ?: Color.Red
//        val alpha = feature.attributes[AlphaAttribute] ?: 1f
//
//        when (feature) {
//            is ScalableImageFeature -> {
//                val offset = XY(feature.rectangle.left, feature.rectangle.top).toOffset()
//                val backgroundSize = Size(
//                    (feature.rectangle.width * scale),
//                    (feature.rectangle.height * scale)
//                )
//
//                translate(offset.x, offset.y) {
//                    with(painterCache[feature]!!) {
//                        draw(backgroundSize)
//                    }
//                }
//            }
//
//            is FeatureSelector -> drawFeature(scale, feature.selector(scale))
//
//            is CircleFeature -> drawCircle(
//                color,
//                feature.radius.toPx(),
//                center = feature.center.toOffset(),
//                alpha = alpha
//            )
//
//            is LineFeature -> drawLine(
//                color,
//                feature.a.toOffset(),
//                feature.b.toOffset(),
//                alpha = alpha
//            )
//
//            is PointsFeature -> {
//                val points = feature.points.map { it.toOffset() }
//                drawPoints(
//                    points = points,
//                    color = color,
//                    strokeWidth = feature.attributes[StrokeAttribute] ?: Stroke.HairlineWidth,
//                    pointMode = PointMode.Points,
//                    pathEffect = feature.attributes[PathEffectAttribute],
//                    alpha = alpha
//                )
//            }
//
//            is MultiLineFeature -> {
//                val points = feature.points.map { it.toOffset() }
//                drawPoints(
//                    points = points,
//                    color = color,
//                    strokeWidth = feature.attributes[StrokeAttribute] ?: Stroke.HairlineWidth,
//                    pointMode = PointMode.Polygon,
//                    pathEffect = feature.attributes[PathEffectAttribute],
//                    alpha = alpha
//                )
//            }
//
//            is ArcFeature -> {
//                val topLeft = feature.oval.leftTop.toOffset()
//                val bottomRight = feature.oval.rightBottom.toOffset()
//
//                val size = Size(abs(topLeft.x - bottomRight.x), abs(topLeft.y - bottomRight.y))
//
//                drawArc(
//                    color = color,
//                    startAngle = feature.startAngle.degrees.toFloat(),
//                    sweepAngle = feature.arcLength.degrees.toFloat(),
//                    useCenter = false,
//                    topLeft = topLeft,
//                    size = size,
//                    style = Stroke(),
//                    alpha = alpha
//                )
//            }
//
//            is BitmapIconFeature -> drawImage(feature.image, feature.center.toOffset())
//
//            is VectorIconFeature -> {
//                val offset = feature.center.toOffset()
//                val imageSize = feature.size.toSize()
//                translate(offset.x - imageSize.width / 2, offset.y - imageSize.height / 2) {
//                    with(painterCache[feature]!!) {
//                        draw(imageSize, alpha = alpha)
//                    }
//                }
//            }
//
//            is TextFeature -> drawIntoCanvas { _ ->
//                val offset = feature.position.toOffset()
//                drawText(
//                    feature.text,
//                    offset.x + 5,
//                    offset.y - 5,
//                    java.awt.Font(null, PLAIN, 16),
//                    color
//                )
//            }
//
//            is DrawFeature -> {
//                val offset = feature.position.toOffset()
//                translate(offset.x, offset.y) {
//                    feature.drawFeature(this)
//                }
//            }
//
//            is FeatureGroup -> {
//                feature.featureMap.values.forEach {
//                    drawFeature(scale, it)
//                }
//            }
//        }
//    }

    val svgGraphics2D: SVGGraphics2D = SVGGraphics2D(width, height)
    val svgCanvasState: XYCanvasState = XYCanvasState(ViewConfig())
    val svgScope = SvgDrawScope(svgCanvasState, svgGraphics2D, Size(width.toFloat(), height.toFloat()), painterCache)

    svgScope.apply {
        features.values.filter {
            viewPoint.zoom in it.zoomRange
        }.forEach { feature ->
            drawFeature(feature)
        }
    }
    return svgGraphics2D.getSVGElement(id)
}

public fun FeatureStateSnapshot<XY>.exportToSvg(
    viewPoint: ViewPoint<XY>,
    width: Double,
    height: Double,
    path: java.nio.file.Path,
) {

    val svgString = generateSvg(viewPoint, width, height)

    SVGUtils.writeToSVG(path.toFile(), svgString)
}