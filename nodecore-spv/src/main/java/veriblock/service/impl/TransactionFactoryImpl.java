package veriblock.service.impl;

import nodecore.api.grpc.VeriBlockMessages;
import nodecore.api.grpc.utilities.ByteStringAddressUtility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.veriblock.core.utilities.AddressUtility;
import org.veriblock.sdk.models.Sha256Hash;
import veriblock.conf.NetworkParameters;
import veriblock.model.StandardTransaction;
import veriblock.model.Transaction;
import veriblock.service.TransactionFactory;
import veriblock.wallet.WalletProtobufSerializer;

public class TransactionFactoryImpl implements TransactionFactory {
    private static final Logger logger = LoggerFactory.getLogger(TransactionFactoryImpl.class);

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

        Transaction transaction = new StandardTransaction(
            Sha256Hash.wrap(message.getTransaction().getTxId().toByteArray()),
            ByteStringAddressUtility.parseProperAddressTypeAutomatically(message.getTransaction().getSourceAddress()),
            message.getTransaction().getSourceAmount(),
            serializer.deserializeOutputs(message.getTransaction().getOutputsList()),
            message.getSignatureIndex(),
            message.getTransaction().getData().toByteArray(),
            networkParameters
        );

        transaction.addSignature(message.getSignature().toByteArray(), message.getPublicKey().toByteArray());
        boolean valid = AddressUtility.isSignatureValid(
            transaction.getTxId().getBytes(), transaction.getSignature(), transaction.getPublicKey(), transaction.getInputAddress().get()
        );

        if (!valid) {
            logger.error("Transaction signature is not valid.");
            throw new RuntimeException();
        }

        return transaction;
    }

    @Override
    public Transaction create(VeriBlockMessages.SignedMultisigTransaction message) {
        //TODO implement working with Multisig
//        setSignatureBundle(message.getSignatureBundle());
        throw  new UnsupportedOperationException();
    }

}
