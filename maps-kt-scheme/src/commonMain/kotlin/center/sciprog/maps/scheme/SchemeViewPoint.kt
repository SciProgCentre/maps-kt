package center.sciprog.maps.scheme

import kotlin.math.pow

data class SchemeViewPoint(val focus: SchemeCoordinates, val scale: Float = 1f)

fun SchemeViewPoint.move(deltaX: Float, deltaY: Float): SchemeViewPoint {
    return copy(focus = SchemeCoordinates(focus.x + deltaX, focus.y + deltaY))
}

fun SchemeViewPoint.zoom(
    zoom: Float,
    invariant: SchemeCoordinates = focus,
): SchemeViewPoint = if (invariant == focus) {
    copy(scale = scale * 2f.pow(zoom))
} else {
    val difScale =  (1 - 2f.pow(-zoom))
    val newCenter = SchemeCoordinates(
        focus.x + (invariant.x - focus.x) * difScale,
        focus.y + (invariant.y - focus.y) * difScale
    )
    SchemeViewPoint(newCenter, scale * 2f.pow(zoom))
}