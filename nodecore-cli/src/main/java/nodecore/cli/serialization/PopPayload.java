// VeriBlock NodeCore CLI
// Copyright 2017-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package nodecore.cli.serialization;

import nodecore.api.grpc.VeriBlockMessages;
import nodecore.api.grpc.utilities.ByteStringUtility;

public class PopPayload {
    public PopPayload(final VeriBlockMessages.GetPopReply reply) {
        block_height = reply.getBlockHeight();
        version = reply.getVersion();
        previous_block_hash = ByteStringUtility.byteStringToHex(reply.getPreviousBlockHash());
        second_previous_block_hash = ByteStringUtility.byteStringToHex(reply.getSecondPreviousBlockHash());
        third_previous_block_hash = ByteStringUtility.byteStringToHex(reply.getThirdPreviousBlockHash());
        merkle_root = ByteStringUtility.byteStringToHex(reply.getMerkleRoot());
        timestamp = reply.getTimestamp();
        encoded_difficulty = reply.getEncodedDifficulty();
        nonce = reply.getNonce();

        pop_miner_address = ByteStringUtility.byteStringToBase58(reply.getPopMinerAddress());
        full_pop = ByteStringUtility.byteStringToHex(reply.getFullPop());
        last_known_block = ByteStringUtility.byteStringToHex(reply.getLastKnownBlock().getHeader());
    }

    public int block_height;

    public int version;

    public String previous_block_hash;

    public String second_previous_block_hash;

    public String third_previous_block_hash;

    public String merkle_root;

    public int timestamp;

    public int encoded_difficulty;

    public int nonce;

    public String pop_miner_address;

    public String full_pop;

    public String last_known_block;
}
