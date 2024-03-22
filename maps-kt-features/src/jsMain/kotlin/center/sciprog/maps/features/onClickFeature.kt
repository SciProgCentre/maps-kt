package center.sciprog.maps.features

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.PointerKeyboardModifiers

@OptIn(ExperimentalFoundationApi::class)
public class DesktopPointerMatcher(private val matcher: androidx.compose.foundation.PointerMatcher) : PointerMatcher {
    override fun matches(event: PointerEvent): Boolean {
        return matcher.matches(event)
    }

}

@OptIn(ExperimentalFoundationApi::class)
@Suppress("UNCHECKED_CAST")
public actual fun <T : Any, F : DomainFeature<T>> FeatureRef<T, F>.onClick(
    pointerMatcher: PointerMatcher,
    keyboardModifiers: PointerKeyboardModifiers.() -> Boolean,
    onClick: PointerEvent.(click: ViewPoint<T>) -> Unit,
): FeatureRef<T, F> = modifyAttributes {
    ClickListenerAttribute.add(
        MouseListener { event, point ->
            if (pointerMatcher.matches(event) && keyboardModifiers(event.keyboardModifiers)) {
                event.onClick(point as ViewPoint<T>)
            }
        }
    )
}