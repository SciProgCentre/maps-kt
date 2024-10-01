package space.kscience.maps.compose

import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import org.jfree.svg.SVGUtils
import space.kscience.maps.coordinates.Gmc
import space.kscience.maps.features.ViewConfig
import space.kscience.maps.features.ViewPoint
import space.kscience.maps.svg.FeatureSetSnapshot
import space.kscience.maps.svg.generateSvg
import java.nio.file.Path

public fun FeatureSetSnapshot<Gmc>.exportToSvg(
    mapTileProvider: MapTileProvider,
    viewPoint: ViewPoint<Gmc>,
    width: Double,
    height: Double,
    path: Path,
) {
    val mapCanvasState: MapCanvasState = MapCanvasState(mapTileProvider, ViewConfig()).apply {
        this.viewPoint = viewPoint
        this.canvasSize = DpSize(width.dp, height.dp)
    }

    val svgString: String = generateSvg(mapCanvasState)
    SVGUtils.writeToSVG(path.toFile(), svgString)
}