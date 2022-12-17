package center.sciprog.maps.features

import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.isPrimaryPressed

public fun interface DragHandle<in V: ViewPoint<*>> {
    /**
     * @param event - qualifiers of the event used for drag
     * @param start - is a point where drag begins, end is a point where drag ends
     * @param end - end point of the drag
     *
     * @return true if default event processors should be used after this one
     */
    public fun handle(event: PointerEvent, start: V, end: V): Boolean

    public companion object {
        public val BYPASS: DragHandle<*> = DragHandle<ViewPoint<*>> { _, _, _ -> true }

        /**
         * Process only events with primary button pressed
         */
        public fun <V> withPrimaryButton(
            block: (event: PointerEvent, start: V, end: V) -> Boolean,
        ): DragHandle<V> = DragHandle { event, start, end ->
            if (event.buttons.isPrimaryPressed) {
                block(event, start, end)
            } else {
                true
            }
        }

        /**
         * Combine several handles into one
         */
        public fun <V> combine(vararg handles: DragHandle<V>): DragHandle<V> = DragHandle { event, start, end ->
            handles.forEach {
                if (!it.handle(event, start, end)) return@DragHandle false
            }
            return@DragHandle true
        }
    }
}