// VeriBlock NodeCore CLI
// Copyright 2017-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package nodecore.cli.serialization;

import com.google.gson.annotations.SerializedName;
import nodecore.api.grpc.VeriBlockMessages;
import nodecore.api.grpc.utilities.ByteStringAddressUtility;
import nodecore.api.grpc.utilities.ByteStringUtility;
import org.veriblock.core.utilities.Utility;

import java.util.ArrayList;
import java.util.List;

public class TransactionInfo {
    public TransactionInfo(final VeriBlockMessages.Transaction transaction) {
        switch (transaction.getType()) {
            case STANDARD:
                type = "standard";
                break;
            case PROOF_OF_PROOF:
                type = "proof_of_proof";
                break;
            case MULTISIG:
                type = "multisig";
                break;
            case UNRECOGNIZED:
                break;
        }

        if (transaction != VeriBlockMessages.Transaction.getDefaultInstance()) {
            txid = ByteStringUtility.byteStringToHex(transaction.getTxId());
            merklePath = transaction.getMerklePath();
            size = transaction.getSize();
            timestamp = transaction.getTimestamp();
            sourceAmount = Utility.formatAtomicLongWithDecimal(transaction.getSourceAmount());
            sourceAddress = ByteStringAddressUtility.parseProperAddressTypeAutomatically(transaction.getSourceAddress());
            data = ByteStringUtility.byteStringToHex(transaction.getData());
            fee = Utility.formatAtomicLongWithDecimal(transaction.getTransactionFee());
            if (transaction.getType() == VeriBlockMessages.Transaction.Type.PROOF_OF_PROOF) {
                byte[] bitcoinBlockHeaderBytes = transaction.getBitcoinBlockHeaderOfProof() != null ?
                    transaction.getBitcoinBlockHeaderOfProof().toByteArray() :
                    new byte[]{};

                if(bitcoinBlockHeaderBytes.length > 0 && bitcoinBlockHeaderBytes[0] == 10 && bitcoinBlockHeaderBytes[1] == 80) {
                    //TODO: remove first two bytes if they are 0A50 (10,80)
                    byte[] newBytes = new byte[bitcoinBlockHeaderBytes.length - 2];
                    System.arraycopy(bitcoinBlockHeaderBytes, 2, newBytes, 0, newBytes.length);
                    bitcoinBlockHeaderBytes = newBytes;
                }
                bitcoinBlockHeader = Utility.bytesToHex(bitcoinBlockHeaderBytes);
                bitcoinTransaction = ByteStringUtility.byteStringToHex(transaction.getBitcoinTransaction());
                endorsedBlockHeader = ByteStringUtility.byteStringToHex(transaction.getEndorsedBlockHeader());
            } else {
                bitcoinBlockHeader = "N/A";
                bitcoinTransaction = "N/A";
                endorsedBlockHeader = "N/A";
                List<String> contextBlockHeaders = new ArrayList<String>();

                contextBlockHeaders.add("N/A");
                // contextBitcoinBlockHeaders = contextBlockHeaders;
            }
            for (final VeriBlockMessages.Output output : transaction.getOutputsList())
                outputs.add(new OutputInfo(output));
        } else {
            size = 0;
            txid = "N/A";
            data = "N/A";
            type = "N/A";
            fee = "N/A";
            timestamp = 0;
            sourceAmount = "N/A";
            merklePath = "N/A";
            sourceAddress = "N/A";
            bitcoinTransaction = "N/A";
            bitcoinBlockHeader = "N/A";
            endorsedBlockHeader = "N/A";
        }
    }

    public int size;

    public String txid;

    public String data;

    public String type;

    public String fee;

    public int timestamp;

    @SerializedName("source_amount")
    public String sourceAmount;

    @SerializedName("merkle_path")
    public String merklePath;

    @SerializedName("source_address")
    public String sourceAddress;

    @SerializedName("bitcoin_transaction")
    public String bitcoinTransaction;

    @SerializedName("bitcoin_block_header_of_proof")
    public String bitcoinBlockHeader;

    @SerializedName("endorsed_block_header")
    public String endorsedBlockHeader;

    // @SerializedName("context_bitcoin_block_headers")
    // public List<String> contextBitcoinBlockHeaders;

    public List<OutputInfo> outputs = new ArrayList<>();
}
