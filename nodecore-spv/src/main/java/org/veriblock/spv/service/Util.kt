package org.veriblock.spv.service

import org.veriblock.core.bitcoinj.BitcoinUtilities
import org.veriblock.core.crypto.Sha256Hash
import org.veriblock.core.crypto.VBlakeHash
import org.veriblock.core.params.NetworkParameters
import org.veriblock.sdk.blockchain.VeriBlockDifficultyCalculator
import org.veriblock.sdk.blockchain.store.StoredVeriBlockBlock
import org.veriblock.sdk.models.VeriBlockBlock
import org.veriblock.sdk.services.ValidationService
import kotlin.math.max

// returns ancestor of `this` at height `height`
fun StoredVeriBlockBlock.getAncestorAtHeight(
    height: Int,
    blockStore: BlockStore
): StoredVeriBlockBlock? {
    if (height < 0 || height > this.height) {
        return null
    }

    // in O(n) seek backwards until we hit valid height
    var cursor: StoredVeriBlockBlock? = this
    while (cursor != null && cursor.height > height) {
        cursor = blockStore.readBlock(cursor.header.previousBlock)
    }

    return cursor
}

class StatefulIterable<out T>(wrapped: Sequence<T>): Iterable<T> {
    private val iterator = wrapped.iterator()
    override fun iterator() = iterator
}

// make sequence remember its last state. e.g. sequential .take(...) will not be overlapping
fun <T> Sequence<T>.asStateful(): Sequence<T> = StatefulIterable(this).asSequence()
