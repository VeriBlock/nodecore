package org.veriblock.spv.service

import org.veriblock.sdk.blockchain.store.StoredVeriBlockBlock
import java.lang.IllegalStateException
import kotlin.math.abs

// maximum number of blocks that can be reorganized in VBK chain
const val VBK_MAX_REORG_DISTANCE = 2000

// note: 2001 because max reorg distance is 2000,
// and very first block in this chain is first block
// that can not be reorganized
private const val blocksToStoreInChain = VBK_MAX_REORG_DISTANCE + 1

// a class which stores last `blocksToStoreInChain` VBK blocks from active chain in memory
class TruncatedChain(
    val blockStore: BlockStore
) {
    private var chain = ArrayList<StoredVeriBlockBlock>(blocksToStoreInChain)

    init {
        setTip(blockStore.getTip())
    }

    fun get(index: Int): StoredVeriBlockBlock? {
        val inner = first().height
        if (index < inner || inner >= chain.size) return null
        return chain[index - inner]
    }

    fun contains(block: StoredVeriBlockBlock): Boolean {
        val inner = get(block.height)
        return inner == block
    }

    fun tip(): StoredVeriBlockBlock {
        assert(chain.isNotEmpty())
        return chain[chain.size - 1]
    }

    fun first(): StoredVeriBlockBlock {
        assert(chain.isNotEmpty())
        return chain[0]
    }

    fun setTip(block: StoredVeriBlockBlock) {
        val size = Integer.min(block.height + 1, blocksToStoreInChain)
        chain = blockStore.readChainWithTip(block.hash, size)
        blockStore.setTip(block)
    }

    // finds a fork between current tip and a block
    // if fork block is past maxReorgDistance, or fork can not be found - returns null
    fun findFork(block: StoredVeriBlockBlock): StoredVeriBlockBlock? {
        if (abs(tip().height - block.height) > VBK_MAX_REORG_DISTANCE) {
            return null
        }

        var cursor: StoredVeriBlockBlock? = null
        val lastHeight = tip().height
        if (block.height > lastHeight) {
            cursor = block.getAncestorAtHeight(lastHeight, blockStore)
        }
        while (cursor != null && !contains(cursor)) {
            cursor = blockStore.readBlock(cursor.header.previousBlock)
        }
        return cursor
    }

    fun getLast(n: Int): List<StoredVeriBlockBlock> {
        if (n > chain.size) {
            return chain
        }

        return chain.subList(chain.size - n, chain.size)
    }
}
