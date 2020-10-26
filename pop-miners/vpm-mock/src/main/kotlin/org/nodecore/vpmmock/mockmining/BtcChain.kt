package org.nodecore.vpmmock.mockmining

class BtcChain(
    val minHeight: Int = 0,
    tip: BtcBlockIndex,
) {
    private var chain = ArrayList<BtcBlockIndex>()

    init {
        chain.add(tip)
    }

    // get by height
    operator fun get(index: Int): BtcBlockIndex? {
        if (index < minHeight || index >= chain.size - minHeight) return null
        return chain[index - minHeight]
    }

    fun contains(block: BtcBlockIndex): Boolean {
        val inner = get(block.height)
        return inner == block
    }

    val tip: BtcBlockIndex
        get() = chain[chain.size - 1]

    val first: BtcBlockIndex
        get() = chain[0]

    fun setTip(block: BtcBlockIndex) {
        if (chain.isNotEmpty() && tip == block.prev) {
            chain.add(block)
            return
        }

        chain.ensureCapacity(block.height + 1)

        // FIXME: workaround for ArrayList to allow usage of operator[] in next while loop
        while (chain.size <= block.height) {
            // this `block` will be overwritten in the next loop anyway
            chain.add(block)
        }

        var cursor: BtcBlockIndex? = block
        while (cursor != null && !contains(cursor)) {
            chain[cursor.height - minHeight] = cursor
            cursor = cursor.prev
        }
    }

    // finds a fork between current tip and a block
    fun findFork(block: BtcBlockIndex, maxSearchDistance: Int = Integer.MAX_VALUE): BtcBlockIndex? {
        var cursor: BtcBlockIndex? = null
        val lastHeight = tip.height
        if (block.height > lastHeight) {
            cursor = block.getAncestorAtHeight(lastHeight)
        }
        var i = 0
        while (cursor != null && !contains(cursor) && i++ < maxSearchDistance) {
            cursor = cursor.prev
        }
        return cursor
    }

    fun getLast(n: Int): List<BtcBlockIndex> {
        if (n > chain.size) {
            return chain
        }

        return chain.subList(chain.size - n, chain.size)
    }
}
