package space.kscience.maps.compose

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.asSkiaBitmap
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import org.jetbrains.skia.Image
import org.jfree.svg.SVGUtils
import space.kscience.maps.coordinates.Gmc
import space.kscience.maps.features.FeatureSet
import space.kscience.maps.features.PainterFeature
import space.kscience.maps.features.ViewConfig
import space.kscience.maps.features.ViewPoint
import space.kscience.maps.svg.generateBitmap
import space.kscience.maps.svg.generateSvg
import java.nio.file.Path
import kotlin.io.path.writeBytes

/**
 * Exports the features of a [FeatureSet] to an SVG file.
 *
 * @param viewPoint The `ViewPoint` providing the spatial context (focus and zoom level) for rendering the SVG.
 * @param painterCache A map associating `PainterFeature` instances with their respective `Painter` objects,
 * used for rendering the features in the SVG.
 * @param size The dimensions (`Size`) of the SVG file to generate.
 * @param path The file path where the generated SVG will be saved.
 */
public fun FeatureSet<Gmc>.exportToSvg(
    viewPoint: ViewPoint<Gmc>,
    painterCache: Map<PainterFeature<Gmc>, Painter>,
    size: Size,
    path: Path,
) {
    val mapCanvasState: MapCanvasState = MapCanvasState(ViewConfig()).apply {
        this.viewPoint = viewPoint
        this.canvasSize = DpSize(size.width.dp, size.height.dp)
    }

    val svgString: String = generateSvg(mapCanvasState, painterCache)
    SVGUtils.writeToSVG(path.toFile(), svgString)
}

public fun FeatureSet<Gmc>.exportToPng(
    viewPoint: ViewPoint<Gmc>,
    painterCache: Map<PainterFeature<Gmc>, Painter>,
    textMeasurer: TextMeasurer,
    size: Size,
    path: Path,
) {
    val mapCanvasState: MapCanvasState = MapCanvasState(ViewConfig()).apply {
        this.viewPoint = viewPoint
        this.canvasSize = DpSize(size.width.dp, size.height.dp)
    }

    val bitmap = generateBitmap(mapCanvasState, painterCache, textMeasurer, size)

    Image.makeFromBitmap(bitmap.asSkiaBitmap()).encodeToData()?.bytes?.let {
        path.writeBytes(it)
    }
}