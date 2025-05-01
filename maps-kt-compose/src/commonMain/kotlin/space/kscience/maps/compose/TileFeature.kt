package space.kscience.maps.compose

import space.kscience.attributes.Attributes
import space.kscience.maps.coordinates.Gmc
import space.kscience.maps.features.CoordinateSpace
import space.kscience.maps.features.CustomFeature
import space.kscience.maps.features.Feature
import space.kscience.maps.features.Rectangle

/**
 * Represents a custom feature associated with a specific map tile provider.
 *
 * This feature is designed to handle map tiles using the specified [MapTileProvider]
 * and includes functionality for defining and modifying its attributes and spatial
 * characteristics.
 *
 * @property space Defines the coordinate space used for this feature, enabling
 * manipulation and operations in the map/scheme coordinate context.
 * @property tileProvider The provider responsible for asynchronous loading and
 * management of map tiles.
 * @property attributes A collection of attributes associated with this feature,
 * which can be modified through the `withAttributes` method.
 */
public data class TileFeature(
    override val space: CoordinateSpace<Gmc>,
    public val tileProvider: MapTileProvider,
    override val attributes: Attributes
) : CustomFeature<Gmc> {
    override fun getBoundingBox(zoom: Float): Rectangle<Gmc>? = null

    override fun withAttributes(modify: Attributes.() -> Attributes): Feature<Gmc> = copy(attributes = modify(attributes))
}