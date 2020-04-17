package org.veriblock.core.tuweni.progpow;

import com.google.common.primitives.Ints;
import org.veriblock.core.tuweni.ethash.EthHash;
import org.veriblock.core.tuweni.units.bigints.UInt32;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.veriblock.core.types.Pair;

import java.util.HashMap;
import java.util.Map;

public class ProgPoWCache {
    private static final Logger _logger = LoggerFactory.getLogger(ProgPoWCache.class);

    private static final int BUFFER_FOR_CALCULATION = 100;

    // Maps epochs to pairs of DAG caches and cDags
    private static final Map<Integer, Pair<UInt32[], UInt32[]>> cachedPairs = new HashMap<>();

    public static Pair<UInt32[], UInt32[]> getDAGCache(int blockHeight) {
        int epoch = (int)EthHash.epoch(blockHeight);

        if (!(cachedPairs.containsKey(epoch))) {
            _logger.info("Generating DAG cache for epoch " + epoch + " on demand...");
            // Generate both DAG cache and cDag
            UInt32[] cache = EthHash.mkCache(Ints.checkedCast(EthHash.getCacheSize(blockHeight)), blockHeight);
            UInt32[] cDag = ProgPoW.createDagCache(blockHeight, (ind) -> EthHash.calcDatasetItem(cache, ind));

            cachedPairs.put(epoch, new Pair<>(cache, cDag));
        }

        return cachedPairs.get(epoch);
    }

    public static void bufferCache(int currentBlockHeight) {
        int currentEpoch = (int)EthHash.epoch(currentBlockHeight);
        if (!cachedPairs.containsKey(currentEpoch)) {
            _logger.info("Generating DAG cache for current epoch " + currentEpoch + "...");
            UInt32[] cache = EthHash.mkCache(Ints.checkedCast(EthHash.getCacheSize(currentBlockHeight)), currentBlockHeight);
            UInt32[] cDag = ProgPoW.createDagCache(currentBlockHeight, (ind) -> EthHash.calcDatasetItem(cache, ind));

            cachedPairs.put(currentEpoch, new Pair<>(cache, cDag));
        }

        int futureBlockHeight = currentBlockHeight  + BUFFER_FOR_CALCULATION;
        int futureEpoch = (int)EthHash.epoch(futureBlockHeight);
        if (!cachedPairs.containsKey(futureEpoch)) {
            _logger.info("Pre-generating DAG cache for future epoch " + futureEpoch + "...");
            UInt32[] cache = EthHash.mkCache(Ints.checkedCast(EthHash.getCacheSize(futureBlockHeight)), futureBlockHeight);
            UInt32[] cDag = ProgPoW.createDagCache(futureBlockHeight, (ind) -> EthHash.calcDatasetItem(cache, ind));

            cachedPairs.put(futureEpoch, new Pair<>(cache, cDag));
        }
    }
}
