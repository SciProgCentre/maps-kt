package center.sciprog.maps.features

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.*
import kotlin.math.pow
import kotlin.math.sqrt

private fun distanceBetween(a: DpOffset, b: DpOffset): Dp = sqrt((b.x - a.x).value.pow(2) + (b.y - a.y).value.pow(2)).dp

public abstract class CoordinateViewScope<T : Any>(
    public val config: ViewConfig<T>,
) {

    public abstract val space: CoordinateSpace<T>

    protected var canvasSizeState: MutableState<DpSize?> = mutableStateOf(null)
    protected var viewPointState: MutableState<ViewPoint<T>?> = mutableStateOf(null)

    public var canvasSize: DpSize
        get() = canvasSizeState.value ?: DpSize(512.dp, 512.dp)
        set(value) {
            canvasSizeState.value = value
        }

    public var viewPoint: ViewPoint<T>
        get() = viewPointState.value ?: space.defaultViewPoint
        set(value) {
            viewPointState.value = value
            config.onViewChange(viewPoint)
        }

    public val zoom: Float get() = viewPoint.zoom


    // Selection rectangle. If null - no selection
    public var selectRect: DpRect? by mutableStateOf(null)

    public abstract fun DpOffset.toCoordinates(): T

    public abstract fun T.toDpOffset(): DpOffset

    public fun T.toOffset(density: Density): Offset = with(density) {
        val dpOffset = this@toOffset.toDpOffset()
        Offset(dpOffset.x.toPx(), dpOffset.y.toPx())
    }

    public abstract fun Rectangle<T>.toDpRect(): DpRect

    public abstract fun ViewPoint<T>.moveBy(x: Dp, y: Dp): ViewPoint<T>

    public abstract fun computeViewPoint(rectangle: Rectangle<T>): ViewPoint<T>
}


public val DpRect.topLeft: DpOffset get() = DpOffset(left, top)

public val DpRect.bottomRight: DpOffset get() = DpOffset(right, bottom)