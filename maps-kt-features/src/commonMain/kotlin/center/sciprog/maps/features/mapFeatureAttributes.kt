package center.sciprog.maps.features

import androidx.compose.ui.graphics.Color
import center.sciprog.attributes.Attribute
import center.sciprog.attributes.SetAttribute

public object ZAttribute : Attribute<Float>

public object DraggableAttribute : Attribute<DragHandle<Any>>

public object DragListenerAttribute : SetAttribute<DragListener<Any>>

public object ClickListenerAttribute : SetAttribute<MouseListener<Any>>

public object HoverListenerAttribute : SetAttribute<MouseListener<Any>>

public object VisibleAttribute : Attribute<Boolean>

public object ColorAttribute : Attribute<Color>

public object ZoomRangeAttribute : Attribute<FloatRange>

public object AlphaAttribute : Attribute<Float>