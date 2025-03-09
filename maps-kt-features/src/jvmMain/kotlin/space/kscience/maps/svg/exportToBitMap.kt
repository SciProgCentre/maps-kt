package space.kscience.maps.svg

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import space.kscience.maps.features.CanvasState
import space.kscience.maps.features.ComposeFeatureDrawScope
import space.kscience.maps.features.FeatureSet
import space.kscience.maps.features.PainterFeature

public fun <T : Any> FeatureSet<T>.generateBitmap(
    canvasState: CanvasState<T>,
    painterCache: Map<PainterFeature<T>, Painter>,
    textMeasurer: TextMeasurer,
    size: Size
): ImageBitmap {

    val bitmap = ImageBitmap(size.width.toInt(), size.height.toInt())

    CanvasDrawScope().draw(
        density = Density(1f),
        layoutDirection = LayoutDirection.Ltr,
        canvas = Canvas(bitmap),
        size = size,
    ) {
        ComposeFeatureDrawScope(this, canvasState, painterCache, textMeasurer).features(this@generateBitmap)
    }
    return bitmap
}