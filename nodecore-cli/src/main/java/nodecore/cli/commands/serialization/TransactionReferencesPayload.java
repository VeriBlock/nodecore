// VeriBlock NodeCore CLI
// Copyright 2017-2019 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package nodecore.cli.commands.serialization;

import java.util.ArrayList;
import java.util.List;

public class TransactionReferencesPayload {
    public TransactionReferencesPayload(final List<nodecore.api.grpc.TransactionInfo> list) {
        for (final nodecore.api.grpc.TransactionInfo transaction : list)
            transactions.add(new TransactionReferenceInfo(transaction));
    }

    public List<TransactionReferenceInfo> transactions = new ArrayList<>();
}
