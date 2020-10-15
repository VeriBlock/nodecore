package org.veriblock.spv.service

class LRUCache<K, V>(private val cacheSize: Int) :
    LinkedHashMap<K, V>(
        /*initSize*/cacheSize,
        /*loadFactor*/0.75f,
        /*accessOrder*/ true
    ) {
    override fun removeEldestEntry(
        eldest: MutableMap.MutableEntry<K, V>?
    ): Boolean {
        return this.size > cacheSize
    }
}
