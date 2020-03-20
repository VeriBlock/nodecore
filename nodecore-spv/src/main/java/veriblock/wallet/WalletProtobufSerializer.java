// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.
package veriblock.wallet;

import nodecore.api.grpc.VeriBlockMessages;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.veriblock.sdk.models.Coin;
import org.veriblock.sdk.models.Sha256Hash;
import org.veriblock.sdk.models.VBlakeHash;
import veriblock.model.AddressFactory;
import veriblock.model.Output;
import veriblock.model.OutputFactory;
import veriblock.model.StandardAddress;
import veriblock.model.StandardTransaction;
import veriblock.model.TransactionMeta;

import java.util.List;
import java.util.stream.Collectors;

public class WalletProtobufSerializer {
    private static final Logger logger = LoggerFactory.getLogger(WalletProtobufSerializer.class);

    public List<Output> deserializeOutputs(List<VeriBlockMessages.Output> output) {
        List<Output> outputs = output.stream().map(out -> deserialize(out)).collect(Collectors.toList());

        return outputs;
    }

    public Output deserialize(VeriBlockMessages.Output output) {
        return new Output(new StandardAddress(output.getAddress().toString()), Coin.valueOf(output.getAmount()));
    }


    public List<StandardTransaction> deserialize(List<Protos.Transaction> transactions) {
        return transactions.stream().map(this::deserialize).collect(Collectors.toList());
    }

    public StandardTransaction deserialize(Protos.Transaction proto) {
        StandardTransaction tx = new StandardTransaction(Sha256Hash.wrap(proto.getTxId().toByteArray()));
        tx.setInputAddress(AddressFactory.create(proto.getInput().getAddress()));
        tx.setInputAmount(Coin.valueOf(proto.getInput().getAmount()));

        for (Protos.TransactionOutput o : proto.getOutputsList()) {
            tx.addOutput(OutputFactory.create(o.getAddress(), o.getAmount()));
        }

        tx.setSignatureIndex(proto.getSignatureIndex());
        tx.setData(proto.getData().toByteArray());
        //TODO: tx.setMerklePath(deserialize(proto.getMerkleBranch()));
        tx.setTransactionMeta(deserialize(proto.getMeta()));

        return tx;
    }

    public TransactionMeta deserialize(Protos.TransactionMeta proto) {
        TransactionMeta meta = new TransactionMeta(Sha256Hash.wrap(proto.getTxId().toByteArray()));
        meta.setState(TransactionMeta.MetaState.forNumber(proto.getStateValue()));
        meta.setAppearsInBestChainBlock(VBlakeHash.wrap(proto.getAppearsInBestChainBlock().toByteArray()));

        proto.getAppearsInBlocksList().forEach(bytes -> meta.addBlockAppearance(VBlakeHash.wrap(bytes.toByteArray())));

        meta.setAppearsAtChainHeight(proto.getAppearsAtHeight());
        meta.setDepth(proto.getDepth());

        return meta;
    }
}
