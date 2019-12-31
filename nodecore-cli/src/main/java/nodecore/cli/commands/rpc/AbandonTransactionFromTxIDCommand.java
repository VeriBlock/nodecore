// VeriBlock NodeCore CLI
// Copyright 2017-2019 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package nodecore.cli.commands.rpc;

import com.google.protobuf.ByteString;
import io.grpc.StatusRuntimeException;
import nodecore.api.grpc.VeriBlockMessages;
import nodecore.cli.annotations.CommandParameterType;
import nodecore.cli.annotations.CommandSpec;
import nodecore.cli.annotations.CommandSpecParameter;
import nodecore.cli.commands.serialization.AbandonTransactionFromTxIDInfo;
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
        name = "Abandon Transaction From TxID",
        form = "abandontransactionfromtxid",
        description = "Abandons the specified pending transaction and all dependent transactions")
@CommandSpecParameter(name = "txId", required = true, type = CommandParameterType.STRING)
public class AbandonTransactionFromTxIDCommand implements Command {
    private static final Logger _logger = LoggerFactory.getLogger(AbandonTransactionFromTxIDCommand.class);

    public AbandonTransactionFromTxIDCommand() {
    }


    @Override
    public Result execute(CommandContext context) {
        Result result = new DefaultResult();

        String txid = context.getParameter("txId");

        try {
            VeriBlockMessages.AbandonTransactionRequest request = VeriBlockMessages.AbandonTransactionRequest.newBuilder()
                    .setTxids(
                            VeriBlockMessages.TransactionSet.newBuilder()
                                    .addTxids(ByteString.copyFrom(Utility.hexToBytes(txid))))
                    .build();

            VeriBlockMessages.AbandonTransactionReply reply = context
                    .adminService()
                    .abandonTransactionRequest(request);
            if (!reply.getSuccess()) {
                result.fail();
            } else {
                FormattableObject<AbandonTransactionFromTxIDInfo> temp = new FormattableObject<>(reply.getResultsList());

                temp.success = !result.didFail();
                temp.payload = new AbandonTransactionFromTxIDInfo(reply);

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
