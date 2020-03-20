// VeriBlock NodeCore
// Copyright 2017-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package nodecore.cli.serialization;

import com.google.gson.annotations.SerializedName;
import com.opencsv.bean.CsvBindByName;
import nodecore.api.grpc.VeriBlockMessages;
import nodecore.api.grpc.utilities.ByteStringAddressUtility;
import nodecore.api.grpc.utilities.ByteStringUtility;
import org.veriblock.core.utilities.BlockUtility;
import org.veriblock.core.utilities.Utility;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

public class WalletTransactionInfo {

    public WalletTransactionInfo(VeriBlockMessages.WalletTransaction transaction) {
        txId  = ByteStringUtility.byteStringToHex(transaction.getTxId());
        ZonedDateTime date = Instant.ofEpochSecond(transaction.getTimestamp()).atZone(ZoneId.systemDefault());
        timestamp = date.format(DateTimeFormatter.ofPattern("MM/dd/yyyy HH:mm:ss z"));

        confirmations = transaction.getMeta().getConfirmations();

        //get block
        byte[] header = transaction.getMeta().getBlockHeader().toByteArray();
        if (header.length > 0) {
            blockHeight = BlockUtility.extractBlockHeightFromBlockHeader(header);
        }

        addressMine = ByteStringAddressUtility.parseProperAddressTypeAutomatically(transaction.getAddress());
        status = transaction.getMeta().getStatus().name();
        txType = transaction.getType().name();

        if (txType.equals("RECEIVED")) {
            addressFrom = ByteStringAddressUtility.parseProperAddressTypeAutomatically(transaction.getInput().getAddress());
            if(transaction.getOutputsCount() > 0) {
                addressTo = ByteStringAddressUtility.parseProperAddressTypeAutomatically(transaction.getOutputs(0).getAddress());
            }
        } else if (txType.equals("SENT")) {
            addressFrom = ByteStringAddressUtility.parseProperAddressTypeAutomatically(transaction.getInput().getAddress());
            if(transaction.getOutputsCount() > 0) {

                for(int i=0; i<transaction.getOutputsCount(); i++) {
                    addressTo += ByteStringAddressUtility.parseProperAddressTypeAutomatically(transaction.getOutputs(i).getAddress());
                    if(i < (transaction.getOutputsCount()-1)) {
                        addressTo += ":";
                    }
                }
            }
        }

        amount = Utility.formatAtomicLongWithDecimal(transaction.getNetAmount());
    }

    @CsvBindByName
    @SerializedName("transaction_id")
    private String txId;

    @CsvBindByName
    @SerializedName("timestamp")
    private String timestamp;

    @CsvBindByName
    @SerializedName("address_mine")
    private String addressMine;

    @CsvBindByName
    @SerializedName("address_to")
    private String addressTo;

    @CsvBindByName
    @SerializedName("address_from")
    private String addressFrom;

    @CsvBindByName
    @SerializedName("amount")
    private String amount;

    //CB|TX sent | TX Received
    @CsvBindByName
    @SerializedName("transaction_type")
    private String txType;

    //UNKNOWN|PENDING|CONFIRMED|DEAD
    @CsvBindByName
    @SerializedName("status")
    private String status;

    @CsvBindByName
    @SerializedName("confirmations")
    private int confirmations;

    @CsvBindByName
    @SerializedName("block_height")
    private int blockHeight;

    public String getTxId() {
        return txId;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public String getAddressMine() {
        return addressMine;
    }

    public String getAddressTo() {
        return addressTo;
    }

    public String getAddressFrom() {
        return addressFrom;
    }

    public String getAmount() {
        return amount;
    }

    public String getTxType() {
        return txType;
    }

    public String getStatus() {
        return status;
    }

    public int getConfirmations() {
        return confirmations;
    }

    public int getBlockHeight() {
        return blockHeight;
    }
}
