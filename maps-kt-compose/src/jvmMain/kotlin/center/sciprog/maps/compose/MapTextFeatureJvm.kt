package center.sciprog.maps.compose

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.NativeCanvas
import androidx.compose.ui.graphics.toArgb
import org.jetbrains.skia.Paint

public actual typealias Font = org.jetbrains.skia.Font

public actual fun NativeCanvas.drawString(
    text: String,
    x: Float,
    y: Float,
    font: Font,
    color: Color
) {
    drawString(text, x, y, font, color.toPaint())
}

private fun Color.toPaint(): Paint = Paint().apply {
    isAntiAlias = true
    color = toArgb()
}