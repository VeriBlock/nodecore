// VeriBlock NodeCore CLI
// Copyright 2017-2019 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package nodecore.cli.commands.serialization;

import nodecore.api.grpc.TransactionUnion;

public class TransactionUnionInfo {
    public TransactionUnionInfo(final TransactionUnion union) {
        switch (union.getTransactionCase()) {
            case UNSIGNED:
                type = "unsigned";
                unsigned = new TransactionInfo(union.getUnsigned());
                break;
            case SIGNED:
                type = "signed";
                signed = new SignedTransactionInfo(union.getSigned());
                break;
            case TRANSACTION_NOT_SET:
                type = "unknown";
                break;
        }
    }

    public String type;

    public TransactionInfo unsigned;

    public SignedTransactionInfo signed;
}
