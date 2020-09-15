// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2019 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.
package org.veriblock.sdk.blockchain.store

import org.veriblock.core.crypto.VBlakeHash
import org.veriblock.core.utilities.Utility
import org.veriblock.core.utilities.createLogger
import org.veriblock.sdk.models.BlockStoreException
import org.veriblock.sdk.sqlite.tables.KeyValueData
import org.veriblock.sdk.sqlite.tables.KeyValueRepository
import org.veriblock.sdk.sqlite.tables.VeriBlockBlockRepository
import java.math.BigInteger
import java.sql.Connection
import java.sql.SQLException
import java.util.ArrayList

//private static final int DEFAULT_NUM_HEADERS = 90000;
private val logger = createLogger {}

class VeriBlockStore(
    // underlying database
    private val databaseConnection: Connection
) : BlockStore<StoredVeriBlockBlock, VBlakeHash> {
    private val veriBlockRepository: VeriBlockBlockRepository = VeriBlockBlockRepository(databaseConnection)
    private val keyValueRepository: KeyValueRepository = KeyValueRepository(databaseConnection)
    private val chainHeadRepositoryName = "chainHeadVbk"

    override fun shutdown() {
        try {
            databaseConnection.close()
        } catch (e: SQLException) {
            logger.error("Error closing database connection", e)
        }
    }

    @Throws(SQLException::class)
    override fun clear() {
        veriBlockRepository.clear()
        keyValueRepository.clear()
    }

    @Throws(BlockStoreException::class, SQLException::class)
    override fun getChainHead(): StoredVeriBlockBlock? {
        val headEncoded = keyValueRepository.getValue(chainHeadRepositoryName)
            ?: return null
        return get(VBlakeHash.wrap(Utility.hexToBytes(headEncoded)))
    }

    @Throws(BlockStoreException::class, SQLException::class)
    override fun setChainHead(chainHead: StoredVeriBlockBlock): StoredVeriBlockBlock? {
        if (get(chainHead.hash) == null) {
            throw BlockStoreException("Chain head should reference existing block")
        }
        val previousBlock = getChainHead()
        val headEncoded = Utility.bytesToHex(chainHead.block.hash.bytes)
        val data = KeyValueData()
        data.key = chainHeadRepositoryName
        data.value = headEncoded
        keyValueRepository.save(data.key, data.value)
        return previousBlock
    }

    @Throws(BlockStoreException::class, SQLException::class)
    override fun put(block: StoredVeriBlockBlock) {
        if (veriBlockRepository[block.hash] != null) {
            throw BlockStoreException("A block with the hash " + block.hash + " is already in the store")
        }
        veriBlockRepository.save(block)
    }

    @Throws(BlockStoreException::class, SQLException::class)
    fun put(storedBlocks: List<StoredVeriBlockBlock>) {
        veriBlockRepository.saveAll(storedBlocks)
    }

    @Throws(BlockStoreException::class, SQLException::class)
    override fun get(hash: VBlakeHash): StoredVeriBlockBlock? {
        val blocks = veriBlockRepository.getEndsWithId(hash)
        return if (blocks.isEmpty()) {
            null
        } else {
            blocks[0]
        }
    }

    @Throws(BlockStoreException::class, SQLException::class)
    override fun erase(hash: VBlakeHash): StoredVeriBlockBlock? {
        val headEncoded = keyValueRepository.getValue(chainHeadRepositoryName)
        if (headEncoded != null && VBlakeHash.wrap(Utility.hexToBytes(headEncoded)) == hash) {
            throw BlockStoreException("Cannot erase the chain head block")
        }
        val erased = get(hash)
        if (erased != null && veriBlockRepository.isInUse(hash.trimToPreviousBlockSize())) {
            throw BlockStoreException("Cannot erase a block referenced by another block")
        }
        veriBlockRepository.delete(hash)
        return erased
    }

    @Throws(BlockStoreException::class, SQLException::class)
    override fun replace(
        hash: VBlakeHash,
        block: StoredVeriBlockBlock
    ): StoredVeriBlockBlock? {
        if (hash != block.hash) {
            throw BlockStoreException("The original and replacement block hashes must match")
        }
        val replaced = get(hash)
        veriBlockRepository.save(block)
        return replaced
    }

    @Throws(BlockStoreException::class, SQLException::class)
    override fun get(hash: VBlakeHash, count: Int): List<StoredVeriBlockBlock> {
        val blocks: MutableList<StoredVeriBlockBlock> = ArrayList()
        var currentHash = hash
        while (true) {
            // check if we got the needed blocks
            if (blocks.size >= count) break
            val current = get(currentHash) ?: break

            // check if the block exists
            blocks.add(current)

            // check if we found the Genesis block
            if (currentHash.toBigInteger().compareTo(BigInteger.ZERO) == 0) break
            currentHash = current.block.previousBlock
        }
        return blocks
    }

    // search for a block 'blocksAgo' blocks before the block with 'hash'
    @Throws(BlockStoreException::class, SQLException::class)
    override fun getFromChain(hash: VBlakeHash, blocksAgo: Int): StoredVeriBlockBlock? {
        val blocks = get(hash, blocksAgo + 1)
        // check if the branch is long enough
        return if (blocks.size < blocksAgo + 1) {
            null
        } else {
            blocks[blocksAgo]
        }
    }

    // start from the chainHead and search for a block with hash
    @Throws(BlockStoreException::class, SQLException::class)
    override fun scanBestChain(hash: VBlakeHash): StoredVeriBlockBlock? {
        val head = getChainHead()
            ?: return null

        var current: StoredVeriBlockBlock? = head
        var currentHash = head.hash
        while (true) {
            // trim both hashes to the lowest common length
            val commonMinLength = Math.min(currentHash.length, hash.length)
            val trimmedCurrentHash = VBlakeHash.trim(currentHash, commonMinLength)
            val trimmedHash = VBlakeHash.trim(hash, commonMinLength)
            if (trimmedCurrentHash == trimmedHash) {
                return current
            }

            // check if the block exists
            if (current == null) {
                return null
            }
            // check if we found the Genesis block
            if (currentHash.toBigInteger().compareTo(BigInteger.ZERO) == 0) return null
            currentHash = current.block.previousBlock
            current = get(currentHash)
        }
    }
}
