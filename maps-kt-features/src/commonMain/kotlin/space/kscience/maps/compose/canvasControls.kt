package space.kscience.maps.compose

import androidx.compose.foundation.gestures.drag
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.DpRect
import androidx.compose.ui.unit.dp
import space.kscience.maps.features.*
import kotlin.math.max
import kotlin.math.min


/**
 * Create a modifier for Map/Scheme canvas controls on desktop
 * @param features a collection of features to be rendered in descending [ZAttribute] order
 */
public fun <T : Any> Modifier.canvasControls(
    state: CanvasState<T>,
    features: FeatureStore<T>,
): Modifier = with(state) {

//    //selecting all tapabales ahead of time
//    val allTapable = buildMap {
//        features.forEachWithAttribute(TapListenerAttribute) { _, feature, listeners ->
//            put(feature, listeners)
//        }
//    }

    pointerInput(Unit) {
        awaitPointerEventScope {
            while (true) {
                val event = awaitPointerEvent()
                val coordinates = toCoordinates(event.changes.first().position, this)
                val point = state.space.ViewPoint(coordinates, zoom)

                if (event.type == PointerEventType.Move) {
                    features.forEachWithAttribute(HoverListenerAttribute) { _, feature, listeners ->
                        if (point in feature as DomainFeature) {
                            listeners.forEach { it.handle(event, point) }
                            return@forEachWithAttribute
                        }
                    }
                }
            }
        }
    }.pointerInput(Unit) {
        detectClicks(
            onDoubleClick = if (viewConfig.zoomOnDoubleClick) {
                { event ->
                    val invariant = toCoordinates(event.position, this)
                    viewPoint = with(space) {
                        viewPoint.zoomBy(
                            if (event.buttons.isPrimaryPressed) 1f else if (event.buttons.isSecondaryPressed) -1f else 0f,
                            invariant
                        )
                    }
                }
            } else null,
            onClick = { event ->
                val coordinates = toCoordinates(event.position, this)
                val point = space.ViewPoint(coordinates, zoom)

                viewConfig.onClick?.handle(
                    event,
                    point
                )

                features.forEachWithAttributeUntil(ClickListenerAttribute) {_, feature, listeners ->
                    if (point in (feature as DomainFeature)) {
                        listeners.forEach { it.handle(event, point) }
                        false
                    } else {
                        true
                    }
                }
            }
        )
    }.pointerInput(Unit) {
        awaitPointerEventScope {
            while (true) {
                val event: PointerEvent = awaitPointerEvent()
                event.changes.forEach { change ->

                    if (event.type == PointerEventType.Scroll) {
                        val (xPos, yPos) = change.position
                        //compute invariant point of translation
                        val invariant = DpOffset(xPos.toDp(), yPos.toDp()).toCoordinates()
                        viewPoint = with(space) {
                            viewPoint.zoomBy(-change.scrollDelta.y * viewConfig.zoomSpeed, invariant)
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

                        //apply drag handle and check if it prohibits the drag even propagation
                        if (selectionStart == null) {
                            val dragStart = space.ViewPoint(
                                toCoordinates(dragChange.previousPosition, this),
                                zoom
                            )
                            val dragEnd = space.ViewPoint(
                                toCoordinates(dragChange.position, this),
                                zoom
                            )
                            val dragResult = viewConfig.dragHandle?.handle(event, dragStart, dragEnd)
                            if (dragResult?.handleNext == false) return@drag

                            var continueAfter = true

                            features.forEachWithAttributeUntil(DraggableAttribute) { _, _, handler ->
                                handler.handle(event, dragStart, dragEnd).handleNext.also {
                                    if (!it) continueAfter = false
                                }
                            }

                            if (!continueAfter) return@drag
                        }

                        if (event.buttons.isPrimaryPressed) {
                            //If the selection process is started, modify the frame
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
                        viewConfig.onSelect(coordinateRect)
                        if (viewConfig.zoomOnSelect) {
                            viewPoint = computeViewPoint(coordinateRect)
                        }
                        selectRect = null
                    }
                }
            }
        }
    }
}

/*
.pointerInput(Unit) {
        allTapable.forEach { (feature, listeners) ->
            listeners.forEach { listener ->
                detectTapGestures(listener.pointerMatcher, listener.keyboardFilter) { offset ->
                    val point = space.ViewPoint(offset.toCoordinates(this@pointerInput), zoom)
                    if (point in feature as DomainFeature) {
                        listener.onTap(point)
                    }
                }
            }
        }
    }
 */