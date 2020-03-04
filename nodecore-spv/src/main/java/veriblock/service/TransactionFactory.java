package veriblock.service;

import nodecore.api.grpc.VeriBlockMessages;
import veriblock.model.Transaction;

public interface TransactionFactory {

    Transaction create(VeriBlockMessages.TransactionUnion union);

    Transaction create(VeriBlockMessages.SignedTransaction message);

    Transaction create(VeriBlockMessages.SignedMultisigTransaction message);
}
