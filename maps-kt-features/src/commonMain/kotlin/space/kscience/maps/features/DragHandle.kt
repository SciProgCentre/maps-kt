package space.kscience.maps.features

import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.isPrimaryPressed

/**
 * @param result - the endpoint of the drag to perform constrained drag
 * @param handleNext - if false do not evaluate subsequent drag handles
 */
public data class DragResult<T : Any>(val result: ViewPoint<T>, val handleNext: Boolean = true)

public fun interface DragListener<in T : Any> {
    public fun handle(event: PointerEvent, from: ViewPoint<T>, to: ViewPoint<T>)
}

public fun interface DragHandle<T : Any> {
    /**
     * @param event - qualifiers of the event used for drag
     * @param start - is a point where drag begins, end is a point where drag ends
     * @param end - end point of the drag
     *
     * @return true if default event processors should be used after this one
     */
    public fun handle(event: PointerEvent, start: ViewPoint<T>, end: ViewPoint<T>): DragResult<T>

    public companion object {
        public fun <T : Any> bypass(): DragHandle<T> = DragHandle<T> { _, _, end -> DragResult(end) }

        /**
         * Process only events with primary button pressed
         */
        public fun <T : Any> withPrimaryButton(
            block: (event: PointerEvent, start: ViewPoint<T>, end: ViewPoint<T>) -> DragResult<T>,
        ): DragHandle<T> = DragHandle { event, start, end ->
            if (event.buttons.isPrimaryPressed) {
                block(event, start, end)
            } else {
                DragResult(end)
            }
        }

        /**
         * Combine several handles into one
         */
        public fun <T : Any> combine(vararg handles: DragHandle<T>): DragHandle<T> = DragHandle { event, start, end ->
            var current: ViewPoint<T> = end
            handles.forEach {
                val result = it.handle(event, start, current)
                if (!result.handleNext) return@DragHandle result else {
                    current = result.result
                }
            }
            return@DragHandle DragResult(current)
        }
    }
}