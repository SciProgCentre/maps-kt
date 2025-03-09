@file:Suppress("DEPRECATION")

package space.kscience.maps.compose

import kotlin.jvm.Synchronized


internal class LruCache<K, V>(
    private var capacity: Int,
) {
    private val cache = linkedMapOf<K, V>()

    @Synchronized
    fun put(key: K, value: V){
        if (cache.size >= capacity) {
            cache.remove(cache.iterator().next().key)
        }
        cache[key] = value
    }

    operator fun get(key: K): V? {
        val value = cache[key]
        if (value != null) {
            cache.remove(key)
            cache[key] = value
        }
        return value
    }

    @Synchronized
    fun remove(key: K) {
        cache.remove(key)
    }

    @Synchronized
    fun getOrPut(key: K, factory: () -> V): V = get(key) ?: factory().also { put(key, it) }

    @Synchronized
    fun reset(newCapacity: Int? = null) {
        cache.clear()
        capacity = newCapacity ?: capacity
    }

}
