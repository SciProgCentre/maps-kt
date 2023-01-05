package center.sciprog.attributes

import androidx.compose.ui.graphics.Color
import center.sciprog.maps.features.DragHandle
import center.sciprog.maps.features.DragListener
import center.sciprog.maps.features.FloatRange
import center.sciprog.maps.features.MouseListener

public interface Attribute<T>

public object ZAttribute : Attribute<Float>

public object DraggableAttribute : Attribute<DragHandle<Any>>

public interface SetAttribute<V> : Attribute<Set<V>>

public object DragListenerAttribute : SetAttribute<DragListener<Any>>

public object ClickListenerAttribute : SetAttribute<MouseListener<Any>>

public object HoverListenerAttribute : SetAttribute<MouseListener<Any>>

public object VisibleAttribute : Attribute<Boolean>

public object ColorAttribute : Attribute<Color>

public object ZoomRangeAttribute : Attribute<FloatRange>

public object AlphaAttribute : Attribute<Float>
