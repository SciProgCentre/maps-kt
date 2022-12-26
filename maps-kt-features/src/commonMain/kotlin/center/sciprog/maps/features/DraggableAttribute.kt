package center.sciprog.maps.features


public object DraggableAttribute : Feature.Attribute<DragHandle<Any>>

public object DragListenerAttribute : Feature.Attribute<Set<(begin: Any, end: Any) -> Unit>>