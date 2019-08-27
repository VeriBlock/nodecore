// VeriBlock NodeCore CLI
// Copyright 2017-2019 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package nodecore.cli.commands.rpc;

import com.google.inject.Inject;
import io.grpc.StatusRuntimeException;
import nodecore.api.grpc.GetPendingTransactionsReply;
import nodecore.api.grpc.GetPendingTransactionsRequest;
import nodecore.cli.annotations.CommandSpec;
import nodecore.cli.commands.serialization.FormattableObject;
import nodecore.cli.commands.serialization.TransactionInfo;
import nodecore.cli.contracts.Command;
import nodecore.cli.contracts.CommandContext;
import nodecore.cli.contracts.DefaultResult;
import nodecore.cli.contracts.Result;
import nodecore.cli.utilities.CommandUtility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@CommandSpec(
        name = "Get Pending Transactions",
        form = "getpendingtransactions",
        description = "Returns the transactions pending on the network")
public class GetPendingTransactionsCommand implements Command {
    private static final Logger _logger = LoggerFactory.getLogger(GetPendingTransactionsCommand.class);

    @Inject
    public GetPendingTransactionsCommand() {
    }

    @Override
    public Result execute(CommandContext context) {
        Result result = new DefaultResult();

        try {
            GetPendingTransactionsReply reply = context
                    .adminService()
                    .getPendingTransactions(GetPendingTransactionsRequest.newBuilder().build());
            if (!reply.getSuccess()) {
                result.fail();
            } else {
                FormattableObject<List<TransactionInfo>> temp = new FormattableObject<>(reply.getResultsList());
                temp.success = !result.didFail();
                temp.payload = reply.getTransactionsList()
                        .stream()
                        .map(TransactionInfo::new).collect(Collectors.toList());

                context.outputObject(temp);

                context.suggestCommands(Arrays.asList(
                        SendCommand.class,
                        GetBalanceCommand.class,
                        GetHistoryCommand.class,
                        SigIndexCommand.class
                ));

            }
            for (nodecore.api.grpc.Result r : reply.getResultsList())
                result.addMessage(r.getCode(), r.getMessage(), r.getDetails(), r.getError());
        } catch (StatusRuntimeException e) {
            CommandUtility.handleRuntimeException(result, e, _logger);
        }

        return result;
    }
}
