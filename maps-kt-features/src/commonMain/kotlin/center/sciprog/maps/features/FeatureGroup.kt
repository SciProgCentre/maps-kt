package center.sciprog.maps.features

///**
// * A group of other features
// */
//public data class FeatureGroup<T : Any>(
//    val parentBuilder: FeatureBuilder<T>,
//    private val groupId: String,
//    public override val zoomRange: FloatRange,
//    override val attributes: Attributes = Attributes.EMPTY,
//) : FeatureBuilder<T>, Feature<T> {
//
//    override val space: CoordinateSpace<T> get() = parentBuilder.space
//
//    override fun generateUID(feature: Feature<T>?): String = parentBuilder.generateUID(feature)
//
//    override fun <F : Feature<T>> feature(id: String?, feature: F): FeatureId<F> =
//        parentBuilder.feature("${groupId}.${id ?: parentBuilder.generateUID(feature)}", feature)
//
//    override fun <F : Feature<T>, V> FeatureId<F>.withAttribute(key: Attribute<V>, value: V?): FeatureId<F> =
//        with(parentBuilder) {
//            FeatureId<F>("${groupId}.${this@withAttribute.id}").withAttribute(key, value)
//        }
//
//    override fun getBoundingBox(zoom: Float): Rectangle<T>? {
//        TODO("Not yet implemented")
//    }
//
//    override fun withAttributes(modify: Attributes.() -> Attributes): Feature<T> =
//        copy(attributes = attributes.modify())
//}
//
//public fun <T : Any> FeatureBuilder<T>.group(
//    zoomRange: FloatRange = defaultZoomRange,
//    id: String? = null,
//    builder: FeatureBuilder<T>.() -> Unit,
//): FeatureId<FeatureGroup<T>> = feature(id, FeatureGroup(this, id ?: generateUID(null), zoomRange).apply(builder))
