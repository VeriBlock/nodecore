// VeriBlock NodeCore CLI
// Copyright 2017-2019 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package nodecore.cli.commands.rpc;

import com.google.inject.Inject;
import io.grpc.StatusRuntimeException;
import nodecore.api.grpc.GetPeerInfoReply;
import nodecore.api.grpc.GetPeerInfoRequest;
import nodecore.cli.annotations.CommandSpec;
import nodecore.cli.commands.serialization.FormattableObject;
import nodecore.cli.commands.serialization.PeerInfoPayload;
import nodecore.cli.contracts.Command;
import nodecore.cli.contracts.CommandContext;
import nodecore.cli.contracts.DefaultResult;
import nodecore.cli.contracts.Result;
import nodecore.cli.utilities.CommandUtility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;

@CommandSpec(
        name = "Get Peer Info",
        form = "getpeerinfo",
        description = "Returns a list of connected peers")
public class GetPeerInfoCommand implements Command {
    private static final Logger _logger = LoggerFactory.getLogger(GetPeerInfoCommand.class);

    @Inject
    public GetPeerInfoCommand() {}

    @Override
    public Result execute(CommandContext context) {
        Result result = new DefaultResult();
        try {
            GetPeerInfoReply reply = context
                    .adminService()
                    .getPeerInfo(GetPeerInfoRequest.newBuilder().build());
            if (!reply.getSuccess()) {
                result.fail();
            } else {
                FormattableObject<PeerInfoPayload> temp = new FormattableObject<>(reply.getResultsList());
                temp.success = !result.didFail();
                temp.payload = new PeerInfoPayload(reply);

                context.outputObject(temp);

                context.suggestCommands(Arrays.asList(
                        AddNodeCommand.class,
                        RemoveNodeCommand.class
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
