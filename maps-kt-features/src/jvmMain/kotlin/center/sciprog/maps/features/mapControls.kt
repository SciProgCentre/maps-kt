package center.sciprog.maps.features

import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.gestures.forEachGesture
import androidx.compose.runtime.*
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.unit.*
import kotlin.math.max
import kotlin.math.min


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

    public abstract fun DpOffset.toCoordinates(): T

    public abstract fun T.toDpOffset(): DpOffset

    public fun T.toOffset(density: Density): Offset = with(density){
        val dpOffset = this@toOffset.toDpOffset()
        Offset(dpOffset.x.toPx(), dpOffset.y.toPx())
    }

    public abstract fun ViewPoint<T>.moveBy(x: Dp, y: Dp): ViewPoint<T>

    public abstract fun viewPointFor(rectangle: Rectangle<T>): ViewPoint<T>

    // Selection rectangle. If null - no selection
    public var selectRect: DpRect? by mutableStateOf<DpRect?>(null)
}

public val DpRect.topLeft: DpOffset get() = DpOffset(left, top)
public val DpRect.bottomRight: DpOffset get() = DpOffset(right, bottom)


@OptIn(ExperimentalComposeUiApi::class)
public fun <T : Any> Modifier.mapControls(
    state: CoordinateViewState<T>,
): Modifier = with(state) {
    pointerInput(Unit) {
        forEachGesture {
            awaitPointerEventScope {
                fun Offset.toDpOffset() = DpOffset(x.toDp(), y.toDp())

                val event: PointerEvent = awaitPointerEvent()

                event.changes.forEach { change ->
                    val dragStart = change.position
                    val dpPos = DpOffset(dragStart.x.toDp(), dragStart.y.toDp())

                    //start selection
                    val selectionStart: Offset? =
                        if (event.buttons.isPrimaryPressed && event.keyboardModifiers.isShiftPressed) {
                            change.position
                        } else {
                            null
                        }

                    drag(change.id) { dragChange ->
                        val dragAmount: Offset = dragChange.position - dragChange.previousPosition
                        val dpStart = dragChange.previousPosition.toDpOffset()
                        val dpEnd = dragChange.position.toDpOffset()

                        //apply drag handle and check if it prohibits the drag even propagation
                        if (selectionStart == null && !config.dragHandle.handle(
                                event,
                                space.ViewPoint(dpStart.toCoordinates(), viewPoint.zoom),
                                space.ViewPoint(dpEnd.toCoordinates(), viewPoint.zoom)
                            )
                        ) {
                            return@drag
                        }

                        if (event.buttons.isPrimaryPressed) {
                            //If selection process is started, modify the frame
                            selectionStart?.let { start ->
                                val offset = dragChange.position
                                selectRect = DpRect(
                                    min(offset.x, start.x).dp,
                                    min(offset.y, start.y).dp,
                                    max(offset.x, start.x).dp,
                                    max(offset.y, start.y).dp
                                )
                                return@drag
                            }

//                            config.onClick(MapViewPoint(dpPos.toGeodetic(), viewPoint.zoom), event)
                            //If no selection, drag map
                            viewPoint = viewPoint.moveBy(
                                -dragAmount.x.toDp(),
                                dragAmount.y.toDp()
                            )

                        }
                    }

                    // evaluate selection
                    selectRect?.let { rect ->
                        //Use selection override if it is defined
                        val coordinateRect = space.Rectangle(
                            rect.topLeft.toCoordinates(),
                            rect.bottomRight.toCoordinates()
                        )
                        config.onSelect(coordinateRect)
                        if (config.zoomOnSelect) {
                            viewPoint = viewPointFor(coordinateRect)
                        }
                        selectRect = null
                    }
                }
            }
        }
    }.onPointerEvent(PointerEventType.Scroll) {
        val change = it.changes.first()
        val (xPos, yPos) = change.position
        //compute invariant point of translation
        val invariant = DpOffset(xPos.toDp(), yPos.toDp()).toCoordinates()
        viewPoint = with(space) {
            viewPoint.zoomBy(-change.scrollDelta.y.toDouble() * config.zoomSpeed, invariant)
        }
    }
}