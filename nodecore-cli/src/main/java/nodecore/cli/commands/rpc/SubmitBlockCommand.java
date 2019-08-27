// VeriBlock NodeCore CLI
// Copyright 2017-2019 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package nodecore.cli.commands.rpc;

import com.google.inject.Inject;
import com.google.protobuf.InvalidProtocolBufferException;
import io.grpc.StatusRuntimeException;
import nodecore.api.grpc.ProtocolReply;
import nodecore.api.grpc.SubmitBlocksRequest;
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
        name = "Submit Block",
        form = "submitblock",
        description = "Attempts to add the specified raw block")
@CommandSpecParameter(name = "rawBlock", required = true, type = CommandParameterType.STRING)
public class SubmitBlockCommand implements Command {
    private static final Logger _logger = LoggerFactory.getLogger(SubmitBlockCommand.class);

    @Inject
    public SubmitBlockCommand() {
    }

    @Override
    public Result execute(CommandContext context) throws InvalidProtocolBufferException {
        Result result = new DefaultResult();

        try {
            String rawBlock = context.getParameter("rawBlock");

            ProtocolReply reply = context
                    .adminService()
                    .submitBlocks(SubmitBlocksRequest.parseFrom(Utility.base64ToBytes(rawBlock)));
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
