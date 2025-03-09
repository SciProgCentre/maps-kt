package space.kscience.maps.features

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.*

/**
 * A state holder for current canvas size and view point. Allows transformation from coordinates to pixels and back
 */
public abstract class CanvasState<T: Any>(
    public val viewConfig: ViewConfig<T>
){
    public abstract val space: CoordinateSpace<T>

    private var canvasSizeState: MutableState<DpSize?> = mutableStateOf(null)
    private var viewPointState: MutableState<ViewPoint<T>?> = mutableStateOf(null)

    public var canvasSize: DpSize
        get() = canvasSizeState.value ?: DpSize(512.dp, 512.dp)
        set(value) {
            canvasSizeState.value = value
            viewConfig.onCanvasSizeChange(value)
        }

    public var viewPoint: ViewPoint<T>
        get() = viewPointState.value ?: space.defaultViewPoint
        set(value) {
            viewPointState.value = value
            viewConfig.onViewChange(viewPoint)
        }

    public val zoom: Float get() = viewPoint.zoom


    // Selection rectangle. If null - no selection
    public var selectRect: DpRect? by mutableStateOf(null)

    public abstract fun Rectangle<T>.toDpRect(): DpRect

    public abstract fun ViewPoint<T>.moveBy(x: Dp, y: Dp): ViewPoint<T>

    public abstract fun computeViewPoint(rectangle: Rectangle<T>): ViewPoint<T>

    public abstract fun DpOffset.toCoordinates(): T


    public abstract fun T.toDpOffset(): DpOffset

    public fun toCoordinates(offset: Offset, density: Density): T  = with(density){
        val dpOffset = DpOffset(offset.x.toDp(), offset.y.toDp())
        dpOffset.toCoordinates()
    }

    public fun toOffset(coordinates: T, density: Density): Offset = with(density){
        val dpOffset = coordinates.toDpOffset()
        return Offset(dpOffset.x.toPx(), dpOffset.y.toPx())
    }

}

public val DpRect.topLeft: DpOffset get() = DpOffset(left, top)

public val DpRect.bottomRight: DpOffset get() = DpOffset(right, bottom)