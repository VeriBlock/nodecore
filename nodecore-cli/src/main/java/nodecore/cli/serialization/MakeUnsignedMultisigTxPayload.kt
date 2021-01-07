// VeriBlock NodeCore CLI
// Copyright 2017-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.
package nodecore.cli.serialization

import nodecore.api.grpc.VeriBlockMessages.MakeUnsignedMultisigTxReply
import nodecore.api.grpc.utilities.ByteStringUtility
import org.veriblock.core.utilities.Utility

class MakeUnsignedMultisigTxPayload(
    reply: MakeUnsignedMultisigTxReply
) {
    val signatureThresholdM = reply.signatureThresholdM

    val addressCompositionCountN = reply.addressCompositionCountN

    val unsignedTransaction = TransactionInfo(reply.unsignedMultisigTransactionWithIndex.unsignedMultisigTansaction)

    val unsignedTransactionHex = Utility.bytesToHex(reply.unsignedMultisigTransactionWithIndex.toByteArray())

    val txid = reply.txid.toHex()

    val sigIndex = reply.signatureIndex

    val instructions = "Have at least $signatureThresholdM of the component addresses sign the TxID listed above (see: signhexmessage command) to validly sign this transaction, then submit the raw transaction hex listed above (see: submitmultisigtx command)!"
}
