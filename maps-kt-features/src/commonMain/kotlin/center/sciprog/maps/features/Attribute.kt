package center.sciprog.maps.features

import androidx.compose.ui.graphics.Color

public interface Attribute<T>

public object ZAttribute : Attribute<Float>

public object DraggableAttribute : Attribute<DragHandle<Any>>

public object DragListenerAttribute : Attribute<Set<DragListener<Any>>>

public object ClickListenerAttribute : Attribute<Set<MouseListener<Any>>>

public object HoverListenerAttribute : Attribute<Set<MouseListener<Any>>>

public object VisibleAttribute : Attribute<Boolean>

public object ColorAttribute : Attribute<Color>

public object AlphaAttribute : Attribute<Float>
