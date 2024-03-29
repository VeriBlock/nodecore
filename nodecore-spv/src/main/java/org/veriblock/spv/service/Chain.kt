package org.veriblock.spv.service

import java.math.BigInteger
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write

class Chain(
    tip: BlockIndex,
    // cached cumulative work of current tip
    var tipWork: BigInteger
) {
    private var chain = ArrayList<BlockIndex>()

    val lock = ReentrantReadWriteLock()

    init {
        chain.add(tip)
    }

    // get by height
    operator fun get(index: Int): BlockIndex? = lock.read {
        if (index < 0 || index >= chain.size) return null
        return chain[index]
    }

    fun contains(block: BlockIndex): Boolean {
        val inner = get(block.height)
        return inner == block
    }

    val tip: BlockIndex
        get() = lock.read {
            chain[chain.size - 1]
        }

    val first: BlockIndex
        get() = chain[0]

    fun setTip(block: BlockIndex, cumulativeWork: BigInteger) = lock.write {
        tipWork = cumulativeWork
        if (chain.isNotEmpty() && tip == block.prev) {
            chain.add(block)
            return
        }

        chain.ensureCapacity(block.height + 1)

        // FIXME: workaround for ArrayList to allow usage of operator[] in next while loop
        while (chain.size <= block.height) {
            chain.add(block)
        }

        var cursor: BlockIndex? = block.prev
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
