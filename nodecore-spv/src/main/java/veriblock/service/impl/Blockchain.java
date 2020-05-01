// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.
package veriblock.service.impl;

import com.google.common.collect.EvictingQueue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.veriblock.core.bitcoinj.BitcoinUtilities;
import org.veriblock.sdk.blockchain.store.StoredVeriBlockBlock;
import org.veriblock.sdk.blockchain.store.VeriBlockStore;
import org.veriblock.sdk.models.VBlakeHash;
import org.veriblock.sdk.models.VeriBlockBlock;
import spark.utils.CollectionUtils;

import java.math.BigInteger;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class Blockchain {
    private static final Logger logger = LoggerFactory.getLogger(Blockchain.class);

    private final VeriBlockBlock genesisBlock;
    private final VeriBlockStore blockStore;
    //TODO SPV-124
    private final EvictingQueue<StoredVeriBlockBlock> blocksCache = EvictingQueue.create(1000);

    public Blockchain(VeriBlockBlock genesisBlock, VeriBlockStore blockStore) {
        this.genesisBlock = genesisBlock;
        this.blockStore = blockStore;
    }

    public VeriBlockStore getBlockStore() {
        return blockStore;
    }

    public VeriBlockBlock getChainHead() {
        if (blockStore != null) {
            try {
                return blockStore.getChainHead().getBlock();
            } catch (SQLException e) {
                logger.error("Unable to get chain head", e);
            }
        }

        return null;
    }

    public StoredVeriBlockBlock get(VBlakeHash hash) throws SQLException {
        return blockStore.get(hash);
    }

    public void add(VeriBlockBlock block) throws SQLException {
        StoredVeriBlockBlock previous = blockStore.get(block.getPreviousBlock());
        if (previous == null) {
            // Nothing to build on
            return;
        }
        StoredVeriBlockBlock storedBlock =
            new StoredVeriBlockBlock(block, previous.getWork().add(BitcoinUtilities.decodeCompactBits(block.getDifficulty())));

        // TODO: Make the put(...) and setChainHead(...) atomic

        blockStore.put(storedBlock);
        blocksCache.add(storedBlock);

        // TODO: PoP fork resolution additional
        if (storedBlock.getWork().compareTo(blockStore.getChainHead().getWork()) > 0) {
            blockStore.setChainHead(storedBlock);
        }

        // TODO: Broadcast events: new best block, reorganize, new block
    }

    public void addAll(List<VeriBlockBlock> blocks) throws SQLException {
        List<VeriBlockBlock> sortedBlock = blocks.stream()
                .sorted(Comparator.comparingInt(VeriBlockBlock::getHeight))
                .collect(Collectors.toList());

        logger.info("Add blocks {} blocks, height {} - {}", sortedBlock.size(), sortedBlock.get(0).getHeight(), sortedBlock.get(sortedBlock.size() - 1).getHeight());

        if(!areBlocksSequentially(sortedBlock)){
            // todo throw Exception
            return;
        }

        List<VeriBlockBlock> listToStore = listToStore(sortedBlock);

        if (CollectionUtils.isEmpty(listToStore)) {
            // todo throw Exception
            // Nothing to build on
            return;
        }

        List<StoredVeriBlockBlock> storedBlocks = convertToStoreVeriBlocks(listToStore);

        blockStore.put(storedBlocks);
        blocksCache.addAll(storedBlocks);

        // TODO: PoP fork resolution additional
        if (storedBlocks.get(storedBlocks.size() - 1).getWork().compareTo(blockStore.getChainHead().getWork()) > 0) {
            blockStore.setChainHead(storedBlocks.get(storedBlocks.size() - 1));
        }
    }

    public StoredVeriBlockBlock getBlockByHeight(Integer height) {
        return blocksCache.stream()
            .filter(block -> block.getHeight() == height)
            .findAny()
            .orElse(null);
    }

    private List<VeriBlockBlock> listToStore(List<VeriBlockBlock> veriBlockBlocks) throws SQLException {
        for (int i = 0; i < veriBlockBlocks.size(); i++) {
            StoredVeriBlockBlock previous = blockStore.get(veriBlockBlocks.get(i).getPreviousBlock());
            if (previous != null) {
                return veriBlockBlocks.subList(i, veriBlockBlocks.size());
            }
        }
        return Collections.EMPTY_LIST;
    }

    private List<StoredVeriBlockBlock> convertToStoreVeriBlocks(List<VeriBlockBlock> veriBlockBlocks) throws SQLException {
        Map<String, BigInteger> blockWorks = new HashMap<>();
        List<StoredVeriBlockBlock> storedBlocks = new ArrayList<>();

        StoredVeriBlockBlock commonBlock = blockStore.get(veriBlockBlocks.get(0).getPreviousBlock());
        blockWorks.put(commonBlock.getHash().toString().substring(24), commonBlock.getWork());

        for (VeriBlockBlock veriBlockBlock : veriBlockBlocks) {
            BigInteger work = blockWorks.get(veriBlockBlock.getPreviousBlock().toString());
            //This block is from fork, our Blockchain doesn't have this previousBlock.
            if(work == null){
                StoredVeriBlockBlock storedVeriBlockBlock = blockStore.get(veriBlockBlock.getPreviousBlock());
                if(storedVeriBlockBlock == null) {
                    //There is no such block.
                    continue;
                }
                work = storedVeriBlockBlock.getWork();
            }
            BigInteger workOfCurrentBlock = work.add(BitcoinUtilities.decodeCompactBits(veriBlockBlock.getDifficulty()));
            blockWorks.put(veriBlockBlock.getHash().toString().substring(24), workOfCurrentBlock);

            StoredVeriBlockBlock block = new StoredVeriBlockBlock(veriBlockBlock, workOfCurrentBlock);
            storedBlocks.add(block);
        }

        return storedBlocks;
    }

    public List<VeriBlockBlock> getPeerQuery() {
        List<VeriBlockBlock> blocks = new ArrayList<>(16);

        try {
            StoredVeriBlockBlock cursor = blockStore.getChainHead();
            if (cursor != null) {
                blocks.add(cursor.getBlock());
                outer:
//                for (int i = 0; i < 16; i++) {
//                TODO 16 is too much for bigDb with current approach. It take a lot of time. Try to get amount of blocks by height and process in memory.

                for (int i = 0; i < 5; i++) {
                    final int seek = cursor.getBlock().getHeight() - (int) Math.pow(2, i);
                    if (seek < 0) {
                        break;
                    }

                    while (seek != cursor.getBlock().getHeight()) {
                        cursor = blockStore.get(cursor.getBlock().getPreviousBlock());
                        if (cursor == null) {
                            break outer;
                        }
                    }

                    blocks.add(cursor.getBlock());
                }
            }
        } catch (Exception e) {
            logger.error("Unable to build peer query", e);
        }

        blocks.add(genesisBlock);

        return blocks;
    }

    private void remove(VeriBlockBlock block) {
        try {
            // TODO: Update affected transactions
        } catch (Exception e) {
            // TODO: Handle
        }
    }

    private boolean areBlocksSequentially(List<VeriBlockBlock> blocks) {
        Set<String> hashes = new HashSet<>();

        if (CollectionUtils.isEmpty(blocks)) {
            return false;
        }

        int firstHeight = blocks.get(0).getHeight();
        for (VeriBlockBlock veriBlockBlock : blocks) {
            if (firstHeight < veriBlockBlock.getHeight()) {
                if (!hashes.contains(veriBlockBlock.getPreviousBlock().toString())) {
                    return false;
                }
            }

            hashes.add(veriBlockBlock.getHash().toString().substring(24));
        }

        return true;
    }


}
