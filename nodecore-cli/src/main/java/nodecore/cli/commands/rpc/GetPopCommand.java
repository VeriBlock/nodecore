// VeriBlock NodeCore CLI
// Copyright 2017-2019 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package nodecore.cli.commands.rpc;

import com.google.inject.Inject;
import io.grpc.StatusRuntimeException;
import nodecore.api.grpc.GetPopReply;
import nodecore.api.grpc.GetPopRequest;
import nodecore.cli.annotations.CommandParameterType;
import nodecore.cli.annotations.CommandSpec;
import nodecore.cli.annotations.CommandSpecParameter;
import nodecore.cli.commands.serialization.FormattableObject;
import nodecore.cli.commands.serialization.PopPayload;
import nodecore.cli.commands.shell.StartPoPMinerCommand;
import nodecore.cli.contracts.Command;
import nodecore.cli.contracts.CommandContext;
import nodecore.cli.contracts.DefaultResult;
import nodecore.cli.contracts.Result;
import nodecore.cli.utilities.CommandUtility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.util.Collections;

@CommandSpec(
        name = "Get Pop",
        form = "getpop",
        description = "Gets the data VeriBlock wants Proof-of-Proof published to Bitcoin")
@CommandSpecParameter(name = "block", required = false, type = CommandParameterType.INTEGER)
public class GetPopCommand implements Command {
    private static final Logger _logger = LoggerFactory.getLogger(GetPopCommand.class);

    @Inject
    public GetPopCommand() {
    }

    @Override
    public Result execute(CommandContext context) {
        Result result = new DefaultResult();

        Integer blockNum = context.getParameter("block");
        try {
            GetPopRequest.Builder popRequest = GetPopRequest.newBuilder();
            if (blockNum != null && blockNum >= 0) {
                popRequest.setBlockNum(blockNum);
            }
            GetPopReply reply = context
                    .adminService()
                    .getPop(popRequest.build());
            if (!reply.getSuccess()) {
                result.fail();
            } else {
                FormattableObject<PopPayload> temp = new FormattableObject<>(reply.getResultsList());
                temp.success = !result.didFail();
                temp.payload = new PopPayload(reply);

                context.outputObject(temp);

                if (!GraphicsEnvironment.isHeadless()) {
                    context.suggestCommands(Collections.singletonList(StartPoPMinerCommand.class));
                }
            }
            for (nodecore.api.grpc.Result r : reply.getResultsList())
                result.addMessage(r.getCode(), r.getMessage(), r.getDetails(), r.getError());
        } catch (StatusRuntimeException e) {
            CommandUtility.handleRuntimeException(result, e, _logger);
        }

        return result;
    }
}
