// VeriBlock NodeCore CLI
// Copyright 2017-2019 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package nodecore.cli.commands.serialization;

import nodecore.api.grpc.TroubleshootPoPTransactionsReply;

public class TroubleshootPoPTransactionsPayload {
    public TroubleshootPoPTransactionsPayload(final TroubleshootPoPTransactionsReply reply) {
        PoPProblemReports = new PoPTransactionProblemReport[reply.getPopProblemReportsCount()];
        for (int i = 0; i < reply.getPopProblemReportsCount(); i++) {
            PoPProblemReports[i] = new PoPTransactionProblemReport(reply.getPopProblemReports(i));
        }
    }

    public PoPTransactionProblemReport[] PoPProblemReports;
}
