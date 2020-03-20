// VeriBlock NodeCore CLI
// Copyright 2017-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package nodecore.cli.serialization;

import nodecore.api.grpc.VeriBlockMessages;
import nodecore.api.grpc.utilities.ByteStringUtility;
import org.veriblock.core.utilities.Utility;

public class MakeUnsignedMultisigTxPayload {
    public MakeUnsignedMultisigTxPayload(final VeriBlockMessages.MakeUnsignedMultisigTxReply reply) {
        this.signatureThresholdM = reply.getSignatureThresholdM();
        this.addressCompositionCountN = reply.getAddressCompositionCountN();
        this.unsignedTransaction = new TransactionInfo(reply.getUnsignedMultisigTransactionWithIndex().getUnsignedMultisigTansaction());
        this.unsignedTransactionHex = Utility.bytesToHex(reply.getUnsignedMultisigTransactionWithIndex().toByteArray());
        this.txid = ByteStringUtility.byteStringToHex(reply.getTxid());
        this.sigIndex = reply.getSignatureIndex();
        this.instructions = "Have at least " + signatureThresholdM + " of the component addresses sign the TxID listed above (see: signhexmessage command) to validly sign this transaction, then submit the raw transaction hex listed above (see: submitmultisigtx command)!";
    }

    public int signatureThresholdM;
    public int addressCompositionCountN;
    public TransactionInfo unsignedTransaction;
    public String unsignedTransactionHex;
    public String txid;
    public long sigIndex;
    public String instructions;
}
