// VeriBlock NodeCore
// Copyright 2017-2019 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.


// VeriBlock NodeCore CLI
// Copyright 2017-2019 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package nodecore.cli.commands.rpc;

import com.google.inject.Inject;
import io.grpc.StatusRuntimeException;
import nodecore.api.grpc.GetProtectedChildrenReply;
import nodecore.api.grpc.GetProtectedChildrenRequest;
import nodecore.api.grpc.utilities.ByteStringUtility;
import nodecore.cli.annotations.CommandParameterType;
import nodecore.cli.annotations.CommandSpec;
import nodecore.cli.annotations.CommandSpecParameter;
import nodecore.cli.commands.serialization.FormattableObject;
import nodecore.cli.commands.serialization.PoPEndorsementsInfo;
import nodecore.cli.commands.shell.StartPoPMinerCommand;
import nodecore.cli.contracts.Command;
import nodecore.cli.contracts.CommandContext;
import nodecore.cli.contracts.DefaultResult;
import nodecore.cli.contracts.Result;
import nodecore.cli.utilities.CommandUtility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@CommandSpec(
        name = "Get Protected Children",
        form = "getprotectedchildren",
        description = "Returns the children protected by PoP transactions in a particular VeriBlock block identified by the provided block hash")

@CommandSpecParameter(name = "blockhash", required = true, type = CommandParameterType.HASH)
@CommandSpecParameter(name = "searchLength", required = false, type = CommandParameterType.INTEGER)
public class GetProtectedChildrenCommand implements Command {
    private static final Logger _logger = LoggerFactory.getLogger(GetPoPEndorsementsInfoCommand.class);

    @Inject
    public GetProtectedChildrenCommand() {
    }

    @Override
    public Result execute(CommandContext context) {
        Result result = new DefaultResult();

        String hash = context.getParameter("blockhash");
        Integer searchLength = context.getParameter("searchLength");

        try {
            GetProtectedChildrenRequest.Builder requestBuilder = GetProtectedChildrenRequest.newBuilder();
            requestBuilder.setVeriblockBlockHash(ByteStringUtility.hexToByteString(hash));

            if (searchLength != null)
                requestBuilder.setSearchLength(searchLength);

            GetProtectedChildrenReply reply = context
                    .adminService()
                    .getProtectedChildren(requestBuilder.build());
            if (!reply.getSuccess()) {
                result.fail();
            } else {
                FormattableObject<List<PoPEndorsementsInfo>> temp = new FormattableObject<>(reply.getResultsList());

                _logger.info("We received a total of " + reply.getPopEndorsementsCount() + " pop endorsements!");

                temp.success = !result.didFail();
                temp.payload = reply.getPopEndorsementsList()
                        .stream()
                        .map(PoPEndorsementsInfo::new).collect(Collectors.toList());

                context.outputObject(temp);

                context.suggestCommands(Arrays.asList(
                        GetProtectingParentsCommand.class,
                        GetPoPEndorsementsInfoCommand.class,
                        StartPoPMinerCommand.class
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
