// VeriBlock NodeCore
// Copyright 2017-2021 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.
package nodecore.cli.serialization

import com.google.gson.annotations.SerializedName
import com.opencsv.bean.CsvBindByName
import nodecore.api.grpc.RpcWalletTransaction
import org.veriblock.sdk.extensions.toHex
import org.veriblock.sdk.extensions.toProperAddressType
import org.veriblock.core.utilities.BlockUtility
import org.veriblock.core.utilities.extensions.formatAtomicLongWithDecimal
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class WalletTransactionInfo(
    transaction: RpcWalletTransaction
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
    val addressMine = transaction.address.toProperAddressType()

    //CB|TX sent | TX Received
    @CsvBindByName
    @SerializedName("transaction_type")
    val txType = transaction.type.name

    @CsvBindByName
    @SerializedName("address_to")
    val addressTo = if (txType == "RECEIVED" && transaction.outputsCount > 0) {
        transaction.getOutputs(0).address.toProperAddressType()
    } else if (txType == "SENT" && transaction.outputsCount > 0) {
        transaction.outputsList.joinToString(":") {
            it.address.toProperAddressType()
        }
    } else {
        null
    }

    @CsvBindByName
    @SerializedName("address_from")
    val addressFrom = if (this.txType == "RECEIVED") {
        transaction.input.address.toProperAddressType()
    } else if (txType == "SENT") {
        transaction.input.address.toProperAddressType()
    } else {
        null
    }

    @CsvBindByName
    @SerializedName("amount")
    val amount = transaction.netAmount.formatAtomicLongWithDecimal()

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
