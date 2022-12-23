package center.sciprog.maps.features

/**
 * @param T type of coordinates used for the view point
 */
public interface ViewPoint<T: Any> {
    public val focus: T
    public val zoom: Double
}