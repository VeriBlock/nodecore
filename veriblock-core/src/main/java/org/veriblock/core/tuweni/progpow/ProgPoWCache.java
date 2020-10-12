package org.veriblock.core.tuweni.progpow;

import com.google.common.primitives.Ints;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.veriblock.core.tuweni.ethash.EthHash;
import org.veriblock.core.types.Pair;
import org.veriblock.core.types.Triple;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class ProgPoWCache {
    private static final Logger _logger = LoggerFactory.getLogger(ProgPoWCache.class);

    private static final int BUFFER_FOR_CALCULATION = 100;

    // Maps epochs to pairs of DAG caches and cDags
    private static final Map<Integer, Triple<int[], int[], Long>> cachedPairs = new HashMap<>();

    private static Integer MAX_CACHED_PAIRS = 10 + new Random().nextInt(4);

    public static void setMaxCachedPairs(int limit) {
        MAX_CACHED_PAIRS = limit;
    }

    public static Pair<int[], int[]> getDAGCache(int blockHeight) {
        int epoch = (int)EthHash.epoch(blockHeight);

        if (!(cachedPairs.containsKey(epoch))) {
            _logger.info("Generating DAG cache for epoch " + epoch + " on demand...");
            // Generate both DAG cache and cDag
            int[] cache = EthHash.mkCache(Ints.checkedCast(EthHash.getCacheSize(blockHeight)), blockHeight);

            int[] cDag = ProgPoW.createDagCache(blockHeight, (ind) -> EthHash.calcDatasetItem(cache, ind));

            cachedPairs.put(epoch, new Triple<>(cache, cDag, System.currentTimeMillis()));
        }

        Triple<int[], int[], Long> fetched = cachedPairs.get(epoch);
        fetched.setThird(System.currentTimeMillis());

        pruneCache();

        return new Pair<>(fetched.getFirst(), fetched.getSecond());
    }

    public static void bufferCache(int currentBlockHeight) {
        int currentEpoch = (int)EthHash.epoch(currentBlockHeight);
        if (!cachedPairs.containsKey(currentEpoch)) {
            _logger.info("Generating DAG cache for current epoch " + currentEpoch + "...");
            int[] cache = EthHash.mkCache(Ints.checkedCast(EthHash.getCacheSize(currentBlockHeight)), currentBlockHeight);

            int[] cDag = ProgPoW.createDagCache(currentBlockHeight, (ind) -> EthHash.calcDatasetItem(cache, ind));

            cachedPairs.put(currentEpoch, new Triple<>(cache, cDag, System.currentTimeMillis()));
        }

        int futureBlockHeight = currentBlockHeight  + BUFFER_FOR_CALCULATION;
        int futureEpoch = (int)EthHash.epoch(futureBlockHeight);
        if (!cachedPairs.containsKey(futureEpoch)) {
            _logger.info("Pre-generating DAG cache for future epoch " + futureEpoch + "...");
            int[] cache = EthHash.mkCache(Ints.checkedCast(EthHash.getCacheSize(futureBlockHeight)), futureBlockHeight);

            int[] cDag = ProgPoW.createDagCache(futureBlockHeight, (ind) -> EthHash.calcDatasetItem(cache, ind));

            cachedPairs.put(futureEpoch, new Triple<>(cache, cDag, System.currentTimeMillis()));
        }

        pruneCache();
    }

    private static void pruneCache() {
        while (cachedPairs.size() > MAX_CACHED_PAIRS) {
            // Loop through all pairs, find the one that was used the longest ago to remove
            long earliestTimestamp = Long.MAX_VALUE;
            Integer earliestKey = null;
            for (Integer key : cachedPairs.keySet()) {
                long lastAccessed = cachedPairs.get(key).getThird();
                if (lastAccessed < earliestTimestamp) {
                    earliestTimestamp = lastAccessed;
                    earliestKey = key;
                }
            }

            cachedPairs.remove(earliestKey);
        }
    }
}
