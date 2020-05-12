// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2019 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package org.veriblock.sdk.blockchain;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.veriblock.sdk.auditor.Change;
import org.veriblock.sdk.blockchain.changes.AddBitcoinBlockChange;
import org.veriblock.sdk.blockchain.changes.SetBitcoinHeadChange;
import org.veriblock.sdk.blockchain.store.BlockStore;
import org.veriblock.sdk.blockchain.store.StoredBitcoinBlock;
import org.veriblock.sdk.conf.BitcoinNetworkParameters;
import org.veriblock.sdk.models.BitcoinBlock;
import org.veriblock.sdk.models.BlockStoreException;
import org.veriblock.sdk.models.Constants;
import org.veriblock.sdk.models.Sha256Hash;
import org.veriblock.sdk.models.VerificationException;
import org.veriblock.sdk.services.ValidationService;
import org.veriblock.sdk.util.BitcoinUtils;
import org.veriblock.sdk.util.Preconditions;

import java.math.BigInteger;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.OptionalLong;

public class BitcoinBlockchain {
    private static final Logger log = LoggerFactory.getLogger(BitcoinBlockchain.class);

    private static final int MINIMUM_TIMESTAMP_BLOCK_COUNT = 11;

    private final BlockStore<StoredBitcoinBlock, Sha256Hash> store;
    private final BitcoinNetworkParameters networkParameters;
    private final Map<Sha256Hash, StoredBitcoinBlock> temporalStore;
    private StoredBitcoinBlock temporaryChainHead = null;
    
    private boolean skipValidateBlocksDifficulty = false; 

    private boolean hasTemporaryModifications() {
        return temporaryChainHead != null || temporalStore.size() > 0;
    }

    public BitcoinBlockchain(BitcoinNetworkParameters networkParameters, BlockStore<StoredBitcoinBlock, Sha256Hash> store) {
        Preconditions.notNull(store, "Store cannot be null");

        this.store = store;
        this.temporalStore = new HashMap<>();
        this.networkParameters = networkParameters;
    }

    public BlockStore<StoredBitcoinBlock, Sha256Hash> getStore() {
        return store;
    }

    public boolean isValidateBlocksDifficulty() {
        return !skipValidateBlocksDifficulty;
    }
    
    public void setSkipValidateBlocksDifficulty(boolean skip) {
        skipValidateBlocksDifficulty = skip;
    }

    public BitcoinBlock get(Sha256Hash hash) throws BlockStoreException, SQLException {
        StoredBitcoinBlock storedBlock = getInternal(hash);
        if (storedBlock != null) {
            return storedBlock.getBlock();
        }

        return null;
    }

    public BitcoinBlock searchBestChain(Sha256Hash hash) throws BlockStoreException, SQLException {
        // Look at the temporal store first
        StoredBitcoinBlock storedBlock;
        if (temporaryChainHead != null) {
            storedBlock = temporalStore.get(hash);
        } else {
            storedBlock = store.scanBestChain(hash);
        }

        if (storedBlock != null) {
            return storedBlock.getBlock();
        }

        return null;
    }

    public List<Change> add(BitcoinBlock block) throws VerificationException, BlockStoreException, SQLException {
        Preconditions.state(!hasTemporaryModifications(), "Cannot add a block while having temporary modifications");

        // Lightweight verification of the header
        ValidationService.verify(block);

        BigInteger work = BigInteger.ZERO;
        // TODO: Need to be able to set this accurately on the first block
        int currentHeight = 0;
        if (getChainHeadInternal() != null) {
            // Further verification requiring context
            StoredBitcoinBlock previous = checkConnectivity(block);
            if (!verifyBlock(block, previous)) {
                return Collections.emptyList();
            }
            work = work.add(previous.getWork());
            currentHeight = previous.getHeight() + 1;
        }

        StoredBitcoinBlock storedBlock = new StoredBitcoinBlock(
                block,
                work.add(BitcoinUtils.decodeCompactBits(block.getBits())),
                currentHeight);

        List<Change> changes = new ArrayList<>();
        store.put(storedBlock);
        changes.add(new AddBitcoinBlockChange(null, storedBlock));

        StoredBitcoinBlock chainHead = store.getChainHead();
        if (chainHead == null || storedBlock.getWork().compareTo(chainHead.getWork()) > 0) {
            StoredBitcoinBlock priorHead = store.setChainHead(storedBlock);
            ///HACK: this is a dummy block that represents a change from null to genesis block
            if(priorHead == null) {
                BitcoinBlock emptyBlock = new BitcoinBlock(0, Sha256Hash.ZERO_HASH, Sha256Hash.ZERO_HASH, 0, 1, 0);
                priorHead = new StoredBitcoinBlock(emptyBlock, BigInteger.ONE, 0);
            }
            changes.add(new SetBitcoinHeadChange(priorHead, storedBlock));
        }

        return changes;
    }

    public List<Change> addAll(List<BitcoinBlock> blocks) throws VerificationException, BlockStoreException, SQLException {
        Preconditions.state(!hasTemporaryModifications(), "Cannot add blocks whle having temporary modifications");

        List<Change> changes = new ArrayList<>();
        for (BitcoinBlock block : blocks) {
            changes.addAll(add(block));
        }

        return changes;
    }

    public void addTemporarily(BitcoinBlock block) throws VerificationException, BlockStoreException, SQLException {
        // Lightweight verification of the header
        ValidationService.verify(block);

        // Further verification requiring context
        StoredBitcoinBlock previous = checkConnectivity(block);
        if (!verifyBlock(block, previous)) {
            return;
        }

        StoredBitcoinBlock storedBlock = new StoredBitcoinBlock(
                block,
                previous.getWork().add(BitcoinUtils.decodeCompactBits(block.getBits())),
                previous.getHeight() + 1);

        temporalStore.put(block.getHash(), storedBlock);

        StoredBitcoinBlock chainHead = getChainHeadInternal();
        if (storedBlock.getWork().compareTo(chainHead.getWork()) > 0) {
            temporaryChainHead = storedBlock;
        }
    }

    public void addAllTemporarily(List<BitcoinBlock> blocks) {
        blocks.forEach(t -> {
            try {
                addTemporarily(t);
            } catch (VerificationException | BlockStoreException | SQLException e) {
                throw new BlockStoreException(e);
            }
        });
    }

    public void clearTemporaryModifications() {
        temporaryChainHead = null;
        temporalStore.clear();
    }

    public void rewind(List<Change> changes) throws BlockStoreException, SQLException {
        for (Change change : changes) {
            if (change.getChainIdentifier().equals(Constants.BITCOIN_HEADER_MAGIC)) {
                switch (change.getOperation()) {
                    case ADD_BLOCK:
                        StoredBitcoinBlock newValue = StoredBitcoinBlock.deserialize(change.getNewValue());
                        if (change.getOldValue() != null && change.getOldValue().length > 0) {
                            StoredBitcoinBlock oldValue = StoredBitcoinBlock.deserialize(change.getOldValue());
                            store.replace(newValue.getHash(), oldValue);
                        } else {
                            store.erase(newValue.getHash());
                        }
                        break;
                    case SET_HEAD:
                        StoredBitcoinBlock priorHead = StoredBitcoinBlock.deserialize(change.getOldValue());
                        store.setChainHead(priorHead);
                        break;
                default:
                    break;
                }
            }
        }
    }

    public BitcoinBlock getChainHead() throws SQLException {
        StoredBitcoinBlock chainHead = store.getChainHead();

        return chainHead == null ? null : chainHead.getBlock();
    }

    // in case there's a need to know the chain head block height
    public StoredBitcoinBlock getStoredChainHead() throws SQLException {
        return store.getChainHead();
    }

    private StoredBitcoinBlock getInternal(Sha256Hash hash) throws BlockStoreException, SQLException {
        if (temporalStore.containsKey(hash)) {
            return temporalStore.get(hash);
        }

        return store.get(hash);
    }

    private StoredBitcoinBlock getChainHeadInternal() throws BlockStoreException, SQLException {
        if (temporaryChainHead != null) return temporaryChainHead;

        return store.getChainHead();
    }

    private List<StoredBitcoinBlock> getTemporaryBlocks(Sha256Hash hash, int count) {
        List<StoredBitcoinBlock> blocks = new ArrayList<>();

        Sha256Hash cursor = Sha256Hash.wrap(hash.getBytes());
        while (temporalStore.containsKey(cursor)) {
            StoredBitcoinBlock tempBlock = temporalStore.get(cursor);
            blocks.add(tempBlock);

            if (blocks.size() >= count) break;

            cursor = tempBlock.getBlock().getPreviousBlock();
        }

        return blocks;
    }


    private boolean verifyBlock(BitcoinBlock block, StoredBitcoinBlock previous) throws VerificationException, BlockStoreException, SQLException {
        if (!checkDuplicate(block)) return false;

        checkTimestamp(block);
        checkDifficulty(block, previous);

        return true;
    }

    private boolean checkDuplicate(BitcoinBlock block) throws BlockStoreException, SQLException {
        // Duplicate?
        StoredBitcoinBlock duplicate = getInternal(block.getHash());
        if (duplicate != null) {
            log.trace("Block '{}' has already been added", block.getHash().toString());
            return false;
        }

        return true;
    }

    private StoredBitcoinBlock checkConnectivity(BitcoinBlock block) throws BlockStoreException, SQLException {
        // Connects to a known "seen" block (except for origin block)
        StoredBitcoinBlock previous = getInternal(block.getPreviousBlock());
        if (previous == null) {
            // corner case: the first bootstrap block connects to the blockchain
            // by definition despite not having the previous block in the store
            if (getInternal(block.getHash()) == null) {
                throw new VerificationException("Block does not fit");
            }
        }

        return previous;
    }

    // return the earliest valid timestamp for a block that follows the blockHash block
    public OptionalInt getNextEarliestTimestamp(Sha256Hash blockHash) throws BlockStoreException, SQLException {
        // Checks the temporary blocks first
        List<StoredBitcoinBlock> context = getTemporaryBlocks(blockHash, MINIMUM_TIMESTAMP_BLOCK_COUNT);
        if (context.size() > 0) {
            StoredBitcoinBlock last = context.get(context.size() - 1);
            context.addAll(store.get(last.getBlock().getPreviousBlock(), MINIMUM_TIMESTAMP_BLOCK_COUNT - context.size()));
        } else {
            context.addAll(store.get(blockHash, MINIMUM_TIMESTAMP_BLOCK_COUNT));
        }

        if (context.size() < MINIMUM_TIMESTAMP_BLOCK_COUNT) {
            return OptionalInt.empty();
        }

        Optional<Integer> median = context.stream().sorted(Comparator.comparingInt(StoredBitcoinBlock::getHeight).reversed())
                .limit(MINIMUM_TIMESTAMP_BLOCK_COUNT)
                .map(b -> b.getBlock().getTimestamp())
                .sorted()
                .skip(MINIMUM_TIMESTAMP_BLOCK_COUNT / 2)
                .findFirst();

        return median.isPresent() ? OptionalInt.of(median.get() + 1) : OptionalInt.empty();
    }

    private void checkTimestamp(BitcoinBlock block) throws VerificationException, BlockStoreException, SQLException {
        OptionalInt timestamp = getNextEarliestTimestamp(block.getPreviousBlock());

        if (timestamp.isPresent()) {
            if (block.getTimestamp() < timestamp.getAsInt()) {
                throw new VerificationException("Block is too far in the past");
            }
        } else {
            log.debug("Not enough context blocks to check the timestamp of block '{}'", block.getHash().toString());
        }
    }

    public OptionalLong getNextDifficulty(int blockTimestamp, StoredBitcoinBlock previous) throws BlockStoreException, SQLException {
        int difficultyAdjustmentInterval = networkParameters.getPowTargetTimespan()
                                         / networkParameters.getPowTargetSpacing();

        // Special rule for the regtest: all blocks are minimum difficulty
        if (networkParameters.getPowNoRetargeting()) return OptionalLong.of(previous.getBlock().getBits());

        // Previous + 1 = height of block
        if ((previous.getHeight() + 1) % difficultyAdjustmentInterval > 0) {

            // Unless minimum difficulty blocks are allowed(special difficulty rule for the testnet),
            // the difficulty should be same as previous
            if (!networkParameters.getAllowMinDifficultyBlocks()) {
                return OptionalLong.of(previous.getBlock().getBits());

            } else {

                long proofOfWorkLimit = BitcoinUtils.encodeCompactBits(networkParameters.getPowLimit());

                // If the block's timestamp is more than 2*PowTargetSpacing minutes
                // then allow mining of a minimum difficulty block
                if (blockTimestamp > previous.getBlock().getTimestamp() + networkParameters.getPowTargetSpacing()*2) {
                    return OptionalLong.of(proofOfWorkLimit);

                } else {

                    // Find the last non-minimum difficulty block
                    while (previous != null && previous.getBlock().getPreviousBlock() != null
                        && previous.getHeight() % difficultyAdjustmentInterval != 0
                        && previous.getBlock().getBits() == proofOfWorkLimit) {

                        previous = getInternal(previous.getBlock().getPreviousBlock());
                    }

                    // Corner case: we're less than difficultyAdjustmentInterval
                    // from the bootstrap and all blocks are minimum difficulty
                    if (previous == null) return OptionalLong.empty();

                    // Difficulty matches the closest non-minimum difficulty block
                    return OptionalLong.of(previous.getBlock().getBits());
                }
            }
        } else {

            // Difficulty needs to adjust

            List<StoredBitcoinBlock> tempBlocks = getTemporaryBlocks(previous.getHash(), difficultyAdjustmentInterval);

            StoredBitcoinBlock cycleStart;
            if (tempBlocks.size() == difficultyAdjustmentInterval) {
                cycleStart = tempBlocks.get(tempBlocks.size() - 1);
            } else if (tempBlocks.size() > 0) {
                StoredBitcoinBlock last = tempBlocks.get(tempBlocks.size() - 1);
                cycleStart = store.getFromChain(last.getBlock().getPreviousBlock(), difficultyAdjustmentInterval - tempBlocks.size());
            } else {
                cycleStart = store.getFromChain(previous.getHash(), difficultyAdjustmentInterval - 1);
            }

            if (cycleStart == null) {
                // Because there will just be some Bitcoin block from whence accounting begins, it's likely
                // that the first adjustment period will not have sufficient blocks to compute correctly
                return OptionalLong.empty();
            }

            long newTarget = calculateNewTarget(
                    previous.getBlock().getBits(),
                    cycleStart.getBlock().getTimestamp(),
                    previous.getBlock().getTimestamp());

            return OptionalLong.of(newTarget);
        }

    }

    private void checkDifficulty(BitcoinBlock block, StoredBitcoinBlock previous) throws VerificationException, BlockStoreException, SQLException {
        if(!isValidateBlocksDifficulty()){
            return;
        }

        OptionalLong difficulty = getNextDifficulty(block.getTimestamp(), previous);

        if (difficulty.isPresent()) {
            if (block.getBits() != difficulty.getAsLong()) {
                throw new VerificationException("Block does not match computed difficulty adjustment");
            }
        } else {
            log.debug("Not enough context blocks to check the difficulty of block '{}'", block.getHash().toString());
        }


    }

    private long calculateNewTarget(long current, int startTimestamp, int endTimestamp) {
        int elapsed = endTimestamp - startTimestamp;

        elapsed = Math.max(elapsed, networkParameters.getPowTargetTimespan() / 4);
        elapsed = Math.min(elapsed, networkParameters.getPowTargetTimespan() * 4);

        BigInteger newTarget = BitcoinUtils.decodeCompactBits(current)
                                           .multiply(BigInteger.valueOf(elapsed))
                                           .divide(BigInteger.valueOf(networkParameters.getPowTargetTimespan()));

        // Should never occur; hitting the max target would mean Bitcoin has the hashrate of a few CPUs
        newTarget = newTarget.min(networkParameters.getPowLimit());

        // Reduce the precision of the calculated difficulty to match that of the compact bits representation
        int byteLength = (newTarget.bitLength() + 8 - 1) / 8;
        BigInteger mask = BigInteger.valueOf(0xFFFFFFL).shiftLeft((byteLength - 3) * 8);
        newTarget = newTarget.and(mask);

        return BitcoinUtils.encodeCompactBits(newTarget);
    }

    // Returns true if the store was empty and the bootstrap
    // blocks were added to it successfully.
    // Otherwise, returns false.
    public boolean bootstrap(List<BitcoinBlock> blocks, int firstBlockHeight) throws SQLException, VerificationException {
        assert(!blocks.isEmpty());
        assert(firstBlockHeight >= 0);

        boolean bootstrapped = store.getChainHead() != null;

        if (!bootstrapped) {
            log.info("Bootstrapping starting at height {} with {} blocks: {} to {}",
                     String.valueOf(firstBlockHeight),
                     String.valueOf(blocks.size()),
                     blocks.get(0).getHash().toString(),
                     blocks.get(blocks.size() - 1).getHash().toString());

            Sha256Hash prevHash = null;
            for (BitcoinBlock block : blocks) {
                if (prevHash != null && !block.getPreviousBlock().equals(prevHash) )
                    throw new VerificationException("Bitcoin bootstrap blocks must be contiguous");

                prevHash = block.getHash();
            }

            int blockHeight = firstBlockHeight;
            for (BitcoinBlock block : blocks) {
                BigInteger work = BitcoinUtils.decodeCompactBits(block.getBits());
                StoredBitcoinBlock storedBlock = new StoredBitcoinBlock(block, work, blockHeight);
                blockHeight++;
                store.put(storedBlock);
                store.setChainHead(storedBlock);
            }
        }

        return !bootstrapped;
    }

    public boolean bootstrap(BitcoinBlockchainBootstrapConfig config) throws SQLException, VerificationException {
        return bootstrap(config.blocks, config.firstBlockHeight);
    }

}
