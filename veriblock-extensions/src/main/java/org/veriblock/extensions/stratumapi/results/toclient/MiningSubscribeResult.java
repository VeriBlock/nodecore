package org.veriblock.extensions.stratumapi.results.toclient;

import com.google.gson.JsonArray;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import org.veriblock.extensions.stratumapi.arguments.StratumArgumentError;
import org.veriblock.extensions.stratumapi.arguments.StratumArgumentId;
import org.veriblock.extensions.stratumapi.arguments.StratumArgumentSubscriptionTriple;
import org.veriblock.extensions.stratumapi.arguments.StratumArgumentSyntheticExtraNonce;
import org.veriblock.extensions.stratumapi.results.StratumResult;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * A MiningSubscribeResult [in EthereumStratum/1.0.0, not Bitcoin stratum!] contains an array of 2-item triples containing the subscription
 * type(s), id(s), and protocol version(s), along with an extra_nonce.
 *
 * Note: In order to support VeriBlock's ProgPoW modifications, the extra_nonce MUST BE 4-bytes and is ALL ZERO, which forces the clients to produce
 * results with an effective 4-byte nonce space. VeriBlock headers only permit a 4-byte nonce, which during hash evaluation is zero-padded to 8 bytes
 * to maintain backwards compatibility with ProgPoW's expectation of an 8-byte nonce.
 *
 * Note that the "extra_nonce" used in EthereumStratum/1.0.0 is not truly an "extra_nonce"; it does not allow the generation of alternate header
 * hashes for initial input into ProgPoW, but instead is an artificial extra_nonce implementation which takes up some of the starting bytes of
 * Ethereum's 8-byte normal nonce. VeriBlock's 4-byte nonce means that the pool has to generate different header hashes for each client and give
 * each client the same "extra_nonce" of 0x00000000, to ensure all clients generate work by only exploring the bottom 4 bytes of nonce space.
 *
 * The 4-byte nonce search range allows 4 GH of total mining, which at a rate of 200 MH/s (multiple high-end GPUs) supports 20 seconds of mining.
 * The VeriBlock pool should send mining jobs every several seconds, ensuring even high-hashrate clients never run out of work.
 *
 * Extremely high hashrate miners (above ~1GH/s) or miners who don't efficiently explore the nonce range (skipping many valid nonces) may require
 * multiple parallel subscriptions to ensure enough work. An improved Stratum protocol with support for VeriBlock-specific features is in development
 * which will enable miners to use the true extra_nonce present in VeriBlock to generate multiple header hashes to use as input into ProgPoW. This
 * upgraded version of Stratum will enable exacale-sized mining farms to run from a single Stratum connection.
 */
public class MiningSubscribeResult extends StratumResult {
    private final StratumArgumentId id;
    private final List<StratumArgumentSubscriptionTriple> subscriptionTriples;
    private final StratumArgumentSyntheticExtraNonce syntheticExtraNonce;
    private final StratumArgumentError errorString;

    public MiningSubscribeResult(StratumArgumentId id,
                                 List<StratumArgumentSubscriptionTriple> subscriptionTriples,
                                 StratumArgumentSyntheticExtraNonce syntheticExtraNonce,
                                 StratumArgumentError errorString) {
        if (id == null) {
            throw new IllegalArgumentException("MiningSubscribeResult cannot be constructed with a null id!");
        }

        if (subscriptionTriples == null) {
            throw new IllegalArgumentException("MiningSubscribeResult cannot be constructed with a null list of subscription triples!");
        }

        if (syntheticExtraNonce == null) {
            throw new IllegalArgumentException("MiningSubscribeResult cannot be constructed with a null extra nonce!");
        }

        this.id = id;

        this.subscriptionTriples = new ArrayList<>(subscriptionTriples);

        this.syntheticExtraNonce = syntheticExtraNonce;

        this.errorString = errorString;
    }

    public String compileResult() {
        JsonArray result = new JsonArray();
        for (StratumArgumentSubscriptionTriple triple : subscriptionTriples) {
            result.add(triple.getJsonArray());
        }

        result.add(syntheticExtraNonce.getSerialized());

        JsonObject root = new JsonObject();
        root.addProperty("id", id.getData());
        root.add("result", result);

        if (errorString == null){
            root.add("error", JsonNull.INSTANCE);
        } else {
            root.add("error", errorString.getJsonArray());
        }

        return root.toString();
    }
}
