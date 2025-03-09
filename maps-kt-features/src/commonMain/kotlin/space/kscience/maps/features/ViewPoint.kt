package space.kscience.maps.features

/**
 * @param T type of coordinates used for the view point
 */
public interface ViewPoint<out T: Any> {
    public val focus: T
    public val zoom: Float
}