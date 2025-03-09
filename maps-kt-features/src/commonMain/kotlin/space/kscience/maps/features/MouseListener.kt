package space.kscience.maps.features

import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.isPrimaryPressed

public fun interface MouseListener<in T : Any> {
    public fun handle(event: PointerEvent, point: ViewPoint<T>): Unit

    public companion object {
        public fun <T : Any> withPrimaryButton(
            block: (event: PointerEvent, click: ViewPoint<T>) -> Unit,
        ): MouseListener<T> = MouseListener { event, click ->
            if (event.buttons.isPrimaryPressed) {
                block(event, click)
            }
        }
    }
}

//@OptIn(ExperimentalFoundationApi::class)
//public class TapListener<in T : Any>(
//    public val pointerMatcher: PointerMatcher,
//    public val keyboardFilter: PointerKeyboardModifiers.() -> Boolean = { true },
//    public val onTap: (point: ViewPoint<T>) -> Unit,
//)