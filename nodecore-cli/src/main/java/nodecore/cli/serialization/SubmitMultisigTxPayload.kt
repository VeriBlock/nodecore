// VeriBlock NodeCore CLI
// Copyright 2017-2021 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.
package nodecore.cli.serialization

import nodecore.api.grpc.RpcSubmitMultisigTxReply
import org.veriblock.sdk.extensions.toHex

class SubmitMultisigTxPayload(
    reply: RpcSubmitMultisigTxReply
) {
    val transaction = TransactionInfo(reply.signedMultisigTransaction.transaction)

    val txid = reply.txid.toHex()

    val multisigSlotBundle = MultisigBundleInfo(reply.signedMultisigTransaction.signatureBundle)

    val sigIndex = reply.signedMultisigTransaction.signatureIndex
}
