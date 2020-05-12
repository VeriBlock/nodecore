// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2019 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package org.veriblock.sdk.blockchain.store;

import org.veriblock.sdk.models.BlockStoreException;

import java.sql.SQLException;
import java.util.List;

public interface BlockStore<Block, Id> {
    /**
     * Shut down the store and release resources
     */
    void shutdown();

    /**
     * Delete all blocks from the store
     * @throws SQLException
     */
    void clear() throws SQLException;
    
    /**
     * Get the chain head(the tip of the current best chain)
     * @return the current chain head block or null if not set
     * @throws BlockStoreException
     * @throws SQLException
     */
    Block getChainHead() throws BlockStoreException, SQLException;

    /**
     * Set the chain head(the tip of the current best chain)
     * @param chainHead the new chain head block
     * @return the previous chain head block or null if not set
     * @throws BlockStoreException if the chain head block is not in the store
     * @throws SQLException
     */
    Block setChainHead(Block chainHead) throws BlockStoreException, SQLException;

    /**
     * Put the given block into the store
     * @param block the block to put into the store
     * @throws BlockStoreException if the block with the same
     *                             or similar id is already in the store
     * @throws SQLException
     */
    void put(Block block) throws BlockStoreException, SQLException;

    /**
     * Retrieve the block with the given block id
     * @param id the block id(hash)
     * @return the block with the given id or null
     * @throws BlockStoreException
     * @throws SQLException
     */
    Block get(Id id) throws BlockStoreException, SQLException;

    /**
     * Delete the block with the given block id
     * @param id the block id(hash)
     * @return the deleted block or null if the store has no matching blocks
     * @throws BlockStoreException if the block is referenced by
     *                             any data structures(blocks, chain head)
     * @throws SQLException
     */
    Block erase(Id id) throws BlockStoreException, SQLException;

    /**
     * Replace the block with the given block id with the updated version
     * @param id the block id(hash)
     * @param block the updated block
     * @return the old version of the block or
     *         null if the store has no matching blocks
     * @throws BlockStoreException if the given block does not match the given id
     * @throws BlockStoreException if the given block is null
     *                             -- use erase() to delete a block
     * @throws SQLException
     */
    Block replace(Id id, Block block) throws BlockStoreException, SQLException;

    /**
     * Retrieve a chain of up to the given number of blocks
     * that ends at the block that matches the given id
     * @param id the starting block id(hash)
     * @param count the maximum number of blocks to retrieve
     * @return the list of blocks in the chain,
     *         starting with the block that matches the given id
     * @throws BlockStoreException
     * @throws SQLException
     */
    List<Block> get(Id id, int count) throws BlockStoreException, SQLException;

    /**
     * Retrieve the block that is the given number of blocks
     * before the block with the given id
     * @param id the block id(hash)
     * @param blocksAgo the number of blocks to skip,
     *                  zero value turns the call into a regular get(id)
     * @return the block with the given id or
     *         null if there is no matching block
     * @throws BlockStoreException
     * @throws SQLException
     */
    Block getFromChain(Id id, int blocksAgo) throws BlockStoreException, SQLException;

    /**
     * Retrieve the block with the given id searching
     * only the chain that starts from the chain head
     * @param id the block id(hash)
     * @return the block with the given id or null if
     *         there is no matching block in the chain
     *         pointed to by the chain head
     * @throws BlockStoreException
     * @throws SQLException
     */
    Block scanBestChain(Id id) throws BlockStoreException, SQLException;
}
