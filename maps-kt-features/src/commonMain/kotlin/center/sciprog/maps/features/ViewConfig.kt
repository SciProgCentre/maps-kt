package center.sciprog.maps.features

import androidx.compose.ui.unit.DpSize

public data class ViewConfig<T : Any>(
    val zoomSpeed: Float = 1f / 3f,
    val onClick: MouseListener<T>? = null,
    val dragHandle: DragHandle<T>? = null,
    val onViewChange: ViewPoint<T>.() -> Unit = {},
    val onSelect: (Rectangle<T>) -> Unit = {},
    val zoomOnSelect: Boolean = true,
    val onCanvasSizeChange: (DpSize) -> Unit = {},
)