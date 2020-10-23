package org.veriblock.core.miner

import org.veriblock.core.bitcoinj.BitcoinUtilities
import org.veriblock.core.crypto.PreviousKeystoneVbkHash
import org.veriblock.core.crypto.Sha256Hash
import org.veriblock.core.params.NetworkParameters
import org.veriblock.sdk.blockchain.VeriBlockDifficultyCalculator
import org.veriblock.sdk.models.VeriBlockBlock
import org.veriblock.sdk.services.ValidationService
import java.util.*
import kotlin.math.max
import kotlin.random.Random

fun getNextWorkRequired(
    prev: VeriBlockBlock,
    networkParams: NetworkParameters,
    // chain of at most 100 blocks ending with 'prev'
    // can be less than 100 ONLY if blockchain contains less than 100 blocks
    context: List<VeriBlockBlock>
): Int {
    return BitcoinUtilities.encodeCompactBits(
        VeriBlockDifficultyCalculator.calculate(networkParams, prev, context)
    ).toInt()
}

private const val KEYSTONE_INTERVAL = 20

// returns ancestor of `this` at height `height`
fun getAncestorAtHeight(
    block: VeriBlockBlock,
    height: Int,
    getPreviousBlock: (block: VeriBlockBlock) -> VeriBlockBlock?
): VeriBlockBlock? {
    if (height < 0 || height > block.height) {
        return null
    }

    // in O(n) seek backwards until we hit valid height
    var cursor: VeriBlockBlock? = block
    while (cursor != null && cursor.height > height) {
        cursor = getPreviousBlock(cursor)
    }

    return cursor
}


fun getBlockTemplate(
    prev: VeriBlockBlock,
    // put random merkle root for uniqueness
    merkleRoot: Sha256Hash,
    networkParams: NetworkParameters,
    context: List<VeriBlockBlock>,
    getPreviousBlock: (block: VeriBlockBlock) -> VeriBlockBlock?
): VeriBlockBlock {
    val version = prev.version
    val previousBlock = prev.hash.trimToPreviousBlockSize()
    val height = prev.height + 1

    var diff = prev.height % KEYSTONE_INTERVAL
    // we do not use previous block as a keystone
    if (diff == 0) {
        diff += KEYSTONE_INTERVAL
    }

    var prevKeystone: VeriBlockBlock? = null
    if (diff <= prev.height) {
        prevKeystone = getAncestorAtHeight(prev, prev.height - diff, getPreviousBlock)
    }

    diff += KEYSTONE_INTERVAL
    var secondPrevKeystone: VeriBlockBlock? = null
    if (diff <= prev.height) {
        secondPrevKeystone = getAncestorAtHeight(prev, prev.height - diff, getPreviousBlock)
    }

    val timestamp = max(prev.timestamp, System.currentTimeMillis().toInt())
    val difficulty = getNextWorkRequired(prev, networkParams, context)
    val zeroKeystone = PreviousKeystoneVbkHash.EMPTY_HASH

    return VeriBlockBlock(
        height = height,
        version = version,
        previousBlock = previousBlock,
        previousKeystone = prevKeystone?.hash?.trimToPreviousKeystoneSize() ?: zeroKeystone,
        secondPreviousKeystone = secondPrevKeystone?.hash?.trimToPreviousKeystoneSize() ?: zeroKeystone,
        merkleRoot = merkleRoot,
        timestamp = timestamp,
        difficulty = difficulty,
        nonce = 0 /* not yet mined */
    )
}

private val rnd = Random(0)

fun randomMerkleRoot(): Sha256Hash {
    return Sha256Hash.wrap(rnd.nextBytes(32))
}

// first, create a template with getBlockTemplate, then mine a block
fun mineVbkBlock(template: VeriBlockBlock): VeriBlockBlock {
    do template.nonce++
    while (!ValidationService.isProofOfWorkValid(template))
    return template
}

// helper to generate mining context (e.g. `context` argument in mineVbkChain)
fun getMiningContext(
    block: VeriBlockBlock,
    size: Int = 100,
    getPreviousBlock: (block: VeriBlockBlock) -> VeriBlockBlock?
): LinkedList<VeriBlockBlock> {
    val ctx = LinkedList<VeriBlockBlock>()
    var cursor: VeriBlockBlock? = block
    var i = 0
    while (cursor != null && i++ < size) {
        ctx.add(cursor)
        cursor = getPreviousBlock(cursor)
    }
    return ctx
}

// mine `size` blocks starting at `prev`
fun mineVbkChain(
    prev: VeriBlockBlock,
    networkParams: NetworkParameters,
    size: Int,
    getPreviousBlock: (block: VeriBlockBlock) -> VeriBlockBlock?
): List<VeriBlockBlock> {
    return vbkBlockGenerator(prev, networkParams, getPreviousBlock).take(size).toList()
}

// yielding generator which generates blocks
fun vbkBlockGenerator(
    prev: VeriBlockBlock,
    networkParams: NetworkParameters,
    getPreviousBlock: (block: VeriBlockBlock) -> VeriBlockBlock?
): Sequence<VeriBlockBlock> = sequence {
    val chain = getMiningContext(prev, /*retargeting period=*/100, getPreviousBlock)
    var last: VeriBlockBlock = prev
    while (true) {
        // get template for next block
        val tmpl = getBlockTemplate(
            last,
            randomMerkleRoot(),
            networkParams,
            chain,
            getPreviousBlock
        )
        // mine block
        val block = mineVbkBlock(tmpl)

        chain.add(block)
        while (chain.size > 100) {
            // pop first element until chain size is 100
            chain.pop()
        }
        assert(chain.size <= 100)

        last = block
        yield(last)
    }
}
