package org.veriblock.spv.service

import java.math.BigInteger

class Chain(
    tip: BlockIndex,
    // cached cumulative work of current tip
    var tipWork: BigInteger
) {
    private var chain = ArrayList<BlockIndex>()

    init {
        chain.add(tip)
    }

    // get by height
    operator fun get(index: Int): BlockIndex? {
        if (index < 0 || index >= chain.size) return null
        return chain[index]
    }

    fun contains(block: BlockIndex): Boolean {
        val inner = get(block.height)
        return inner == block
    }

    val tip: BlockIndex
        get() = chain[chain.size - 1]

    val first: BlockIndex
        get() = chain[0]

    fun setTip(block: BlockIndex, cumulativeWork: BigInteger) {
        tipWork = cumulativeWork
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

        var cursor: BlockIndex? = block
        while (cursor != null && !contains(cursor)) {
            chain[cursor.height] = cursor
            cursor = cursor.prev
        }
    }

    // finds a fork between current tip and a block
    fun findFork(block: BlockIndex, maxSearchDistance: Int = Integer.MAX_VALUE): BlockIndex? {
        var cursor: BlockIndex? = null
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

    fun getLast(n: Int): List<BlockIndex> {
        if (n > chain.size) {
            return chain
        }

        return chain.subList(chain.size - n, chain.size)
    }
}
