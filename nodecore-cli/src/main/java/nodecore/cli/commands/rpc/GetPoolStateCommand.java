// VeriBlock NodeCore
// Copyright 2017-2019 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package nodecore.cli.commands.rpc;

import com.google.inject.Inject;
import io.grpc.StatusRuntimeException;
import nodecore.api.grpc.GetPoolStateReply;
import nodecore.api.grpc.GetPoolStateRequest;
import nodecore.cli.annotations.CommandSpec;
import nodecore.cli.commands.serialization.FormattableObject;
import nodecore.cli.commands.serialization.PoolStatePayload;
import nodecore.cli.contracts.Command;
import nodecore.cli.contracts.CommandContext;
import nodecore.cli.contracts.DefaultResult;
import nodecore.cli.contracts.Result;
import nodecore.cli.utilities.CommandUtility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
@CommandSpec(
        name = "Get Pool Info",
        form = "getpoolinfo",
        description = "Returns pool configuration and metrics")
public class GetPoolStateCommand implements Command {
    private static final Logger _logger = LoggerFactory.getLogger(GetStateInfoCommand.class);

    @Inject
    public GetPoolStateCommand() {}

    @Override
    public Result execute(CommandContext context) {
        Result result = new DefaultResult();

        try {
            GetPoolStateReply reply = context
                    .adminService()
                    .getPoolState(GetPoolStateRequest.newBuilder().build());
            if (!reply.getSuccess()) {
                result.fail();
            } else {
                FormattableObject<PoolStatePayload> temp = new FormattableObject<>(reply.getResultsList());
                temp.success = !result.didFail();
                temp.payload = new PoolStatePayload(reply);

                context.outputObject(temp);

                context.suggestCommands(Arrays.asList(
                        GetInfoCommand.class,
                        GetStateInfoCommand.class,
                        StartPoolCommand.class,
                        StopPoolCommand.class));
            }
            for (nodecore.api.grpc.Result r : reply.getResultsList())
                result.addMessage(r.getCode(), r.getMessage(), r.getDetails(), r.getError());
        }  catch (StatusRuntimeException e) {
            CommandUtility.handleRuntimeException(result, e, _logger);
        }

        return result;
    }
}
