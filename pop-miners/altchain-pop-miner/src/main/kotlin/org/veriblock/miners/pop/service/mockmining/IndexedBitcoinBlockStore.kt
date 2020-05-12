// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2019 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package org.veriblock.miners.pop.service.mockmining;

import org.veriblock.sdk.blockchain.store.BlockStore;
import org.veriblock.sdk.blockchain.store.StoredBitcoinBlock;
import org.veriblock.sdk.models.BlockStoreException;
import org.veriblock.sdk.models.Sha256Hash;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// A stopgap store wrapper that implements get(height) on top of BlockStore interface
public class IndexedBitcoinBlockStore implements BlockStore<StoredBitcoinBlock, Sha256Hash> {

    private final BlockStore<StoredBitcoinBlock, Sha256Hash> store;

    private final Map<Integer, StoredBitcoinBlock> blockByHeightIndex = new HashMap<>();

    public IndexedBitcoinBlockStore(BlockStore<StoredBitcoinBlock, Sha256Hash> store) throws SQLException {
        this.store = store;
        
        //FIXME: read and index the store
    }

    public void shutdown() {
        store.shutdown();
    }

    public void clear() throws SQLException {
        store.clear();
        blockByHeightIndex.clear();
    }

    public StoredBitcoinBlock getChainHead() throws BlockStoreException, SQLException {
        return store.getChainHead();
    }

    public StoredBitcoinBlock setChainHead(StoredBitcoinBlock chainHead) throws BlockStoreException, SQLException {
        return store.setChainHead(chainHead);
    }

    public void put(StoredBitcoinBlock block) throws BlockStoreException, SQLException {
        store.put(block);
        blockByHeightIndex.put(block.getHeight(), block);
    }

    public StoredBitcoinBlock get(Sha256Hash hash) throws BlockStoreException, SQLException {
        return store.get(hash);
    }

    public StoredBitcoinBlock get(int height) throws BlockStoreException, SQLException {
        return blockByHeightIndex.get(height);
    }

    public StoredBitcoinBlock erase(Sha256Hash hash) throws BlockStoreException, SQLException {
        StoredBitcoinBlock erased = store.erase(hash);
        if (erased != null) {
            blockByHeightIndex.remove(erased.getHeight());
        }
        return erased;
     }

    public StoredBitcoinBlock replace(Sha256Hash hash, StoredBitcoinBlock block) throws BlockStoreException, SQLException {
        return store.replace(hash, block);
    }

    public List<StoredBitcoinBlock> get(Sha256Hash hash, int count) throws BlockStoreException, SQLException {
        return store.get(hash, count);
    }

    public StoredBitcoinBlock getFromChain(Sha256Hash hash, int blocksAgo) throws BlockStoreException, SQLException {
        return store.getFromChain(hash, blocksAgo);
    }

    public StoredBitcoinBlock scanBestChain(Sha256Hash hash) throws BlockStoreException, SQLException {
        return store.scanBestChain(hash);
    }
}
