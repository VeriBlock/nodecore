// VeriBlock NodeCore CLI
// Copyright 2017-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package nodecore.cli.serialization;

import com.google.gson.annotations.SerializedName;
import nodecore.api.grpc.VeriBlockMessages;
import nodecore.api.grpc.utilities.ByteStringUtility;

public class SignedTransactionInfo {
    public SignedTransactionInfo(final VeriBlockMessages.SignedTransaction signed) {
        signatureIndex = signed.getSignatureIndex();
        signature = ByteStringUtility.byteStringToHex(signed.getSignature());
        publicKey = ByteStringUtility.byteStringToHex(signed.getPublicKey());
        transaction = new TransactionInfo(signed.getTransaction());
    }

    public SignedTransactionInfo(final VeriBlockMessages.SignedTransactionInfo signed) {
        signatureIndex = signed.getSignatureIndex();
        signature = ByteStringUtility.byteStringToHex(signed.getSignature());
        publicKey = ByteStringUtility.byteStringToHex(signed.getPublicKey());
        transaction = new TransactionInfo(signed.getTransaction().getTransaction());
        confirmations = signed.getTransaction().getConfirmations();
        bitcoinConfirmations = signed.getTransaction().getBitcoinConfirmations();
    }

    public String signature;

    @SerializedName("public_key")
    public String publicKey;

    @SerializedName(("signature_index"))
    public long signatureIndex;

    public TransactionInfo transaction;

    @SerializedName("confirmations")
    public int confirmations;

    @SerializedName("bitcoinConfirmations")
    public int bitcoinConfirmations;
}
