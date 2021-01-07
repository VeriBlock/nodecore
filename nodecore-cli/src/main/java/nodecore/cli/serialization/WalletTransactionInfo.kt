// VeriBlock NodeCore
// Copyright 2017-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.
package nodecore.cli.serialization

import com.google.gson.annotations.SerializedName
import com.opencsv.bean.CsvBindByName
import nodecore.api.grpc.VeriBlockMessages
import nodecore.api.grpc.utilities.ByteStringAddressUtility
import nodecore.api.grpc.utilities.ByteStringUtility
import org.veriblock.core.utilities.BlockUtility
import org.veriblock.core.utilities.Utility
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class WalletTransactionInfo(
    transaction: VeriBlockMessages.WalletTransaction
) {
    @CsvBindByName
    @SerializedName("transaction_id")
    val txId = transaction.txId.toHex()

    @CsvBindByName
    @SerializedName("timestamp")
    val timestamp = Instant.ofEpochSecond(
        transaction.timestamp.toLong()
    ).atZone(
        ZoneId.systemDefault()
    ).format(
        DateTimeFormatter.ofPattern("MM/dd/yyyy HH:mm:ss z")
    )

    @CsvBindByName
    @SerializedName("address_mine")
    val addressMine = ByteStringAddressUtility.parseProperAddressTypeAutomatically(transaction.address)

    //CB|TX sent | TX Received
    @CsvBindByName
    @SerializedName("transaction_type")
    val txType = transaction.type.name

    @CsvBindByName
    @SerializedName("address_to")
    val addressTo = if (txType == "RECEIVED" && transaction.outputsCount > 0) {
        ByteStringAddressUtility.parseProperAddressTypeAutomatically(transaction.getOutputs(0).address)
    } else if (txType == "SENT" && transaction.outputsCount > 0) {
        transaction.outputsList.joinToString(":") {
            ByteStringAddressUtility.parseProperAddressTypeAutomatically(it.address)
        }
    } else {
        null
    }

    @CsvBindByName
    @SerializedName("address_from")
    val addressFrom = if (this.txType == "RECEIVED") {
        ByteStringAddressUtility.parseProperAddressTypeAutomatically(transaction.input.address)
    } else if (txType == "SENT") {
        ByteStringAddressUtility.parseProperAddressTypeAutomatically(transaction.input.address)
    } else {
        null
    }

    @CsvBindByName
    @SerializedName("amount")
    val amount = Utility.formatAtomicLongWithDecimal(transaction.netAmount)

    //UNKNOWN|PENDING|CONFIRMED|DEAD
    @CsvBindByName
    @SerializedName("status")
    val status = transaction.meta.status.name

    @CsvBindByName
    @SerializedName("confirmations")
    val confirmations = transaction.meta.confirmations

    @CsvBindByName
    @SerializedName("block_height")
    val blockHeight = if (transaction.meta.blockHeader.toByteArray().isNotEmpty()) {
        BlockUtility.extractBlockHeightFromBlockHeader(transaction.meta.blockHeader.toByteArray())
    } else {
        0
    }
}
