// VeriBlock NodeCore CLI
// Copyright 2017-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package nodecore.cli.serialization;

import com.google.gson.annotations.SerializedName;
import com.google.protobuf.ByteString;
import nodecore.api.grpc.VeriBlockMessages;
import nodecore.api.grpc.utilities.ByteStringUtility;
import org.veriblock.core.utilities.BlockUtility;
import org.veriblock.core.utilities.Utility;

import java.util.ArrayList;
import java.util.List;

public class BlockInfo {
    public BlockInfo(final VeriBlockMessages.Block block) {
        hash = ByteStringUtility.byteStringToHex(block.getHash());
        number = block.getNumber();
        version = (short)block.getVersion();
        previousHash = ByteStringUtility.byteStringToHex(block.getPreviousHash());
        if (number % 20 == 0) {
            previousKeystoneHash = ByteStringUtility.byteStringToHex(block.getSecondPreviousHash());
            secondPreviousKeystoneHash = ByteStringUtility.byteStringToHex(block.getThirdPreviousHash());
        } else {
            secondPreviousHash = ByteStringUtility.byteStringToHex(block.getSecondPreviousHash());
            thirdPreviousHash = ByteStringUtility.byteStringToHex(block.getThirdPreviousHash());
        }
        merkleRoot = ByteStringUtility.byteStringToHex(block.getMerkleRoot());
        timestamp = block.getTimestamp();
        encoded_difficulty = block.getEncodedDifficulty();
        winningNonce = block.getWinningNonce();

        for (VeriBlockMessages.TransactionUnion union : block.getRegularTransactionsList())
            if (union.getTransactionCase() == VeriBlockMessages.TransactionUnion.TransactionCase.SIGNED) {
                regular_transactions.add(ByteStringUtility.byteStringToHex(union.getSigned().getTransaction().getTxId()));
            }
            else {
                regular_transactions.add(ByteStringUtility.byteStringToHex(union.getSignedMultisig().getTransaction().getTxId()));
            }
        for (VeriBlockMessages.TransactionUnion union : block.getPopTransactionsList())
            pop_transactions.add(ByteStringUtility.byteStringToHex(union.getSigned().getTransaction().getTxId()));

        totalFees = Utility.formatAtomicLongWithDecimal(block.getTotalFees());
        powCoinbaseReward = Utility.formatAtomicLongWithDecimal(block.getPowCoinbaseReward());
        popCoinbaseReward = Utility.formatAtomicLongWithDecimal(block.getPopCoinbaseReward());

        for (ByteString header : block.getBitcoinBlockHeadersList())
            bitcoinBlockHeaders.add(ByteStringUtility.byteStringToHex(header));

        blockContentMetapackage = new BlockContentMetapackageInfo(block.getBlockContentMetapackage());
        size = block.getSize();

        if (number % 20 == 0) {
            header = Utility.bytesToHex(BlockUtility.assembleBlockHeader(number,
                    version,
                    previousHash,
                    previousKeystoneHash,
                    secondPreviousKeystoneHash,
                    merkleRoot,
                    timestamp,
                    encoded_difficulty,
                    winningNonce));
        } else {
            header = Utility.bytesToHex(BlockUtility.assembleBlockHeader(number,
                    version,
                    previousHash,
                    secondPreviousHash,
                    thirdPreviousHash,
                    merkleRoot,
                    timestamp,
                    encoded_difficulty,
                    winningNonce));
        }
    }

    @SerializedName("hash")
    public String hash;

    @SerializedName("header")
    public String header;

    @SerializedName("number")
    public int number;

    @SerializedName("version")
    public short version;

    @SerializedName("previous_hash")
    public String previousHash;

    @SerializedName("second_previous_hash")
    public String secondPreviousHash;

    @SerializedName("previous_keystone_hash")
    public String previousKeystoneHash;

    @SerializedName("third_previous_hash")
    public String thirdPreviousHash;

    @SerializedName("second_previous_keystone_hash")
    public String secondPreviousKeystoneHash;

    @SerializedName("merkle_root")
    public String merkleRoot;

    @SerializedName("timestamp")
    public int timestamp;

    @SerializedName("encoded_difficulty")
    public int encoded_difficulty;

    @SerializedName("winning_nonce")
    public int winningNonce;

    @SerializedName("bitcoin_block_headers")
    public List<String> bitcoinBlockHeaders = new ArrayList<>();

    @SerializedName("regular_transactions")
    public List<String> regular_transactions = new ArrayList<>();

    @SerializedName("pop_transactions")
    public List<String> pop_transactions = new ArrayList<>();

    @SerializedName("total_fees")
    public String totalFees;

    @SerializedName("pow_coinbase_reward")
    public String powCoinbaseReward;

    @SerializedName("pop_coinbase_reward")
    public String popCoinbaseReward;

    @SerializedName("block_content_metapackage")
    public BlockContentMetapackageInfo blockContentMetapackage;

    @SerializedName("size")
    public int size;
}
