package center.sciprog.maps.features

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PointMode
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import center.sciprog.attributes.plus
import space.kscience.kmath.PerformancePitfall
import space.kscience.kmath.geometry.degrees


//internal fun Color.toPaint(): Paint = Paint().apply {
//    isAntiAlias = true
//    color = toArgb()
//}


public fun <T : Any> FeatureDrawScope<T>.drawFeature(
    feature: Feature<T>,
): Unit {
    val color = feature.color ?: Color.Red
    val alpha = feature.attributes[AlphaAttribute] ?: 1f

    when (feature) {
        is FeatureSelector -> drawFeature(feature.selector(state.zoom))
        is CircleFeature -> drawCircle(
            color,
            feature.radius.toPx(),
            center = feature.center.toOffset()
        )

        is RectangleFeature -> drawRect(
            color,
            topLeft = feature.center.toOffset() - Offset(
                feature.size.width.toPx() / 2,
                feature.size.height.toPx() / 2
            ),
            size = feature.size.toSize()
        )

        is LineFeature -> drawLine(
            color,
            feature.a.toOffset(),
            feature.b.toOffset(),
            strokeWidth = feature.attributes[StrokeAttribute] ?: Stroke.HairlineWidth,
            pathEffect = feature.attributes[PathEffectAttribute]
        )

        is ArcFeature -> {
            val dpRect = feature.oval.toDpRect().toRect()

            val size = Size(dpRect.width, dpRect.height)

            drawArc(
                color = color,
                startAngle = (feature.startAngle.degrees).toFloat(),
                sweepAngle = (feature.arcLength.degrees).toFloat(),
                useCenter = false,
                topLeft = dpRect.topLeft,
                size = size,
                style = Stroke(),
                alpha = alpha
            )

        }

        is BitmapIconFeature -> drawImage(feature.image, feature.center.toOffset())

        is VectorIconFeature -> {
            val offset = feature.center.toOffset()
            val size = feature.size.toSize()
            translate(offset.x - size.width / 2, offset.y - size.height / 2) {
                with(this@drawFeature.painterFor(feature)) {
                    draw(size, colorFilter = feature.color?.let { ColorFilter.tint(it) })
                }
            }
        }

        is TextFeature -> drawText(feature.text, feature.position.toOffset(), feature.attributes)

        is DrawFeature -> {
            val offset = feature.position.toOffset()
            translate(offset.x, offset.y) {
                feature.drawFeature(this)
            }
        }

        is FeatureGroup -> {
            feature.featureMap.values.forEach {
                drawFeature(it.withAttributes { feature.attributes + this })
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
                color = color,
                strokeWidth = feature.attributes[StrokeAttribute] ?: 5f,
                pointMode = PointMode.Points,
                pathEffect = feature.attributes[PathEffectAttribute],
                alpha = alpha
            )
        }

        is MultiLineFeature -> {
            val points = feature.points.map { it.toOffset() }
            drawPoints(
                points = points,
                color = color,
                strokeWidth = feature.attributes[StrokeAttribute] ?: Stroke.HairlineWidth,
                pointMode = PointMode.Polygon,
                pathEffect = feature.attributes[PathEffectAttribute],
                alpha = alpha
            )
        }

        is PolygonFeature -> {
            val points = feature.points.map { it.toOffset() }
            val last = points.last()
            val polygonPath = Path()
            polygonPath.moveTo(last.x, last.y)
            for ((x, y) in points) {
                polygonPath.lineTo(x, y)
            }
            drawPath(
                path = polygonPath,
                color = color,
                alpha = alpha
            )
        }

        is ScalableImageFeature -> {
            val rect = feature.rectangle.toDpRect().toRect()
            val offset = rect.topLeft

            translate(offset.x, offset.y) {
                with(this@drawFeature.painterFor(feature)) {
                    draw(rect.size)
                }
            }
        }

        is PixelMapFeature -> {
            val rect = feature.rectangle.toDpRect().toRect()
            val xStep = rect.size.width / feature.pixelMap.shape[0]
            val yStep = rect.size.height / feature.pixelMap.shape[1]
            val pixelSize = Size(xStep, yStep)
            //TODO add re-clasterization for small pixel scales
            val offset = rect.topLeft
            translate(offset.x, offset.y) {
                @OptIn(PerformancePitfall::class)
                feature.pixelMap.elements().forEach { (index, color: Color?) ->
                    val (i, j) = index
                    if (color != null) {
                        drawRect(
                            color,
                            topLeft = Offset(
                                x = i * xStep,
                                y = rect.height - j * yStep
                            ),
                            size = pixelSize
                        )
                    }
                }
            }
        }

        else -> {
            //logger.error { "Unrecognized feature type: ${feature::class}" }
        }
    }
}