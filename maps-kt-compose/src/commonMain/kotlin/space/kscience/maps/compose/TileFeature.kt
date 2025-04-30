package space.kscience.maps.compose

import space.kscience.attributes.Attributes
import space.kscience.maps.coordinates.Gmc
import space.kscience.maps.features.CoordinateSpace
import space.kscience.maps.features.CustomFeature
import space.kscience.maps.features.Feature
import space.kscience.maps.features.Rectangle

public data class TileFeature(
    override val space: CoordinateSpace<Gmc>,
    public val tileProvider: MapTileProvider,
    override val attributes: Attributes
) : CustomFeature<Gmc> {
    override fun getBoundingBox(zoom: Float): Rectangle<Gmc>? = null

    override fun withAttributes(modify: Attributes.() -> Attributes): Feature<Gmc> = copy(attributes = modify(attributes))
}