// VeriBlock NodeCore CLI
// Copyright 2017-2019 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package nodecore.cli.commands.rpc;

import com.google.inject.Inject;
import io.grpc.StatusRuntimeException;
import nodecore.api.grpc.GetStateInfoReply;
import nodecore.api.grpc.GetStateInfoRequest;
import nodecore.cli.annotations.CommandSpec;
import nodecore.cli.commands.serialization.FormattableObject;
import nodecore.cli.commands.serialization.GetStateInfoPayload;
import nodecore.cli.contracts.Command;
import nodecore.cli.contracts.CommandContext;
import nodecore.cli.contracts.DefaultResult;
import nodecore.cli.contracts.Result;
import nodecore.cli.utilities.CommandUtility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;

@CommandSpec(
        name = "Get state Info",
        form = "getstateinfo",
        description = "Returns blockchain, operating, and network state information")
public class GetStateInfoCommand implements Command {
    private static final Logger _logger = LoggerFactory.getLogger(GetStateInfoCommand.class);

    @Inject
    public GetStateInfoCommand() {}

    @Override
    public Result execute(CommandContext context) {
        Result result = new DefaultResult();

        try {
            GetStateInfoReply reply = context
                    .adminService()
                    .getStateInfo(GetStateInfoRequest.newBuilder().build());
            if (!reply.getSuccess()) {
                result.fail();
            } else {
                FormattableObject<GetStateInfoPayload> temp = new FormattableObject<>(reply.getResultsList());
                temp.success = !result.didFail();
                temp.payload = new GetStateInfoPayload(reply);

                context.outputObject(temp);

                context.suggestCommands(Collections.singletonList(GetInfoCommand.class));
            }
            for (nodecore.api.grpc.Result r : reply.getResultsList())
                result.addMessage(r.getCode(), r.getMessage(), r.getDetails(), r.getError());
        }  catch (StatusRuntimeException e) {
            CommandUtility.handleRuntimeException(result, e, _logger);
        }

        return result;
    }
}
