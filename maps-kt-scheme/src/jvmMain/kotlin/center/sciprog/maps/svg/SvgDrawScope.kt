package center.sciprog.maps.svg

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.*
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import center.sciprog.attributes.Attributes
import center.sciprog.maps.features.*
import center.sciprog.maps.scheme.XY
import org.jfree.svg.SVGGraphics2D
import java.awt.BasicStroke
import java.awt.geom.*
import java.awt.image.AffineTransformOp
import java.awt.Color as AWTColor

public class SvgDrawScope(
    state: CanvasState<XY>,
    private val graphics: SVGGraphics2D,
    size: Size,
    private val painterCache: Map<PainterFeature<XY>, Painter>,
    private val defaultStrokeWidth: Float = 1f
) : FeatureDrawScope<XY>(state) {

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
        val arc = Arc2D.Float(
            topLeft.x, topLeft.y, size.width, size.height, -startAngle, -sweepAngle, Arc2D.OPEN
        )

        when (style) {
            Fill -> graphics.fill(arc)
            is Stroke -> {
                setupStroke(style)
                graphics.draw(arc)
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

        val arc = Arc2D.Float(
            topLeft.x, topLeft.y, size.width, size.height, -startAngle, -sweepAngle, Arc2D.OPEN
        )

        when (style) {
            Fill -> graphics.fill(arc)
            is Stroke -> {
                setupStroke(style)
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
        val circle = Ellipse2D.Float(
            (center.x - radius),
            (center.y - radius),
            (radius * 2),
            (radius * 2)
        )
        when (style) {
            Fill -> graphics.fill(circle)

            is Stroke -> {
                setupStroke(style)
                graphics.draw(circle)
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
        val circle = Ellipse2D.Float(
            (center.x - radius),
            (center.y - radius),
            (radius * 2),
            (radius * 2)
        )
        when (style) {
            Fill -> graphics.fill(circle)

            is Stroke -> {
                setupStroke(style)
                graphics.draw(circle)
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
        if (style is Stroke) {
            setupStroke(style)
        }
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
        if (style is Stroke) {
            setupStroke(style)
        }
        graphics.drawImage(awtImage, op, dstOffset.x, dstOffset.y)

    }

    @Suppress("OVERRIDE_DEPRECATION")
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
        graphics.draw(Line2D.Float(start.x, start.y, end.x, end.y))
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
        graphics.draw(Line2D.Float(start.x, start.y, end.x, end.y))
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
        val oval = Ellipse2D.Float(topLeft.x, topLeft.y, size.width, size.height)
        when (style) {
            Fill -> graphics.fill(oval)
            is Stroke -> {
                setupStroke(style)
                graphics.draw(oval)
            }
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
        val oval = Ellipse2D.Float(topLeft.x, topLeft.y, size.width, size.height)
        when (style) {
            Fill -> graphics.fill(oval)
            is Stroke -> {
                setupStroke(style)
                graphics.draw(oval)
            }
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
        val rect = Rectangle2D.Float(topLeft.x, topLeft.y, size.width, size.height)
        when (style) {
            Fill -> graphics.fill(rect)
            is Stroke -> {
                setupStroke(style)
                graphics.draw(rect)
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
        val rect = Rectangle2D.Float(topLeft.x, topLeft.y, size.width, size.height)
        when (style) {
            Fill -> graphics.fill(rect)
            is Stroke -> {
                setupStroke(style)
                graphics.draw(rect)
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
        val rect = Rectangle2D.Float(topLeft.x, topLeft.y, size.width, size.height)
        when (style) {
            Fill -> graphics.fill(rect)
            is Stroke -> {
                setupStroke(style)
                graphics.draw(rect)
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

        val roundRect = RoundRectangle2D.Float(
            topLeft.x,
            topLeft.y,
            size.width,
            size.height,
            cornerRadius.x,
            cornerRadius.y
        )

        when (style) {
            Fill -> graphics.fill(roundRect)

            is Stroke -> {
                setupStroke(style)
                graphics.draw(roundRect)
            }
        }
    }

    public fun renderText(
        textFeature: TextFeature<XY>,
    ) {
        textFeature.color?.let { setupColor(it)}
        graphics.drawString(textFeature.text, textFeature.position.x, textFeature.position.y)
    }

    override fun painterFor(feature: PainterFeature<XY>): Painter {
        return painterCache[feature]!!
    }

    override fun drawText(text: String, position: Offset, attributes: Attributes) {
        attributes[ColorAttribute]?.let { setupColor(it)}
        graphics.drawString(text, position.x, position.y)
    }

    override val drawContext: DrawContext = SvgDrawContext(graphics, size)

}