package center.sciprog.maps.compose

import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.isPrimaryPressed
import center.sciprog.maps.coordinates.MapViewPoint

public fun interface DragHandle {
    /**
     * @param event - qualifiers of the event used for drag
     * @param start - is a point where drag begins, end is a point where drag ends
     * @param end - end point of the drag
     *
     * @return true if default event processors should be used after this one
     */
    public fun handle(event: PointerEvent, start: MapViewPoint, end: MapViewPoint): Boolean

    public companion object {
        public val BYPASS: DragHandle = DragHandle { _, _, _ -> true }

        /**
         * Process only events with primary button pressed
         */
        public fun withPrimaryButton(
            block: (event: PointerEvent, start: MapViewPoint, end: MapViewPoint) -> Boolean,
        ): DragHandle = DragHandle { event, start, end ->
            if (event.buttons.isPrimaryPressed) {
                block(event, start, end)
            } else {
                true
            }
        }

        /**
         * Combine several handles into one
         */
        public fun combine(vararg handles: DragHandle): DragHandle = DragHandle { event, start, end ->
            handles.forEach {
                if (!it.handle(event, start, end)) return@DragHandle false
            }
            return@DragHandle true
        }
    }
}