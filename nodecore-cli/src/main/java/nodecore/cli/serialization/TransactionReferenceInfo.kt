// VeriBlock NodeCore CLI
// Copyright 2017-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.
package nodecore.cli.serialization

import nodecore.api.grpc.VeriBlockMessages
import nodecore.api.grpc.utilities.ByteStringUtility

class TransactionReferenceInfo(
    transactionInfo: VeriBlockMessages.TransactionInfo
) {
    val timestamp = transactionInfo.timestamp

    val blockNum = transactionInfo.blockNumber

    val confirmations = transactionInfo.confirmations

    val transaction = TransactionInfo(transactionInfo.transaction)

    val endorsedBlockHash = ByteStringUtility.byteStringToHex(transactionInfo.endorsedBlockHash)

    val bitcoinBlockHash = ByteStringUtility.byteStringToHex(transactionInfo.bitcoinBlockHash)

    val bitcoinTxID = ByteStringUtility.byteStringToHex(transactionInfo.bitcoinTxId)

    val bitcoinConfirmations = transactionInfo.bitcoinConfirmations
}
