// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2019 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.
package org.veriblock.sdk.blockchain.store

import org.veriblock.core.crypto.Sha256Hash
import org.veriblock.core.utilities.Utility
import org.veriblock.core.utilities.createLogger
import org.veriblock.sdk.models.BlockStoreException
import org.veriblock.sdk.sqlite.tables.BitcoinBlockRepository
import org.veriblock.sdk.sqlite.tables.KeyValueData
import org.veriblock.sdk.sqlite.tables.KeyValueRepository
import java.math.BigInteger
import java.sql.Connection
import java.sql.SQLException
import java.util.ArrayList

private val logger = createLogger {}
private const val chainHeadRepositoryName = "chainHead"

class BitcoinStore(// underlying database
    private val databaseConnection: Connection
) : BlockStore<StoredBitcoinBlock, Sha256Hash> {

    private val bitcoinRepository: BitcoinBlockRepository = BitcoinBlockRepository(databaseConnection)
    private val keyValueRepository: KeyValueRepository = KeyValueRepository(databaseConnection)

    override fun shutdown() {
        try {
            databaseConnection.close()
        } catch (e: SQLException) {
            logger.error("Error closing database connection", e)
        }
    }

    @Throws(SQLException::class)
    override fun clear() {
        bitcoinRepository.clear()
        keyValueRepository.clear()
    }

    @Throws(BlockStoreException::class, SQLException::class)
    override fun getChainHead(): StoredBitcoinBlock? {
        val headEncoded = keyValueRepository.getValue(chainHeadRepositoryName) ?: return null
        return get(Sha256Hash.wrap(Utility.hexToBytes(headEncoded)))
    }

    @Throws(BlockStoreException::class, SQLException::class)
    override fun setChainHead(chainHead: StoredBitcoinBlock): StoredBitcoinBlock? {
        logger.info("Setting BTC chain head to: " + chainHead.hash)
        if (get(chainHead.hash) == null) {
            throw BlockStoreException("Chain head should reference existing block")
        }
        val previousBlock = getChainHead()
        val headEncoded = Utility.bytesToHex(chainHead.hash.bytes)
        val data = KeyValueData()
        data.key = chainHeadRepositoryName
        data.value = headEncoded
        keyValueRepository.save(data.key, data.value)
        return previousBlock
    }

    @Throws(BlockStoreException::class, SQLException::class)
    override fun put(block: StoredBitcoinBlock) {
        if (bitcoinRepository[block.hash] != null) {
            throw BlockStoreException("A block with the same hash is already in the store")
        }
        bitcoinRepository.save(block)
    }

    @Throws(BlockStoreException::class, SQLException::class)
    fun putAll(storedBlocks: List<StoredBitcoinBlock>) {
        bitcoinRepository.saveAll(storedBlocks)
    }

    @Throws(BlockStoreException::class, SQLException::class)
    override fun get(hash: Sha256Hash): StoredBitcoinBlock? {
        return bitcoinRepository[hash]
    }

    @Throws(BlockStoreException::class, SQLException::class)
    override fun erase(hash: Sha256Hash): StoredBitcoinBlock? {
        val headEncoded = keyValueRepository.getValue(chainHeadRepositoryName)
        if (headEncoded != null && Sha256Hash.wrap(
                Utility.hexToBytes(headEncoded)
            ) == hash
        ) {
            throw BlockStoreException("Cannot erase the chain head block")
        }
        val erased = get(hash)
        if (erased != null && bitcoinRepository.isInUse(hash)) {
            throw BlockStoreException("Cannot erase a block referenced by another block")
        }
        bitcoinRepository.delete(hash)
        return erased
    }

    @Throws(BlockStoreException::class, SQLException::class)
    override fun replace(hash: Sha256Hash, block: StoredBitcoinBlock): StoredBitcoinBlock? {
        if (hash != block.hash) {
            throw BlockStoreException("The original and replacement block hashes must match")
        }
        logger.info("Replacing " + hash + " with " + block.hash)
        val replaced = get(hash)
        bitcoinRepository.save(block)
        return replaced
    }

    @Throws(BlockStoreException::class, SQLException::class)
    override operator fun get(hash: Sha256Hash, count: Int): List<StoredBitcoinBlock> {
        val blocks: MutableList<StoredBitcoinBlock> = ArrayList()
        var currentHash = hash
        while (true) {
            // check if we got the needed blocks
            if (blocks.size >= count) {
                break
            }
            val current = get(currentHash)
                ?: break
            // check if the block exists
            blocks.add(current)

            // check if we found the Genesis block
            if (currentHash.toBigInteger().compareTo(BigInteger.ZERO) == 0){
                break
            }
            currentHash = current.block.previousBlock
        }
        return blocks
    }

    // search for a block 'blocksAgo' blocks before the block with 'hash'
    @Throws(BlockStoreException::class, SQLException::class)
    override fun getFromChain(hash: Sha256Hash, blocksAgo: Int): StoredBitcoinBlock? {
        val blocks = get(hash, blocksAgo + 1)
        // check if the branch is long enough
        return if (blocks.size < blocksAgo + 1) {
            null
        } else blocks[blocksAgo]
    }

    // start from the chainHead and search for a block with hash
    @Throws(BlockStoreException::class, SQLException::class)
    override fun scanBestChain(hash: Sha256Hash): StoredBitcoinBlock? {
        val head = getChainHead()
            ?: return null

        var current: StoredBitcoinBlock? = head
        var currentHash = head.hash
        while (true) {
            if (currentHash.compareTo(hash) == 0) return current
            // check if the block exists
            if (current == null) return null
            // check if we found the Genesis block
            if (current.height == 0) return null
            currentHash = current.block.previousBlock
            current = get(currentHash)
        }
    }
}
