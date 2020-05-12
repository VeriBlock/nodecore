// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2019 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package org.veriblock.sdk.blockchain.store;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.veriblock.sdk.models.BlockStoreException;
import org.veriblock.sdk.models.VBlakeHash;
import org.veriblock.sdk.sqlite.tables.KeyValueData;
import org.veriblock.sdk.sqlite.tables.KeyValueRepository;
import org.veriblock.sdk.sqlite.tables.VeriBlockBlockRepository;
import org.veriblock.sdk.util.Utils;

import java.math.BigInteger;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class VeriBlockStore implements BlockStore<StoredVeriBlockBlock, VBlakeHash> {
    //private static final int DEFAULT_NUM_HEADERS = 90000;
    private static final Logger log = LoggerFactory.getLogger(VeriBlockStore.class);

    // underlying database
    private final Connection databaseConnection;
    private final VeriBlockBlockRepository veriBlockRepository;
    private final KeyValueRepository keyValueRepository;

    private final String chainHeadRepositoryName = "chainHeadVbk";

    public VeriBlockStore(Connection databaseConnection) throws SQLException {
        this.databaseConnection = databaseConnection;
        veriBlockRepository = new VeriBlockBlockRepository(databaseConnection);
        keyValueRepository = new KeyValueRepository(databaseConnection);
    }

    public void shutdown() {
        try {
            if(databaseConnection != null) databaseConnection.close();
        } catch (SQLException e) {
            log.error("Error closing database connection", e);
        }
    }

    public void clear() throws SQLException {
        veriBlockRepository.clear();
        keyValueRepository.clear();
    }

    public StoredVeriBlockBlock getChainHead() throws BlockStoreException, SQLException {
        String headEncoded = keyValueRepository.getValue(chainHeadRepositoryName);
        if(headEncoded == null) return null;

        StoredVeriBlockBlock block = get(VBlakeHash.wrap(Utils.decodeHex(headEncoded)));
        return block;
    }

    public StoredVeriBlockBlock setChainHead(StoredVeriBlockBlock chainHead) throws BlockStoreException, SQLException {
        StoredVeriBlockBlock existingBlock = get(chainHead.getHash());
        if(existingBlock == null) {
            throw new BlockStoreException("Chain head should reference existing block");
        }

        StoredVeriBlockBlock previousBlock = getChainHead();

        String headEncoded = Utils.encodeHex(chainHead.getBlock().getHash().getBytes());
        KeyValueData data = new KeyValueData();
        data.key = chainHeadRepositoryName;
        data.value = headEncoded;
        keyValueRepository.save(data.key, data.value);

        return previousBlock;
    }

    public void put(StoredVeriBlockBlock storedBlock) throws BlockStoreException, SQLException {
        if (veriBlockRepository.get(storedBlock.getHash()) != null) {
            throw new BlockStoreException("A block with the same hash is already in the store");
        }

        veriBlockRepository.save(storedBlock);
    }

    public void put(List<StoredVeriBlockBlock> storedBlocks) throws BlockStoreException, SQLException {
        veriBlockRepository.saveAll(storedBlocks);
    }

    public StoredVeriBlockBlock get(VBlakeHash hash) throws BlockStoreException, SQLException {
        List<StoredVeriBlockBlock> blocks = veriBlockRepository.getEndsWithId(hash);
        return blocks.isEmpty() ? null : blocks.get(0);
    }

    public StoredVeriBlockBlock erase(VBlakeHash hash) throws BlockStoreException, SQLException {
        String headEncoded = keyValueRepository.getValue(chainHeadRepositoryName);

        if (headEncoded != null && VBlakeHash.wrap(Utils.decodeHex(headEncoded)).equals(hash)) {
            throw new BlockStoreException("Cannot erase the chain head block");
        }

        StoredVeriBlockBlock erased = get(hash);

        if(erased != null && veriBlockRepository.isInUse(hash.trimToPreviousBlockSize())) {
            throw new BlockStoreException("Cannot erase a block referenced by another block");
        }

        veriBlockRepository.delete(hash);
        return erased;
     }

    public StoredVeriBlockBlock replace(VBlakeHash hash, StoredVeriBlockBlock storedBlock) throws BlockStoreException, SQLException {
        if (hash != null && storedBlock != null && !hash.equals(storedBlock.getHash())) {
            throw new BlockStoreException("The original and replacement block hashes must match");
        }
        StoredVeriBlockBlock replaced = get(hash);
        veriBlockRepository.save(storedBlock);
        return replaced;
    }

    public List<StoredVeriBlockBlock> get(VBlakeHash hash, int count) throws BlockStoreException, SQLException {
        List<StoredVeriBlockBlock> blocks = new ArrayList<>();
        VBlakeHash currentHash = hash;

        while(true) {
            // check if we got the needed blocks
            if(blocks.size() >= count) break;

            StoredVeriBlockBlock current = get(currentHash);

            // check if the block exists
            if(current == null) break;
            blocks.add(current);

            // check if we found the Genesis block
            if(currentHash.toBigInteger().compareTo(BigInteger.ZERO) == 0) break;
            currentHash = current.getBlock().getPreviousBlock();
        }

        return blocks;
    }

    // search for a block 'blocksAgo' blocks before the block with 'hash'
    public StoredVeriBlockBlock getFromChain(VBlakeHash hash, int blocksAgo) throws BlockStoreException, SQLException {
        List<StoredVeriBlockBlock> blocks = get(hash, blocksAgo + 1);
        // check if the branch is long enough
        if(blocks.size() < (blocksAgo + 1)) {
            return null;
        }
        return blocks.get(blocksAgo);
    }

    // start from the chainHead and search for a block with hash
    public StoredVeriBlockBlock scanBestChain(VBlakeHash hash) throws BlockStoreException, SQLException {
        StoredVeriBlockBlock current = getChainHead();
        if(current == null) return null;

        VBlakeHash currentHash = current.getHash();

        while(true) {
            // trim both hashes to the lowest common length
            int commonMinLength = Math.min(currentHash.length, hash.length);
            VBlakeHash trimmedCurrentHash = VBlakeHash.trim(currentHash, commonMinLength);            
            VBlakeHash trimmedHash = VBlakeHash.trim(hash, commonMinLength);
            
            if(trimmedCurrentHash.equals(trimmedHash)) return current;

            // check if the block exists
            if(current == null) return null;
            // check if we found the Genesis block
            if(currentHash.toBigInteger().compareTo(BigInteger.ZERO) == 0) return null;

            currentHash = current.getBlock().getPreviousBlock();
            current = get(currentHash);
        }
    }
}
