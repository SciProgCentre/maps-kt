package space.kscience.maps.svg

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.asSkiaBitmap
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import org.jetbrains.skia.Image
import org.jfree.svg.SVGUtils
import space.kscience.maps.features.FeatureSet
import space.kscience.maps.features.PainterFeature
import space.kscience.maps.features.ViewConfig
import space.kscience.maps.features.ViewPoint
import space.kscience.maps.scheme.XY
import space.kscience.maps.scheme.XYCanvasState
import java.nio.file.Path
import kotlin.io.path.writeBytes

public fun FeatureSet<XY>.exportToSvg(
    viewPoint: ViewPoint<XY>,
    painterCache: Map<PainterFeature<XY>, Painter>,
    size: Size,
    path: Path,
) {
    val svgCanvasState: XYCanvasState = XYCanvasState(ViewConfig()).apply {
        this.viewPoint = viewPoint
        this.canvasSize = DpSize(size.width.dp, size.height.dp)
    }

    val svgString: String = generateSvg(svgCanvasState, painterCache)
    SVGUtils.writeToSVG(path.toFile(), svgString)
}

public fun FeatureSet<XY>.exportToPng(
    viewPoint: ViewPoint<XY>,
    painterCache: Map<PainterFeature<XY>, Painter>,
    textMeasurer: TextMeasurer,
    size: Size,
    path: Path,
) {
    val svgCanvasState: XYCanvasState = XYCanvasState(ViewConfig()).apply {
        this.viewPoint = viewPoint
        this.canvasSize = DpSize(size.width.dp, size.height.dp)
    }

    val bitmap = generateBitmap(svgCanvasState, painterCache, textMeasurer, size)

    Image.makeFromBitmap(bitmap.asSkiaBitmap()).encodeToData()?.bytes?.let {
        path.writeBytes(it)
    }
}