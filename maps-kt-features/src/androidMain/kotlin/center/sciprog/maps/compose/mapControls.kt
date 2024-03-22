package center.sciprog.maps.compose

import android.annotation.SuppressLint
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroidSize
import androidx.compose.foundation.gestures.drag
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.util.fastAny
import androidx.compose.ui.util.fastForEach
import center.sciprog.maps.features.ClickListenerAttribute
import center.sciprog.maps.features.CoordinateViewScope
import center.sciprog.maps.features.DomainFeature
import center.sciprog.maps.features.DraggableAttribute
import center.sciprog.maps.features.FeatureGroup
import center.sciprog.maps.features.ZAttribute
import center.sciprog.maps.features.forEachWithAttributeUntil


/**
 * Create a modifier for Map/Scheme canvas controls on desktop
 * @param features a collection of features to be rendered in descending [ZAttribute] order
 */
@SuppressLint("ModifierFactoryUnreferencedReceiver")
public fun <T : Any> Modifier.mapControls(
    state: CoordinateViewScope<T>,
    features: FeatureGroup<T>,
): Modifier = with(state) {
    pointerInput(Unit) {
        awaitEachGesture {
            awaitFirstDown(requireUnconsumed = false)
            do {
                val event = awaitPointerEvent()
                val canceled = event.changes.fastAny { it.isConsumed }
                if (!canceled) {
                    event.changes.fastForEach { change ->
                        val (xPos, yPos) = change.position
                        val zoomChange = event.calculateZoom()
                        if (zoomChange != 0f) {
                            val invariant = DpOffset(xPos.toDp(), yPos.toDp()).toCoordinates()
                            viewPoint = with(space) {
                                viewPoint.zoomBy(0.1f * zoomChange * config.zoomSpeed, invariant)
                            }
                        }
                        change.consume()
                    }
                }
            } while (!canceled && event.changes.fastAny { it.pressed })
        }

    }.pointerInput(Unit) {
        awaitEachGesture {
            awaitFirstDown(requireUnconsumed = false)
            do {
                val event = awaitPointerEvent()
                val canceled = event.changes.fastAny { it.isConsumed }
                if (!canceled) {
                    event.changes.fastForEach { change ->
                        if (change.positionChanged()) {
                            drag(change.id) { dragChange ->
                                val dragAmount: Offset = dragChange.position - dragChange.previousPosition
                                val dragStart = space.ViewPoint(
                                    dragChange.previousPosition.toCoordinates(this),
                                    zoom
                                )
                                val dragEnd = space.ViewPoint(
                                    dragChange.position.toCoordinates(this),
                                    zoom
                                )
                                val dragResult = config.dragHandle?.handle(event, dragStart, dragEnd)
                                if (dragResult?.handleNext == false) return@drag

                                var continueAfter = true

                                features.forEachWithAttributeUntil(DraggableAttribute) { _, _, handler ->
                                    handler.handle(event, dragStart, dragEnd).handleNext.also {
                                        if (!it) continueAfter = false
                                    }
                                }

                                if (!continueAfter) return@drag

                                viewPoint = viewPoint.moveBy(
                                    -dragAmount.x.toDp(),
                                    dragAmount.y.toDp()
                                )
                            }
                        }
                    }
                }
            } while (!canceled && event.changes.fastAny { it.pressed })
        }
    }.pointerInput(Unit) {
        detectClicks { event ->
            val coordinates = event.position.toCoordinates(this)
            val point = space.ViewPoint(coordinates, zoom)

            config.onClick?.handle(
                event,
                point
            )

            features.forEachWithAttributeUntil(ClickListenerAttribute) { _, feature, listeners ->
                if (point in (feature as DomainFeature)) {
                    listeners.forEach { it.handle(event, point) }
                    false
                } else {
                    true
                }
            }
        }
    }
}

private fun PointerEvent.calculateZoom(): Float {
    val currentCentroidSize = calculateCentroidSize(useCurrent = true)
    val previousCentroidSize = calculateCentroidSize(useCurrent = false)

    return currentCentroidSize - previousCentroidSize
}
