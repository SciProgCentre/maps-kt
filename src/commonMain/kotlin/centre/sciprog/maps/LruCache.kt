package centre.sciprog.maps

import kotlin.jvm.Synchronized


class LruCache<K, V>(
    private var capacity: Int,
) {

    private val cache = mutableMapOf<K, V>()
    private val order = mutableListOf<K>()

    @Synchronized
    fun getCache() = cache.toMap()

    @Synchronized
    fun getOrder() = order.toList()

    @Synchronized
    fun put(key: K, value: V) = internalPut(key, value)

    @Synchronized
    operator fun get(key: K) = internalGet(key)

    @Synchronized
    fun remove(key: K) {
        cache.remove(key)
        order.remove(key)
    }

    @Synchronized
    fun getOrPut(key: K, callback: () -> V): V {
        val internalGet = internalGet(key)
        return internalGet ?: callback().also { internalPut(key, it) }
    }

    @Synchronized
    fun clear(newCapacity: Int? = null) {
        cache.clear()
        order.clear()
        capacity = newCapacity ?: capacity
    }

    private fun internalGet(key: K): V? {
        val value = cache[key]
        if (value != null) {
            order.remove(key)
            order.add(key)
        }
        return value
    }

    private fun internalPut(key: K, value: V) {
        if (cache.size >= capacity) {
            cache.remove(order.removeAt(0))
        }
        cache[key] = value
        order.add(key)
    }

}
