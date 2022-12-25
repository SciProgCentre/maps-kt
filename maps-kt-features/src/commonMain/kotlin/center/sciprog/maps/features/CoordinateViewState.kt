package center.sciprog.maps.features

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.*

public abstract class CoordinateViewState<T : Any>(
    public val config: ViewConfig<T>,
    canvasSize: DpSize,
    viewPoint: ViewPoint<T>,
) {

    public abstract val space: CoordinateSpace<T>

    public var canvasSize: DpSize by mutableStateOf(canvasSize)
    protected var viewPointState: MutableState<ViewPoint<T>> = mutableStateOf(viewPoint)

    public var viewPoint: ViewPoint<T>
        get() = viewPointState.value
        set(value) {
            config.onViewChange(value)
            viewPointState.value = value
        }

    public val zoom: Float get() = viewPoint.zoom

    public abstract fun DpOffset.toCoordinates(): T

    public abstract fun T.toDpOffset(): DpOffset

    public fun T.toOffset(density: Density): Offset = with(density){
        val dpOffset = this@toOffset.toDpOffset()
        Offset(dpOffset.x.toPx(), dpOffset.y.toPx())
    }

    public abstract fun Rectangle<T>.toDpRect(): DpRect

    public abstract fun ViewPoint<T>.moveBy(x: Dp, y: Dp): ViewPoint<T>

    public abstract fun viewPointFor(rectangle: Rectangle<T>): ViewPoint<T>

    // Selection rectangle. If null - no selection
    public var selectRect: DpRect? by mutableStateOf(null)
}


public val DpRect.topLeft: DpOffset get() = DpOffset(left, top)

public val DpRect.bottomRight: DpOffset get() = DpOffset(right, bottom)