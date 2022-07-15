package centre.sciprog.maps

import kotlin.jvm.Synchronized


class LruCache<K, V>(
    private var capacity: Int,
) {
    private val cache = linkedMapOf<K, V>()

    @Synchronized
    fun getCache() = cache.toMap()

    @Synchronized
    fun put(key: K, value: V) = internalPut(key, value)

    @Synchronized
    operator fun get(key: K) = internalGet(key)

    @Synchronized
    fun remove(key: K) {
        cache.remove(key)
    }

    @Synchronized
    fun getOrPut(key: K, callback: () -> V): V {
        val internalGet = internalGet(key)
        return internalGet ?: callback().also { internalPut(key, it) }
    }

    @Synchronized
    fun clear(newCapacity: Int? = null) {
        cache.clear()
        capacity = newCapacity ?: capacity
    }

    private fun internalGet(key: K): V? {
        val value = cache[key]
        if (value != null) {
            cache.remove(key)
            cache[key] = value
        }
        return value
    }

    private fun internalPut(key: K, value: V) {
        if (cache.size >= capacity) {
            cache.remove(cache.iterator().next().key)
        }
        cache[key] = value
    }

}
