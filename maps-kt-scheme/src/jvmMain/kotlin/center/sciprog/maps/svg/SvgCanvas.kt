package center.sciprog.maps.svg

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.center
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawContext
import androidx.compose.ui.graphics.drawscope.DrawTransform
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import java.awt.Graphics2D

internal fun Paint.toAwt(): java.awt.Paint {
    return java.awt.Color(color.toArgb())
}


internal fun DrawContext.asDrawTransform(): DrawTransform = object : DrawTransform {
    override val size: Size
        get() = this@asDrawTransform.size

    override val center: Offset
        get() = size.center

    override fun inset(left: Float, top: Float, right: Float, bottom: Float) {
        this@asDrawTransform.canvas.let {
            val updatedSize = Size(size.width - (left + right), size.height - (top + bottom))
            require(updatedSize.width >= 0 && updatedSize.height >= 0) {
                "Width and height must be greater than or equal to zero"
            }
            this@asDrawTransform.size = updatedSize
            it.translate(left, top)
        }
    }

    override fun clipRect(
        left: Float,
        top: Float,
        right: Float,
        bottom: Float,
        clipOp: ClipOp,
    ) {
        this@asDrawTransform.canvas.clipRect(left, top, right, bottom, clipOp)
    }

    override fun clipPath(path: Path, clipOp: ClipOp) {
        this@asDrawTransform.canvas.clipPath(path, clipOp)
    }

    override fun translate(left: Float, top: Float) {
        this@asDrawTransform.canvas.translate(left, top)
    }

    override fun rotate(degrees: Float, pivot: Offset) {
        this@asDrawTransform.canvas.apply {
            translate(pivot.x, pivot.y)
            rotate(degrees)
            translate(-pivot.x, -pivot.y)
        }
    }

    override fun scale(scaleX: Float, scaleY: Float, pivot: Offset) {
        this@asDrawTransform.canvas.apply {
            translate(pivot.x, pivot.y)
            scale(scaleX, scaleY)
            translate(-pivot.x, -pivot.y)
        }
    }

    override fun transform(matrix: Matrix) {
        this@asDrawTransform.canvas.concat(matrix)
    }
}

internal class SvgCanvas(val graphics: Graphics2D) : Canvas {
    override fun clipPath(path: Path, clipOp: ClipOp) {
        TODO("Not yet implemented")
    }

    override fun clipRect(left: Float, top: Float, right: Float, bottom: Float, clipOp: ClipOp) {
        if (clipOp == ClipOp.Intersect) {
            graphics.clipRect(
                left.toInt(),
                top.toInt(),
                (right - left).toInt(),
                (top - bottom).toInt()
            )
        } else {
            TODO()
        }
    }

    override fun concat(matrix: Matrix) {
        TODO()
//        matrix.
//        val affine = AffineTransform()
//        graphics.transform()
    }

    override fun disableZ() {
        //Do nothing
    }

    override fun drawArc(
        left: Float,
        top: Float,
        right: Float,
        bottom: Float,
        startAngle: Float,
        sweepAngle: Float,
        useCenter: Boolean,
        paint: Paint,
    ) {
        graphics.paint = paint.toAwt()
        graphics.drawArc(
            top.toInt(),
            left.toInt(),
            (right - left).toInt(),
            (top - bottom).toInt(),
            startAngle.toInt(),
            sweepAngle.toInt()
        )
    }

    override fun drawCircle(center: Offset, radius: Float, paint: Paint) {
        graphics.paint = paint.toAwt()
        graphics.drawOval(
            (center.x - radius).toInt(),
            (center.y - radius).toInt(),
            (radius * 2).toInt(),
            (radius * 2).toInt()
        )
    }

    override fun drawImage(image: ImageBitmap, topLeftOffset: Offset, paint: Paint) {
        graphics.paint = paint.toAwt()
        graphics.drawImage(image.toAwtImage(), null, topLeftOffset.x.toInt(), topLeftOffset.y.toInt())
    }

    override fun drawImageRect(
        image: ImageBitmap,
        srcOffset: IntOffset,
        srcSize: IntSize,
        dstOffset: IntOffset,
        dstSize: IntSize,
        paint: Paint,
    ) {
        TODO("Not yet implemented")
    }

    override fun drawLine(p1: Offset, p2: Offset, paint: Paint) {
        graphics.paint = paint.toAwt()
        graphics.drawLine(p1.x.toInt(), p1.y.toInt(), p2.x.toInt(), p2.y.toInt())
    }

    override fun drawOval(left: Float, top: Float, right: Float, bottom: Float, paint: Paint) {
        graphics.paint = paint.toAwt()
        graphics.drawOval(
            left.toInt(),
            top.toInt(),
            (right - left).toInt(),
            (top - bottom).toInt()
        )
    }

    override fun drawPath(path: Path, paint: Paint) {
        val skiaPath = path.asSkiaPath()
        val points: List<Offset> = skiaPath.points.mapNotNull { it?.let { Offset(it.x, it.y) } }
        drawPoints(PointMode.Lines, points, paint)
    }

    override fun drawPoints(pointMode: PointMode, points: List<Offset>, paint: Paint) {
        graphics.paint = paint.toAwt()
        val xs = IntArray(points.size) { points[it].x.toInt() }
        val ys = IntArray(points.size) { points[it].y.toInt() }
        when (pointMode) {
            PointMode.Polygon -> {
                graphics.drawPolygon(xs, ys, points.size)
            }

            PointMode.Lines -> {
                graphics.drawPolyline(xs, ys, points.size)
            }

            PointMode.Points -> {
                val diameter = paint.strokeWidth
                if (paint.strokeCap == StrokeCap.Round) {
                    points.forEach { offset ->
                        graphics.fillOval(
                            (offset.x - diameter / 2).toInt(),
                            (offset.y - diameter / 2).toInt(),
                            diameter.toInt(),
                            diameter.toInt()
                        )
                    }
                } else {
                    points.forEach { offset ->
                        graphics.fillRect(
                            (offset.x - diameter / 2).toInt(),
                            (offset.y - diameter / 2).toInt(),
                            diameter.toInt(),
                            diameter.toInt()
                        )
                    }
                }
            }
        }
    }

    override fun drawRawPoints(pointMode: PointMode, points: FloatArray, paint: Paint) {
        require(points.size % 2 == 0) { "The number of floats must be even" }
        val offsets = ArrayList<Offset>(points.size / 2)
        for (i in points.indices step 2) {
            offsets.add(Offset(points[i], points[i + 1]))
        }
        drawPoints(pointMode, offsets, paint)
    }

    override fun drawRect(left: Float, top: Float, right: Float, bottom: Float, paint: Paint) {
        graphics.paint = paint.toAwt()
        graphics.drawRect(
            left.toInt(),
            top.toInt(),
            (right - left).toInt(),
            (top - bottom).toInt()
        )
    }

    override fun drawRoundRect(
        left: Float,
        top: Float,
        right: Float,
        bottom: Float,
        radiusX: Float,
        radiusY: Float,
        paint: Paint,
    ) {
        graphics.paint = paint.toAwt()
        graphics.drawRoundRect(
            left.toInt(),
            top.toInt(),
            (right - left).toInt(),
            (top - bottom).toInt(),
            radiusX.toInt(),
            radiusY.toInt()
        )
    }

    override fun drawVertices(vertices: Vertices, blendMode: BlendMode, paint: Paint) {
        TODO("Not yet implemented")
    }

    override fun enableZ() {
        //do nothing
    }

    override fun restore() {
        TODO("Not yet implemented")
    }

    override fun rotate(degrees: Float) {
        graphics.rotate(degrees.toDouble())
    }

    override fun save() {
        TODO("Not yet implemented")
    }

    override fun saveLayer(bounds: Rect, paint: Paint) {
        TODO("Not yet implemented")
    }

    override fun scale(sx: Float, sy: Float) {
        graphics.scale(sx.toDouble(), sy.toDouble())
    }

    override fun skew(sx: Float, sy: Float) {
        //TODO is this correct?
        graphics.shear(sx.toDouble(), sy.toDouble())
    }

    override fun translate(dx: Float, dy: Float) {
        graphics.translate(dx.toDouble(), dy.toDouble())
    }
}

internal class SvgDrawContext(val graphics: Graphics2D, override var size: Size) : DrawContext {
    override val canvas: Canvas = SvgCanvas(graphics)

    override val transform: DrawTransform = asDrawTransform()
}