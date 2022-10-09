package center.sciprog.maps.svg

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.*
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import java.awt.BasicStroke
import java.awt.Font
import java.awt.Graphics2D
import java.awt.geom.AffineTransform
import java.awt.image.AffineTransformOp
import java.awt.Color as AWTColor

private fun Color.toAWT(): java.awt.Color = AWTColor(toArgb())
private fun Brush.toAWT(): java.awt.Paint = TODO()

public class SvgDrawScope(public val graphics: Graphics2D, size: Size) : DrawScope {

    override val layoutDirection: LayoutDirection
        get() = LayoutDirection.Ltr

    override val density: Float get() = 1f

    override val fontScale: Float get() = 1f

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
        graphics.paint = brush.toAWT()
        when (style) {
            Fill -> graphics.fillArc(
                topLeft.x.toInt(),
                topLeft.y.toInt(),
                size.width.toInt(),
                size.height.toInt(),
                startAngle.toInt(),
                sweepAngle.toInt()
            )

            is Stroke -> graphics.drawArc(
                topLeft.x.toInt(),
                topLeft.y.toInt(),
                size.width.toInt(),
                size.height.toInt(),
                startAngle.toInt(),
                sweepAngle.toInt()
            )
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
        graphics.paint = color.toAWT()
        when (style) {
            Fill -> graphics.fillArc(
                topLeft.x.toInt(),
                topLeft.y.toInt(),
                size.width.toInt(),
                size.height.toInt(),
                startAngle.toInt(),
                sweepAngle.toInt()
            )

            is Stroke -> graphics.drawArc(
                topLeft.x.toInt(),
                topLeft.y.toInt(),
                size.width.toInt(),
                size.height.toInt(),
                startAngle.toInt(),
                sweepAngle.toInt()
            )
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
        graphics.paint = brush.toAWT()
        when (style) {
            Fill -> graphics.fillOval(
                (center.x - radius).toInt(),
                (center.y - radius).toInt(),
                (radius * 2).toInt(),
                (radius * 2).toInt()
            )

            is Stroke -> graphics.drawOval(
                (center.x - radius).toInt(),
                (center.y - radius).toInt(),
                (radius * 2).toInt(),
                (radius * 2).toInt()
            )
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
        graphics.paint = color.toAWT()
        when (style) {
            Fill -> graphics.fillOval(
                (center.x - radius).toInt(),
                (center.y - radius).toInt(),
                (radius * 2).toInt(),
                (radius * 2).toInt()
            )

            is Stroke -> graphics.drawOval(
                (center.x - radius).toInt(),
                (center.y - radius).toInt(),
                (radius * 2).toInt(),
                (radius * 2).toInt()
            )
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
        graphics.paint = brush.toAWT()
        graphics.stroke = BasicStroke(strokeWidth)
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
        graphics.paint = color.toAWT()
        graphics.stroke = BasicStroke(strokeWidth)
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
        graphics.paint = brush.toAWT()
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
        graphics.paint = color.toAWT()
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
        graphics.paint = brush.toAWT()
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
        graphics.paint = color.toAWT()
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
        graphics.paint = brush.toAWT()
        when (style) {
            Fill -> graphics.fillRect(topLeft.x.toInt(), topLeft.y.toInt(), size.width.toInt(), size.height.toInt())
            is Stroke -> graphics.drawRect(
                topLeft.x.toInt(),
                topLeft.y.toInt(),
                size.width.toInt(),
                size.height.toInt()
            )
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
        graphics.paint = color.toAWT()
        when (style) {
            Fill -> graphics.fillRect(topLeft.x.toInt(), topLeft.y.toInt(), size.width.toInt(), size.height.toInt())
            is Stroke -> graphics.drawRect(
                topLeft.x.toInt(),
                topLeft.y.toInt(),
                size.width.toInt(),
                size.height.toInt()
            )
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
        graphics.paint = brush.toAWT()
        when (style) {
            Fill -> graphics.fillRoundRect(
                topLeft.x.toInt(),
                topLeft.y.toInt(),
                size.width.toInt(),
                size.height.toInt(),
                cornerRadius.x.toInt(),
                cornerRadius.y.toInt()
            )

            is Stroke -> graphics.drawRoundRect(
                topLeft.x.toInt(),
                topLeft.y.toInt(),
                size.width.toInt(),
                size.height.toInt(),
                cornerRadius.x.toInt(),
                cornerRadius.y.toInt()
            )
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
        graphics.paint = color.toAWT()
        when (style) {
            Fill -> graphics.fillRoundRect(
                topLeft.x.toInt(),
                topLeft.y.toInt(),
                size.width.toInt(),
                size.height.toInt(),
                cornerRadius.x.toInt(),
                cornerRadius.y.toInt()
            )

            is Stroke -> graphics.drawRoundRect(
                topLeft.x.toInt(),
                topLeft.y.toInt(),
                size.width.toInt(),
                size.height.toInt(),
                cornerRadius.x.toInt(),
                cornerRadius.y.toInt()
            )
        }
    }

    fun drawText(
        text: String,
        x: Float,
        y: Float,
        font: Font,
        color: Color,
    ) {
        graphics.paint = color.toAWT()
        graphics.font = font
        graphics.drawString(text, x, y)
    }

    override val drawContext: DrawContext = SvgDrawContext(graphics, size)

}