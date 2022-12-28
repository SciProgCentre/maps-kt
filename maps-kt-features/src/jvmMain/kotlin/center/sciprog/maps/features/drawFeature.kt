package center.sciprog.maps.features

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.toArgb
import org.jetbrains.skia.Font
import org.jetbrains.skia.Paint
import kotlin.math.PI


internal fun Color.toPaint(): Paint = Paint().apply {
    isAntiAlias = true
    color = toArgb()
}

public fun <T : Any> DrawScope.drawFeature(
    state: CoordinateViewScope<T>,
    painterCache: Map<PainterFeature<T>, Painter>,
    feature: Feature<T>,
): Unit = with(state) {
    fun T.toOffset(): Offset = toOffset(this@drawFeature)

    when (feature) {
        is FeatureSelector -> drawFeature(state, painterCache, feature.selector(state.zoom))
        is CircleFeature -> drawCircle(
            feature.color,
            feature.size.toPx(),
            center = feature.center.toOffset()
        )

        is RectangleFeature -> drawRect(
            feature.color,
            topLeft = feature.center.toOffset() - Offset(
                feature.size.width.toPx() / 2,
                feature.size.height.toPx() / 2
            ),
            size = feature.size.toSize()
        )

        is LineFeature -> drawLine(feature.color, feature.a.toOffset(), feature.b.toOffset())
        is ArcFeature -> {
            val dpRect = feature.oval.toDpRect().toRect()

            val size = Size(dpRect.width, dpRect.height)

            drawArc(
                color = feature.color,
                startAngle = feature.startAngle / PI.toFloat() * 180f,
                sweepAngle = feature.arcLength / PI.toFloat() * 180f,
                useCenter = false,
                topLeft = dpRect.topLeft,
                size = size,
                style = Stroke()
            )

        }

        is BitmapImageFeature -> drawImage(feature.image, feature.center.toOffset())

        is VectorImageFeature -> {
            val offset = feature.center.toOffset()
            val size = feature.size.toSize()
            translate(offset.x - size.width / 2, offset.y - size.height / 2) {
                with(painterCache[feature]!!) {
                    draw(size)
                }
            }
        }

        is TextFeature -> drawIntoCanvas { canvas ->
            val offset = feature.position.toOffset()
            canvas.nativeCanvas.drawString(
                feature.text,
                offset.x + 5,
                offset.y - 5,
                Font().apply(feature.fontConfig),
                feature.color.toPaint()
            )
        }

        is DrawFeature -> {
            val offset = feature.position.toOffset()
            translate(offset.x, offset.y) {
                feature.drawFeature(this)
            }
        }

        is FeatureGroup -> {
            feature.children.values.forEach {
                drawFeature(state, painterCache, it)
            }
        }

        is PathFeature -> {
            TODO("MapPathFeature not implemented")
//                    val offset = feature.rectangle.center.toOffset() - feature.targetRect.center
//                    translate(offset.x, offset.y) {
//                        sca
//                        drawPath(feature.path, brush = feature.brush, style = feature.style)
//                    }
        }

        is PointsFeature -> {
            val points = feature.points.map { it.toOffset() }
            drawPoints(
                points = points,
                color = feature.color,
                strokeWidth = feature.stroke,
                pointMode = feature.pointMode
            )
        }

        is ScalableImageFeature -> {
            val rect = feature.rectangle.toDpRect().toRect()
            val offset = rect.topLeft

            translate(offset.x, offset.y) {
                with(painterCache[feature]!!) {
                    draw(rect.size)
                }
            }
        }

        else -> {
            //logger.error { "Unrecognized feature type: ${feature::class}" }
        }
    }
}