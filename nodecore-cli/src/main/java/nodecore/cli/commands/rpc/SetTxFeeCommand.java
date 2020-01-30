// VeriBlock NodeCore CLI
// Copyright 2017-2019 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package nodecore.cli.commands.rpc;

import io.grpc.StatusRuntimeException;
import nodecore.api.grpc.VeriBlockMessages;
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
        name = "Set Transaction Fee",
        form = "settxfee",
        description = "Set the transaction fee for future transactions")
@CommandSpecParameter(name = "transactionFee", required = true, type = CommandParameterType.STRING)
public class SetTxFeeCommand implements Command {
    private static final Logger _logger = LoggerFactory.getLogger(SetTxFeeCommand.class);

    public SetTxFeeCommand() {}

    @Override
    public Result execute(CommandContext context) {
        Result result = new DefaultResult();

        long fee = Utility.convertDecimalCoinToAtomicLong(context.getParameter("transactionFee"));
        try {
            VeriBlockMessages.ProtocolReply reply = context
                    .adminService()
                    .setTransactionFee(VeriBlockMessages.SetTransactionFeeRequest.newBuilder().setAmount(fee).build());
            if (!reply.getSuccess()) {
                result.fail();
            } else {
                FormattableObject<EmptyPayload> temp = new FormattableObject<>(reply.getResultsList());
                temp.success = !result.didFail();
                temp.payload = new EmptyPayload();

                context.outputObject(temp);
            }
            for (VeriBlockMessages.Result r : reply.getResultsList())
                result.addMessage(r.getCode(), r.getMessage(), r.getDetails(), r.getError());
        } catch (StatusRuntimeException e) {
            CommandUtility.handleRuntimeException(result, e, _logger);
        }

        return result;
    }
}
