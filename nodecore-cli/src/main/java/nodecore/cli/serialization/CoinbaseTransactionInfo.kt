// VeriBlock NodeCore CLI
// Copyright 2017-2021 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.
package nodecore.cli.serialization

import com.google.gson.annotations.SerializedName
import nodecore.api.grpc.RpcCoinbaseTransaction
import nodecore.api.grpc.utilities.extensions.toHex
import org.veriblock.core.utilities.Utility

class CoinbaseTransactionInfo(
    coinbaseTransaction: RpcCoinbaseTransaction
) {
    @SerializedName("pow_coinbase_amount")
    val powCoinbaseAmount = Utility.formatAtomicLongWithDecimal(coinbaseTransaction.powCoinbaseAmount)

    @SerializedName("pop_coinbase_amount")
    val popCoinbaseAmount = Utility.formatAtomicLongWithDecimal(coinbaseTransaction.popCoinbaseAmount)

    @SerializedName("pow_fee_share")
    val powFeeShare = Utility.formatAtomicLongWithDecimal(coinbaseTransaction.powFeeShare)

    @SerializedName("pop_fee_share")
    val popFeeShare = Utility.formatAtomicLongWithDecimal(coinbaseTransaction.popFeeShare)

    @SerializedName("pow_outputs")
    val powOutputs = coinbaseTransaction.powOutputsList.map { output ->
        OutputInfo(output)
    }

    @SerializedName("pop_outputs")
    val popOutputs = coinbaseTransaction.popOutputsList.map { output ->
        OutputInfo(output)
    }

    val coinbase_tx_hash = coinbaseTransaction.txId.toHex()
}
