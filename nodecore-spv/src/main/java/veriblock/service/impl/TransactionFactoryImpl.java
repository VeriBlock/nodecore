package veriblock.service.impl;

import nodecore.api.grpc.VeriBlockMessages;
import veriblock.conf.NetworkParameters;
import veriblock.model.StandardTransaction;
import veriblock.model.Transaction;
import veriblock.service.TransactionFactory;
import veriblock.wallet.WalletProtobufSerializer;

public class TransactionFactoryImpl implements TransactionFactory {

    private final NetworkParameters networkParameters;

    public TransactionFactoryImpl(NetworkParameters networkParameters) {
        this.networkParameters = networkParameters;
    }

    @Override
    public Transaction create(VeriBlockMessages.TransactionUnion message) {
        switch (message.getTransactionCase()) {
            case UNSIGNED:
                throw new IllegalArgumentException("Cannot use this constructor with an Unsigned transaction type");
            case SIGNED:
                return create(message.getSigned());
            case SIGNED_MULTISIG:
                return create(message.getSignedMultisig());
            case TRANSACTION_NOT_SET:
                throw new IllegalArgumentException("Cannot use this constructor with an unset transaction type");
        }

        return null;
    }

    @Override
    public Transaction create(VeriBlockMessages.SignedTransaction message) {
        WalletProtobufSerializer serializer = new WalletProtobufSerializer();

        Transaction transaction =
            new StandardTransaction(message.getTransaction().getSourceAddress().toString(), message.getTransaction().getSourceAmount(),
                serializer.deserializeOutputs(message.getTransaction().getOutputsList()), message.getSignatureIndex(), networkParameters
            );

        transaction.addSignature(message.getSignature().toByteArray(), message.getPublicKey().toByteArray());

        return transaction;
    }

    @Override
    public Transaction create(VeriBlockMessages.SignedMultisigTransaction message) {
        //TODO implement working with Multisig
//        setSignatureBundle(message.getSignatureBundle());
        throw  new UnsupportedOperationException();
    }

}
