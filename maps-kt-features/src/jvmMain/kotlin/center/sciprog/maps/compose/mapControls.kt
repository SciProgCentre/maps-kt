package center.sciprog.maps.compose

import androidx.compose.foundation.gestures.drag
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.DpRect
import androidx.compose.ui.unit.dp
import center.sciprog.maps.features.CoordinateViewScope
import center.sciprog.maps.features.bottomRight
import center.sciprog.maps.features.topLeft
import kotlin.math.max
import kotlin.math.min


@OptIn(ExperimentalComposeUiApi::class)
public fun <T : Any> Modifier.mapControls(
    state: CoordinateViewScope<T>,
): Modifier = with(state) {
    pointerInput(Unit) {
        fun Offset.toDpOffset() = DpOffset(x.toDp(), y.toDp())
        awaitPointerEventScope {
            while (true) {
                val event = awaitPointerEvent()
                if (event.type == PointerEventType.Release ) {
                    config.onClick?.handle(
                        event,
                        space.ViewPoint(event.changes.first().position.toDpOffset().toCoordinates(), zoom)
                    )
                }
            }
        }
    }.pointerInput(Unit) {
        fun Offset.toDpOffset() = DpOffset(x.toDp(), y.toDp())
        awaitPointerEventScope {
            while (true) {
                val event: PointerEvent = awaitPointerEvent()
                event.changes.forEach { change ->

                    if (event.type == PointerEventType.Scroll) {
                        val (xPos, yPos) = change.position
                        //compute invariant point of translation
                        val invariant = DpOffset(xPos.toDp(), yPos.toDp()).toCoordinates()
                        viewPoint = with(space) {
                            viewPoint.zoomBy(-change.scrollDelta.y * config.zoomSpeed, invariant)
                        }
                        change.consume()
                    }
                    //val dragStart = change.position
                    //val dpPos = DpOffset(dragStart.x.toDp(), dragStart.y.toDp())

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
                        if (selectionStart == null) {
                            val dragResult = config.dragHandle?.handle(
                                event,
                                space.ViewPoint(dpStart.toCoordinates(), zoom),
                                space.ViewPoint(dpEnd.toCoordinates(), zoom)
                            )
                            if (dragResult?.handleNext == false) return@drag
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
                            viewPoint = computeViewPoint(coordinateRect)
                        }
                        selectRect = null
                    }
                }
            }
        }
    }
}