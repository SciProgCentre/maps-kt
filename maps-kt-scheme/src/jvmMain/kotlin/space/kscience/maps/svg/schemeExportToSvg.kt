package space.kscience.maps.svg

import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import org.jfree.svg.SVGUtils
import space.kscience.maps.features.ViewConfig
import space.kscience.maps.features.ViewPoint
import space.kscience.maps.scheme.XY
import space.kscience.maps.scheme.XYCanvasState
import java.nio.file.Path

public fun FeatureSetSnapshot<XY>.exportToSvg(
    viewPoint: ViewPoint<XY>,
    width: Double,
    height: Double,
    path: Path,
) {
    val svgCanvasState: XYCanvasState = XYCanvasState(ViewConfig()).apply {
        this.viewPoint = viewPoint
        this.canvasSize = DpSize(width.dp, height.dp)
    }

    val svgString: String = generateSvg(svgCanvasState)
    SVGUtils.writeToSVG(path.toFile(), svgString)
}