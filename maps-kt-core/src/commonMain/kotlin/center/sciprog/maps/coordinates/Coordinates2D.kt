package center.sciprog.maps.coordinates

/**
 * A marker interface for flat coordinates
 */
public interface Coordinates2D

public interface CoordinateBox<T: Coordinates2D>{
    public val a: T
    public val b :T
}