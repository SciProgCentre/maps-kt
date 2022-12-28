package center.sciprog.maps.features

import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.isPrimaryPressed
import androidx.compose.ui.unit.DpSize

public fun interface ClickHandle<T : Any> {
    public fun handle(event: PointerEvent, click: ViewPoint<T>): Unit

    public companion object {
        public fun <T : Any> withPrimaryButton(
            block: (event: PointerEvent, click: ViewPoint<T>) -> Unit,
        ): ClickHandle<T> = ClickHandle { event, click ->
            if (event.buttons.isPrimaryPressed) {
                block(event, click)
            }
        }
    }
}

public data class ViewConfig<T : Any>(
    val zoomSpeed: Float = 1f / 3f,
    val onClick: ClickHandle<T>? = null,
    val dragHandle: DragHandle<T>? = null,
    val onViewChange: ViewPoint<T>.() -> Unit = {},
    val onSelect: (Rectangle<T>) -> Unit = {},
    val zoomOnSelect: Boolean = true,
    val onCanvasSizeChange: (DpSize) -> Unit = {},
)