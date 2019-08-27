// VeriBlock NodeCore CLI
// Copyright 2017-2019 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package nodecore.cli.commands.serialization;

import nodecore.api.grpc.SubmitMultisigTxReply;
import nodecore.api.grpc.utilities.ByteStringUtility;

public class SubmitMultisigTxPayload {
    public SubmitMultisigTxPayload(final SubmitMultisigTxReply reply) {
        this.transaction = new TransactionInfo(reply.getSignedMultisigTransaction().getTransaction());
        this.multisigSlotBundle = new MultisigBundleInfo(reply.getSignedMultisigTransaction().getSignatureBundle());
        this.txid = ByteStringUtility.byteStringToHex(reply.getTxid());
        this.sigIndex = reply.getSignedMultisigTransaction().getSignatureIndex();
    }

    public TransactionInfo transaction;
    public String txid;
    public MultisigBundleInfo multisigSlotBundle;
    public long sigIndex;
}
