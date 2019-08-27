// VeriBlock NodeCore CLI
// Copyright 2017-2019 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package nodecore.cli.commands.rpc;

import com.google.inject.Inject;
import io.grpc.StatusRuntimeException;
import nodecore.api.grpc.BlockFilter;
import nodecore.api.grpc.GetBlocksReply;
import nodecore.api.grpc.GetBlocksRequest;
import nodecore.cli.annotations.CommandParameterType;
import nodecore.cli.annotations.CommandSpec;
import nodecore.cli.annotations.CommandSpecParameter;
import nodecore.cli.commands.serialization.BlocksPayload;
import nodecore.cli.commands.serialization.FormattableObject;
import nodecore.cli.contracts.Command;
import nodecore.cli.contracts.CommandContext;
import nodecore.cli.contracts.DefaultResult;
import nodecore.cli.contracts.Result;
import nodecore.cli.utilities.CommandUtility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;

@CommandSpec(
        name = "Get Raw Block from Index",
        form = "getblockfromindex",
        description = "Returns the block for the given block index")
@CommandSpecParameter(name = "blockIndex", required = true, type = CommandParameterType.INTEGER)
public class GetBlockFromIndexCommand implements Command {
    private static final Logger _logger = LoggerFactory.getLogger(GetBlockFromIndexCommand.class);

    @Inject
    public GetBlockFromIndexCommand() {
    }

    @Override
    public Result execute(CommandContext context) {
        Result result = new DefaultResult();

        int index = context.getParameter("blockIndex");
        try {
            GetBlocksRequest.Builder requestBuilder = GetBlocksRequest.newBuilder();
            requestBuilder.addFilters(BlockFilter.newBuilder().setIndex(index));
            GetBlocksReply reply = context.adminService().getBlocks(requestBuilder.build());
            if (!reply.getSuccess()) {
                result.fail();
            } else {
                FormattableObject<BlocksPayload> temp = new FormattableObject<>(reply.getResultsList());
                temp.success = !result.didFail();
                temp.payload = new BlocksPayload(reply.getBlocksList());

                context.outputObject(temp);

                context.suggestCommands(Arrays.asList(
                        GetBlockFromHashCommand.class,
                        GetTransactionCommand.class
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
