package center.sciprog.maps.svg

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.*
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import org.jfree.svg.SVGGraphics2D
import java.awt.BasicStroke
import java.awt.Font
import java.awt.geom.AffineTransform
import java.awt.geom.Arc2D
import java.awt.image.AffineTransformOp
import java.awt.Color as AWTColor

public class SvgDrawScope(
    private val graphics: SVGGraphics2D,
    size: Size,
    val defaultStrokeWidth: Float = 1f,
) : DrawScope {

    override val layoutDirection: LayoutDirection
        get() = LayoutDirection.Ltr

    override val density: Float get() = 1f

    override val fontScale: Float get() = 1f

    private fun setupStroke(strokeWidth: Float, cap: StrokeCap, join: StrokeJoin = StrokeJoin.Miter) {
        val width = if (strokeWidth == 0f) defaultStrokeWidth else strokeWidth
        val capValue = when (cap) {
            StrokeCap.Butt -> BasicStroke.CAP_BUTT
            StrokeCap.Round -> BasicStroke.CAP_ROUND
            StrokeCap.Square -> BasicStroke.CAP_SQUARE
            else -> BasicStroke.CAP_SQUARE
        }
        val joinValue = when (join) {
            StrokeJoin.Bevel -> BasicStroke.JOIN_BEVEL
            StrokeJoin.Miter -> BasicStroke.JOIN_MITER
            StrokeJoin.Round -> BasicStroke.JOIN_ROUND
            else -> BasicStroke.JOIN_MITER
        }
        graphics.stroke = BasicStroke(width, capValue, joinValue)
    }

    private fun setupStroke(stroke: Stroke) {
        setupStroke(stroke.width, stroke.cap, stroke.join)
    }

    private fun setupColor(color: Color) {
        graphics.paint = AWTColor(color.toArgb(), false)
    }

    private fun setupColor(brush: Brush) {
        when (brush) {
            is SolidColor -> {
                graphics.paint = AWTColor(brush.value.toArgb(), false)
            }

            is ShaderBrush -> TODO()
        }
    }


    override fun drawArc(
        brush: Brush,
        startAngle: Float,
        sweepAngle: Float,
        useCenter: Boolean,
        topLeft: Offset,
        size: Size,
        alpha: Float,
        style: DrawStyle,
        colorFilter: ColorFilter?,
        blendMode: BlendMode,
    ) {
        setupColor(brush)
        when (style) {
            Fill -> graphics.fillArc(
                topLeft.x.toInt(),
                topLeft.y.toInt(),
                size.width.toInt(),
                size.height.toInt(),
                startAngle.toInt(),
                sweepAngle.toInt()
            )

            is Stroke -> {
                setupStroke(style)
                graphics.drawArc(
                    topLeft.x.toInt(),
                    topLeft.y.toInt(),
                    size.width.toInt(),
                    size.height.toInt(),
                    startAngle.toInt(),
                    sweepAngle.toInt()
                )
            }
        }
    }

    override fun drawArc(
        color: Color,
        startAngle: Float,
        sweepAngle: Float,
        useCenter: Boolean,
        topLeft: Offset,
        size: Size,
        alpha: Float,
        style: DrawStyle,
        colorFilter: ColorFilter?,
        blendMode: BlendMode,
    ) {
        setupColor(color)
        when (style) {
            Fill -> graphics.fillArc(
                topLeft.x.toInt(),
                topLeft.y.toInt(),
                size.width.toInt(),
                size.height.toInt(),
                -startAngle.toInt(),
                -sweepAngle.toInt()
            )

            is Stroke -> {
                setupStroke(style)
                val arc = Arc2D.Float(
                    topLeft.x, topLeft.y, size.width, size.height, -startAngle, -sweepAngle, Arc2D.OPEN
                )
                graphics.draw(arc)
            }
        }

    }

    override fun drawCircle(
        brush: Brush,
        radius: Float,
        center: Offset,
        alpha: Float,
        style: DrawStyle,
        colorFilter: ColorFilter?,
        blendMode: BlendMode,
    ) {
        setupColor(brush)
        when (style) {
            Fill -> graphics.fillOval(
                (center.x - radius).toInt(),
                (center.y - radius).toInt(),
                (radius * 2).toInt(),
                (radius * 2).toInt()
            )

            is Stroke -> {
                setupStroke(style)
                graphics.drawOval(
                    (center.x - radius).toInt(),
                    (center.y - radius).toInt(),
                    (radius * 2).toInt(),
                    (radius * 2).toInt()
                )
            }
        }

    }

    override fun drawCircle(
        color: Color,
        radius: Float,
        center: Offset,
        alpha: Float,
        style: DrawStyle,
        colorFilter: ColorFilter?,
        blendMode: BlendMode,
    ) {
        setupColor(color)
        when (style) {
            Fill -> graphics.fillOval(
                (center.x - radius).toInt(),
                (center.y - radius).toInt(),
                (radius * 2).toInt(),
                (radius * 2).toInt()
            )

            is Stroke -> {
                setupStroke(style)
                graphics.drawOval(
                    (center.x - radius).toInt(),
                    (center.y - radius).toInt(),
                    (radius * 2).toInt(),
                    (radius * 2).toInt()
                )
            }
        }
    }

    override fun drawImage(
        image: ImageBitmap,
        topLeft: Offset,
        alpha: Float,
        style: DrawStyle,
        colorFilter: ColorFilter?,
        blendMode: BlendMode,
    ) {
        graphics.drawImage(image.toAwtImage(), null, topLeft.x.toInt(), topLeft.y.toInt())
    }

    override fun drawImage(
        image: ImageBitmap,
        srcOffset: IntOffset,
        srcSize: IntSize,
        dstOffset: IntOffset,
        dstSize: IntSize,
        alpha: Float,
        style: DrawStyle,
        colorFilter: ColorFilter?,
        blendMode: BlendMode,
        filterQuality: FilterQuality,
    ) {
        val scale: AffineTransform = AffineTransform.getScaleInstance(
            dstSize.width.toDouble() / srcSize.width,
            dstSize.height.toDouble() / srcSize.height
        )
        val awtImage = image.toAwtImage().getSubimage(srcOffset.x, srcOffset.y, srcSize.width, srcSize.height)
        val op = AffineTransformOp(scale, AffineTransformOp.TYPE_NEAREST_NEIGHBOR)
        graphics.drawImage(awtImage, op, dstOffset.x, dstOffset.y)
    }

    override fun drawImage(
        image: ImageBitmap,
        srcOffset: IntOffset,
        srcSize: IntSize,
        dstOffset: IntOffset,
        dstSize: IntSize,
        alpha: Float,
        style: DrawStyle,
        colorFilter: ColorFilter?,
        blendMode: BlendMode,
    ) {
        val scale: AffineTransform = AffineTransform.getScaleInstance(
            dstSize.width.toDouble() / srcSize.width,
            dstSize.height.toDouble() / srcSize.height
        )
        val awtImage = image.toAwtImage().getSubimage(srcOffset.x, srcOffset.y, srcSize.width, srcSize.height)
        val op = AffineTransformOp(scale, AffineTransformOp.TYPE_NEAREST_NEIGHBOR)
        graphics.drawImage(awtImage, op, dstOffset.x, dstOffset.y)
    }

    override fun drawLine(
        brush: Brush,
        start: Offset,
        end: Offset,
        strokeWidth: Float,
        cap: StrokeCap,
        pathEffect: PathEffect?,
        alpha: Float,
        colorFilter: ColorFilter?,
        blendMode: BlendMode,
    ) {
        setupColor(brush)
        setupStroke(strokeWidth, cap)
        graphics.drawLine(start.x.toInt(), start.y.toInt(), end.x.toInt(), end.y.toInt())
    }

    override fun drawLine(
        color: Color,
        start: Offset,
        end: Offset,
        strokeWidth: Float,
        cap: StrokeCap,
        pathEffect: PathEffect?,
        alpha: Float,
        colorFilter: ColorFilter?,
        blendMode: BlendMode,
    ) {
        setupColor(color)
        setupStroke(strokeWidth, cap)
        graphics.drawLine(start.x.toInt(), start.y.toInt(), end.x.toInt(), end.y.toInt())
    }

    override fun drawOval(
        brush: Brush,
        topLeft: Offset,
        size: Size,
        alpha: Float,
        style: DrawStyle,
        colorFilter: ColorFilter?,
        blendMode: BlendMode,
    ) {
        setupColor(brush)
        when (style) {
            Fill -> graphics.fillOval(topLeft.x.toInt(), topLeft.y.toInt(), size.width.toInt(), size.height.toInt())
            is Stroke -> graphics.drawOval(
                topLeft.x.toInt(),
                topLeft.y.toInt(),
                size.width.toInt(),
                size.height.toInt()
            )
        }
    }

    override fun drawOval(
        color: Color,
        topLeft: Offset,
        size: Size,
        alpha: Float,
        style: DrawStyle,
        colorFilter: ColorFilter?,
        blendMode: BlendMode,
    ) {
        setupColor(color)
        when (style) {
            Fill -> graphics.fillOval(topLeft.x.toInt(), topLeft.y.toInt(), size.width.toInt(), size.height.toInt())
            is Stroke -> graphics.drawOval(
                topLeft.x.toInt(),
                topLeft.y.toInt(),
                size.width.toInt(),
                size.height.toInt()
            )
        }
    }

    override fun drawPath(
        path: Path,
        brush: Brush,
        alpha: Float,
        style: DrawStyle,
        colorFilter: ColorFilter?,
        blendMode: BlendMode,
    ) {
        val skiaPath = path.asSkiaPath()
        val points = skiaPath.points.mapNotNull { it?.let { Offset(it.x, it.y) } }
        drawPoints(points, PointMode.Lines, brush)
    }

    override fun drawPath(
        path: Path,
        color: Color,
        alpha: Float,
        style: DrawStyle,
        colorFilter: ColorFilter?,
        blendMode: BlendMode,
    ) {
        val skiaPath = path.asSkiaPath()
        val points = skiaPath.points.mapNotNull { it?.let { Offset(it.x, it.y) } }
        drawPoints(points, PointMode.Lines, color)
    }

    override fun drawPoints(
        points: List<Offset>,
        pointMode: PointMode,
        brush: Brush,
        strokeWidth: Float,
        cap: StrokeCap,
        pathEffect: PathEffect?,
        alpha: Float,
        colorFilter: ColorFilter?,
        blendMode: BlendMode,
    ) {
        setupColor(brush)
        graphics.stroke = BasicStroke(strokeWidth)
        val xs = IntArray(points.size) { points[it].x.toInt() }
        val ys = IntArray(points.size) { points[it].y.toInt() }
        graphics.drawPolyline(xs, ys, points.size)
    }

    override fun drawPoints(
        points: List<Offset>,
        pointMode: PointMode,
        color: Color,
        strokeWidth: Float,
        cap: StrokeCap,
        pathEffect: PathEffect?,
        alpha: Float,
        colorFilter: ColorFilter?,
        blendMode: BlendMode,
    ) {
        setupColor(color)
        graphics.stroke = BasicStroke(strokeWidth)
        val xs = IntArray(points.size) { points[it].x.toInt() }
        val ys = IntArray(points.size) { points[it].y.toInt() }
        graphics.drawPolyline(xs, ys, points.size)
    }

    override fun drawRect(
        brush: Brush,
        topLeft: Offset,
        size: Size,
        alpha: Float,
        style: DrawStyle,
        colorFilter: ColorFilter?,
        blendMode: BlendMode,
    ) {
        setupColor(brush)
        when (style) {
            Fill -> graphics.fillRect(topLeft.x.toInt(), topLeft.y.toInt(), size.width.toInt(), size.height.toInt())
            is Stroke -> {
                setupStroke(style)
                graphics.drawRect(
                    topLeft.x.toInt(),
                    topLeft.y.toInt(),
                    size.width.toInt(),
                    size.height.toInt()
                )
            }
        }

    }

    override fun drawRect(
        color: Color,
        topLeft: Offset,
        size: Size,
        alpha: Float,
        style: DrawStyle,
        colorFilter: ColorFilter?,
        blendMode: BlendMode,
    ) {
        setupColor(color)
        when (style) {
            Fill -> graphics.fillRect(topLeft.x.toInt(), topLeft.y.toInt(), size.width.toInt(), size.height.toInt())
            is Stroke -> {
                setupStroke(style)
                graphics.drawRect(
                    topLeft.x.toInt(),
                    topLeft.y.toInt(),
                    size.width.toInt(),
                    size.height.toInt()
                )
            }
        }
    }

    override fun drawRoundRect(
        brush: Brush,
        topLeft: Offset,
        size: Size,
        cornerRadius: CornerRadius,
        alpha: Float,
        style: DrawStyle,
        colorFilter: ColorFilter?,
        blendMode: BlendMode,
    ) {
        setupColor(brush)
        when (style) {
            Fill -> graphics.fillRoundRect(
                topLeft.x.toInt(),
                topLeft.y.toInt(),
                size.width.toInt(),
                size.height.toInt(),
                cornerRadius.x.toInt(),
                cornerRadius.y.toInt()
            )

            is Stroke -> {
                setupStroke(style)
                graphics.drawRoundRect(
                    topLeft.x.toInt(),
                    topLeft.y.toInt(),
                    size.width.toInt(),
                    size.height.toInt(),
                    cornerRadius.x.toInt(),
                    cornerRadius.y.toInt()
                )
            }
        }

    }

    override fun drawRoundRect(
        color: Color,
        topLeft: Offset,
        size: Size,
        cornerRadius: CornerRadius,
        style: DrawStyle,
        alpha: Float,
        colorFilter: ColorFilter?,
        blendMode: BlendMode,
    ) {
        setupColor(color)
        when (style) {
            Fill -> graphics.fillRoundRect(
                topLeft.x.toInt(),
                topLeft.y.toInt(),
                size.width.toInt(),
                size.height.toInt(),
                cornerRadius.x.toInt(),
                cornerRadius.y.toInt()
            )

            is Stroke -> {
                setupStroke(style)
                graphics.drawRoundRect(
                    topLeft.x.toInt(),
                    topLeft.y.toInt(),
                    size.width.toInt(),
                    size.height.toInt(),
                    cornerRadius.x.toInt(),
                    cornerRadius.y.toInt()
                )
            }
        }
    }

    fun drawText(
        text: String,
        x: Float,
        y: Float,
        font: Font,
        color: Color,
    ) {
        setupColor(color)
        graphics.font = font
        graphics.drawString(text, x, y)
    }

    override val drawContext: DrawContext = SvgDrawContext(graphics, size)

}