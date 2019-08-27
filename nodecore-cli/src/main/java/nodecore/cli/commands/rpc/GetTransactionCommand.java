// VeriBlock NodeCore CLI
// Copyright 2017-2019 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package nodecore.cli.commands.rpc;

import com.google.inject.Inject;
import io.grpc.StatusRuntimeException;
import nodecore.api.grpc.GetTransactionsReply;
import nodecore.api.grpc.GetTransactionsRequest;
import nodecore.api.grpc.utilities.ByteStringUtility;
import nodecore.cli.annotations.CommandParameterType;
import nodecore.cli.annotations.CommandSpec;
import nodecore.cli.annotations.CommandSpecParameter;
import nodecore.cli.commands.serialization.FormattableObject;
import nodecore.cli.commands.serialization.TransactionReferencesPayload;
import nodecore.cli.contracts.Command;
import nodecore.cli.contracts.CommandContext;
import nodecore.cli.contracts.DefaultResult;
import nodecore.cli.contracts.Result;
import nodecore.cli.utilities.CommandUtility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;

@CommandSpec(
        name = "Get Transaction",
        form = "gettransaction",
        description = "Gets information regarding provided TxID")
@CommandSpecParameter(name = "txId", required = true, type = CommandParameterType.STRING)
@CommandSpecParameter(name = "searchLength", required = false, type = CommandParameterType.INTEGER)
public class GetTransactionCommand implements Command {
    private static final Logger _logger = LoggerFactory.getLogger(GetTransactionCommand.class);

    @Inject
    public GetTransactionCommand() {}

    @Override
    public Result execute(CommandContext context) {
        Result result = new DefaultResult();

        String transactionId = context.getParameter("txId");
        Integer searchLength = context.getParameter("searchLength");
        try {
            GetTransactionsRequest.Builder requestBuilder = GetTransactionsRequest.newBuilder();
            requestBuilder.addIds(ByteStringUtility.hexToByteString(transactionId));

            if (searchLength != null)
                requestBuilder.setSearchLength(searchLength);

            GetTransactionsReply reply = context.adminService().getTransactions(requestBuilder.build());
            if (!reply.getSuccess()) {
                result.fail();
            } else {
                FormattableObject<TransactionReferencesPayload> temp = new FormattableObject<>(reply.getResultsList());
                temp.success = !result.didFail();
                temp.payload = new TransactionReferencesPayload(reply.getTransactionsList());

                context.outputObject(temp);

                context.suggestCommands(Arrays.asList(
                        GetBalanceCommand.class,
                        GetHistoryCommand.class
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
