// VeriBlock NodeCore CLI
// Copyright 2017-2021 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.
package nodecore.cli.serialization

import nodecore.api.grpc.RpcTransactionInfo
import nodecore.api.grpc.utilities.extensions.toHex

class TransactionReferenceInfo(
    transactionInfo: RpcTransactionInfo
) {
    val timestamp = transactionInfo.timestamp

    val blockNum = transactionInfo.blockNumber

    val confirmations = transactionInfo.confirmations

    val transaction = TransactionInfo(transactionInfo.transaction)

    val endorsedBlockHash = transactionInfo.endorsedBlockHash.toHex()

    val bitcoinBlockHash = transactionInfo.bitcoinBlockHash.toHex()

    val bitcoinTxID = transactionInfo.bitcoinTxId.toHex()

    val bitcoinConfirmations = transactionInfo.bitcoinConfirmations
}
