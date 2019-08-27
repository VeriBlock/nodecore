// VeriBlock NodeCore CLI
// Copyright 2017-2019 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package nodecore.cli.commands.rpc;

import com.google.inject.Inject;
import io.grpc.StatusRuntimeException;
import nodecore.api.grpc.ProtocolReply;
import nodecore.api.grpc.SubmitTransactionsRequest;
import nodecore.api.grpc.TransactionUnion;
import nodecore.cli.annotations.CommandParameterType;
import nodecore.cli.annotations.CommandSpec;
import nodecore.cli.annotations.CommandSpecParameter;
import nodecore.cli.commands.serialization.EmptyPayload;
import nodecore.cli.commands.serialization.FormattableObject;
import nodecore.cli.contracts.Command;
import nodecore.cli.contracts.CommandContext;
import nodecore.cli.contracts.DefaultResult;
import nodecore.cli.contracts.Result;
import nodecore.cli.utilities.CommandUtility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.veriblock.core.utilities.Utility;

@CommandSpec(
        name = "Submit Transaction",
        form = "submittx",
        description = "Submit a raw transaction to be added to the mempool")
@CommandSpecParameter(name = "rawTransaction", required = true, type = CommandParameterType.STRING)
public class SubmitTxCommand implements Command {
    private static final Logger _logger = LoggerFactory.getLogger(SubmitTxCommand.class);

    @Inject
    public SubmitTxCommand() {
    }

    @Override
    public Result execute(CommandContext context) throws Exception {
        Result result = new DefaultResult();

        String rawTransaction = context.getParameter("rawTransaction");
        try {
            ProtocolReply reply = context
                    .adminService()
                    .submitTransactions(SubmitTransactionsRequest
                            .newBuilder()
                            .addTransactions(TransactionUnion.parseFrom(Utility.hexToBytes(rawTransaction)))
                            .build());
            if (!reply.getSuccess()) {
                result.fail();
            } else {
                FormattableObject<EmptyPayload> temp = new FormattableObject<>(reply.getResultsList());
                temp.success = !result.didFail();
                temp.payload = new EmptyPayload();

                context.outputObject(temp);
            }
            for (nodecore.api.grpc.Result r : reply.getResultsList())
                result.addMessage(r.getCode(), r.getMessage(), r.getDetails(), r.getError());
        } catch (StatusRuntimeException e) {
            CommandUtility.handleRuntimeException(result, e, _logger);
        }

        return result;
    }
}
